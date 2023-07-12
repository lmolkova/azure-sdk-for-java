// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.messaging.servicebus.stress.scenarios;

import com.azure.core.util.Context;
import com.azure.core.util.TelemetryAttributes;
import com.azure.core.util.logging.ClientLogger;
import com.azure.core.util.metrics.LongCounter;
import com.azure.core.util.metrics.Meter;
import com.azure.core.util.metrics.MeterProvider;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusMessageBatch;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static com.azure.messaging.servicebus.stress.scenarios.TestUtils.blockingWait;
import static com.azure.messaging.servicebus.stress.scenarios.TestUtils.createBatch;
import static com.azure.messaging.servicebus.stress.scenarios.TestUtils.createMessagePayload;

/**
 * Test ServiceBusSenderAsyncClient
 */
@Component("MessageSenderAsync")
public class MessageSenderAsync extends ServiceBusScenario {
    private static final ClientLogger LOGGER = new ClientLogger(MessageSenderAsync.class);
    @Value("${SEND_MESSAGE_RATE:100}")
    private int sendMessageRatePerSecond;

    @Value("${BATCH_SIZE:2}")
    private int batchSize;

    @Value("${MESSAGE_SIZE_IN_BYTES:128}")
    private int messageSize;

    @Value("${SEND_CONCURRENCY:5}")
    private int sendConcurrency;

    private AtomicReference<ServiceBusSenderAsyncClient> client = new AtomicReference<>();
    private static final Meter METER = MeterProvider.getDefaultProvider().createMeter("stress_test", null, null);
    private static final LongCounter counter = METER.createLongCounter("sender_async_messages_after", "foo", "messages");
    private static final LongCounter attemptSending = METER.createLongCounter("sender_async_messages_before", "foo", "messages");

    @Override
    public void run() {
        beforeRun();

        final byte[] messagePayload = createMessagePayload(messageSize);

        client.set(TestUtils.getSenderBuilder(options, false).buildAsyncClient());

        int batchRatePerSec = sendMessageRatePerSecond / batchSize;
        RateLimiter rateLimiter = new RateLimiter(batchRatePerSec, sendConcurrency);
        Flux<ServiceBusMessageBatch> batches = createBatch(client.get(), messagePayload, batchSize).repeat();

        TelemetryAttributes ok = METER.createAttributes(Collections.singletonMap("status", "ok"));
        TelemetryAttributes error = METER.createAttributes(Collections.singletonMap("status", "error"));
        TelemetryAttributes cancel = METER.createAttributes(Collections.singletonMap("status", "cancel"));
        TelemetryAttributes none = METER.createAttributes(Collections.emptyMap());
        batches
            .take(options.getTestDuration())
            .flatMap(batch ->
                rateLimiter.acquire()
                    .then(Mono.defer(() -> {
                        attemptSending.add(batchSize, none, Context.NONE);
                        return client.get().sendMessages(batch)
                            .doOnSuccess(i -> counter.add(batchSize, ok, Context.NONE))
                            .doOnError(t -> counter.add(batchSize, error, Context.NONE))
                            .doOnCancel(() -> counter.add(batchSize, cancel, Context.NONE));
                        })
                    .onErrorResume(t -> true, t -> {
                        LOGGER.error("error when sending", t);
                        client.set(TestUtils.getSenderBuilder(options, false).buildAsyncClient());
                        return Mono.empty();
                    })
                    .doFinally(i -> rateLimiter.release())))
            .parallel(sendConcurrency, sendConcurrency)
            .runOn(Schedulers.boundedElastic())
            .subscribe();

        blockingWait(options.getTestDuration().plusSeconds(30));
        LOGGER.info("done");
        client.get().close();
        rateLimiter.close();
    }
}
