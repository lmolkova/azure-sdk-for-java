// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.messaging.eventhubs.stress.scenarios;

import com.azure.core.util.BinaryData;
import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventHubProducerAsyncClient;
import com.azure.messaging.eventhubs.models.CreateBatchOptions;
import com.azure.messaging.eventhubs.stress.util.RateLimiter;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static com.azure.messaging.eventhubs.stress.util.TestUtils.blockingWait;
import static com.azure.messaging.eventhubs.stress.util.TestUtils.createMessagePayload;
import static com.azure.messaging.eventhubs.stress.util.TestUtils.getBuilder;

/**
 * Test for EventSender
 */
@Component("EventSender")
public class EventSender extends EventHubsScenario {
    private static final String PREFIX = UUID.randomUUID().toString().substring(25);

    @Value("${SEND_MESSAGE_RATE:-1}")
    private int sendMessageRatePerSecond;

    @Value("${SEND_CONCURRENCY:5}")
    private int sendConcurrency;

    @Value("${BATCH_SIZE:2}")
    private int batchSize;

    private EventHubProducerAsyncClient client;
    private BinaryData messagePayload;
    private final AtomicLong sentCounter = new AtomicLong();
    private CreateBatchOptions batchOptions;

    @Override
    public void run() {
        batchOptions = new CreateBatchOptions().setMaximumSizeInBytes(batchSize * (options.getMessageSize() + 100));
        messagePayload = createMessagePayload(options.getMessageSize());
        client = toClose(getBuilder(options).buildAsyncProducerClient());
        int batchRatePerSec = sendMessageRatePerSecond / batchSize;
        RateLimiter rateLimiter = toClose(new RateLimiter(batchRatePerSec, sendConcurrency));

        toClose(createBatch().repeat()
            .take(options.getTestDuration())
            .flatMap(batch ->
                rateLimiter.acquire()
                    .then(send(batch).doFinally(i -> rateLimiter.release())))
            .parallel(sendConcurrency, sendConcurrency)
            .runOn(Schedulers.boundedElastic())
            .subscribe());

        blockingWait(options.getTestDuration());
    }

    @Override
    public void recordRunOptions(Span span) {
        super.recordRunOptions(span);
        span.setAttribute(AttributeKey.longKey("sendMessageRatePerSecond"), sendMessageRatePerSecond);
        span.setAttribute(AttributeKey.longKey("sendConcurrency"), sendConcurrency);
        span.setAttribute(AttributeKey.longKey("batchSize"), batchSize);
    }

    private Mono<Void> send(EventDataBatch batch) {
        return client.send(batch)
            .onErrorResume(t -> true,
                t -> {
                    recordError("send error", t, "send");
                    return Mono.empty();
                });
    }

    private Mono<EventDataBatch> createBatch() {
        return Mono.defer(() -> client.createBatch(batchOptions)
            .doOnNext(b -> {
                for (int i = 0; i < batchSize; i ++) {
                    EventData message = new EventData(messagePayload);
                    message.setMessageId(PREFIX + sentCounter.getAndIncrement());
                    if (batchOptions.getMaximumSizeInBytes() < b.getSizeInBytes() + options.getMessageSize() + 100 || !b.tryAdd(message)) {
                        break;
                    }
                }
            }));
    }
}
