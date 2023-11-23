// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.messaging.servicebus.stress.scenarios;

import com.azure.core.util.logging.ClientLogger;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import com.azure.messaging.servicebus.stress.util.RunResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.atomic.AtomicReference;

import static com.azure.messaging.servicebus.stress.scenarios.TestUtils.getReceiverBuilder;

/**
 * Test ServiceBusReceiverAsyncClient
 */
@Component("MessageReceiverAsync")
public class MessageReceiverAsync extends ServiceBusScenario {
    private static final ClientLogger LOGGER = new ClientLogger(MessageReceiverAsync.class);
    @Value("${FORWARD_CONNECTION_STRING:null}")
    private String forwardConnectionString;

    @Value("${FORWARD_QUEUE_NAME:null}")
    private String forwardQueue;
    @Override
    public RunResult run() {
        AtomicReference<RunResult> result = new AtomicReference<>(RunResult.INCONCLUSIVE);
        ServiceBusReceiverAsyncClient client = toClose(new ServiceBusClientBuilder()
            .connectionString(forwardConnectionString)
            .receiver()
            .queueName(forwardQueue)
            .receiveMode(ServiceBusReceiveMode.RECEIVE_AND_DELETE)
            .buildAsyncClient());

        client.receiveMessages()
                .take(options.getTestDuration())
                .onErrorResume(error -> {
                    result.set(RunResult.ERROR);
                    LOGGER.error("error receiving", error);
                    return Mono.empty();
                })
                .parallel(32)
                .runOn(Schedulers.boundedElastic())
                .doOnNext(message -> {
                    LOGGER.atInfo()
                        .addKeyValue("messageId", message.getMessageId())
                        .addKeyValue("deliveryCount", message.getDeliveryCount())
                        .log("message received");
                })
                .then()
                .block();

        return result.get();
    }
}
