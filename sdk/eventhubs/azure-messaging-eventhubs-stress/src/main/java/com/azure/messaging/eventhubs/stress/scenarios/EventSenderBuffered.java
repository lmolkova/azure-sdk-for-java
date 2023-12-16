// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.messaging.eventhubs.stress.scenarios;

import com.azure.core.util.BinaryData;
import com.azure.core.util.logging.ClientLogger;
import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventHubBufferedProducerAsyncClient;
import com.azure.messaging.eventhubs.EventHubBufferedProducerClientBuilder;
import com.azure.messaging.eventhubs.stress.util.RateLimiter;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static com.azure.messaging.eventhubs.stress.util.TestUtils.blockingWait;
import static com.azure.messaging.eventhubs.stress.util.TestUtils.createMessagePayload;

/**
 * Test for EventSenderBuffered
 */
@Component("EventSenderBuffered")
public class EventSenderBuffered extends EventHubsScenario {
    private static final String PREFIX = UUID.randomUUID().toString().substring(25);
    private static final ClientLogger LOGGER = new ClientLogger(EventSenderBuffered.class);

    @Value("${SEND_MESSAGE_RATE:100}")
    private int sendMessageRatePerSecond;

    @Value("${MAX_EVENT_BUFFER_LENGTH_PER_PARTITION:100}")
    private int maxEventBufferLengthPerPartition;

    @Value("${MAX_WAIT_TIME_IN_MS:0}")
    private int maxWaitTimeInMs;

    private BinaryData messagePayload;
    private final AtomicLong sentCounter = new AtomicLong();
    private final static Meter METER = GlobalOpenTelemetry.getMeter("EventHubsStress");
    private final LongCounter enqueuedCounter = METER.counterBuilder("messaging.eventhubs.buffered_producer.enqueued_events").build();

    @Override
    public void run() {
        EventHubBufferedProducerAsyncClient sender = getBuilder()
            .onSendBatchFailed(context -> recordError(context.getThrowable().getClass().getName(), context.getThrowable(), "sendBuffed"))
            .onSendBatchSucceeded(context -> LOGGER.verbose("Send success."))
            .buildAsyncClient();

        messagePayload = createMessagePayload(options.getMessageSize());
        RateLimiter rateLimiter = toClose(new RateLimiter(sendMessageRatePerSecond, 10));

        toClose(createEvent()
            .repeat()
            .take(options.getTestDuration())
            .flatMap(event ->
                rateLimiter.acquire()
                    .then(sender.enqueueEvent(event)
                        .doOnNext(unused -> enqueuedCounter.add(1))
                        .doFinally(i -> rateLimiter.release())))
            .subscribe());

        blockingWait(options.getTestDuration().plusSeconds(30));
        sender.close();
    }

    @Override
    public void recordRunOptions(Span span) {
        super.recordRunOptions(span);
        span.setAttribute(AttributeKey.longKey("sendMessageRatePerSecond"), sendMessageRatePerSecond);
        span.setAttribute(AttributeKey.longKey("maxEventBufferLengthPerPartition"), maxEventBufferLengthPerPartition);
        span.setAttribute(AttributeKey.longKey("maxWaitTimeInMs"), maxWaitTimeInMs);
    }

    private Mono<EventData> createEvent() {
        return Mono.fromCallable(() -> {
                EventData message = new EventData(messagePayload);
                message.setMessageId(PREFIX + sentCounter.getAndIncrement());
                return message;
            });
    }

    private EventHubBufferedProducerClientBuilder getBuilder() {
        EventHubBufferedProducerClientBuilder builder = new EventHubBufferedProducerClientBuilder()
            .connectionString(options.getEventHubsConnectionString(), options.getEventHubsEventHubName());

        if (maxEventBufferLengthPerPartition > 0) {
            builder.maxEventBufferLengthPerPartition(maxEventBufferLengthPerPartition);
        }

        if (maxWaitTimeInMs > 0) {
            builder.maxWaitTime(Duration.ofMillis(maxWaitTimeInMs));
        }

        return builder;
    }
}
