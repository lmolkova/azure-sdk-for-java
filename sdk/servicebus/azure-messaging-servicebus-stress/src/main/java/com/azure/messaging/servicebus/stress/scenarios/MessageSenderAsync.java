// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.messaging.servicebus.stress.scenarios;

import com.azure.core.util.logging.ClientLogger;
import com.azure.messaging.servicebus.ServiceBusMessageBatch;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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

    @Override
    public void run() {
        beforeRun();

        final byte[] messagePayload = createMessagePayload(messageSize);

        client.set(TestUtils.getSenderBuilder(options, false).buildAsyncClient());

        int batchRatePerSec = sendMessageRatePerSecond / batchSize;
        RateLimiter rateLimiter = new RateLimiter(batchRatePerSec, sendConcurrency);
        Flux<ServiceBusMessageBatch> batches = createBatch(client.get(), messagePayload, batchSize).repeat();

        batches
            .take(options.getTestDuration())
            .flatMap(batch ->
                rateLimiter.acquire()
                    .then(client.get().sendMessages(batch)
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
