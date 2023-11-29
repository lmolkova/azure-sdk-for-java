// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.messaging.servicebus.stress.scenarios;

import com.azure.core.util.logging.ClientLogger;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.stress.util.RunResult;
import com.azure.spring.messaging.checkpoint.Checkpointer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.binding.BindingsLifecycleController;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.azure.messaging.servicebus.stress.scenarios.TestUtils.blockingWait;
import static com.azure.spring.messaging.AzureHeaders.CHECKPOINTER;

@ConditionalOnProperty(value="binder.enabled")
@Component("BinderConsumer")
public class BinderConsumer extends ServiceBusScenario {
    private static final ClientLogger LOGGER = new ClientLogger(BinderConsumer.class);
    private final AtomicReference<RunResult> runResult = new AtomicReference<>(RunResult.INCONCLUSIVE);
    private byte[] expectedPayload = "Hello world!".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private ServiceBusSenderClient senderClient;

    @Autowired
    private BindingsLifecycleController binderLifecycle;

    @Bean
    public Consumer<Message<String>> consume() {
        return message -> {
            Checkpointer checkpointer = (Checkpointer) message.getHeaders().get(CHECKPOINTER);
            ServiceBusReceivedMessageContext messageContext = (ServiceBusReceivedMessageContext) message.getHeaders().get("azure_service_bus_received_message_context");
            ServiceBusReceivedMessage msg = messageContext.getMessage();
            try {
                checkMessage(messageContext.getMessage());
                senderClient.sendMessage(new ServiceBusMessage(message.getPayload()));
                //many.emitNext(MessageBuilder.withPayload(message.getPayload()).build(), Sinks.EmitFailureHandler.FAIL_FAST);
                checkpoint(checkpointer.success(), msg).block();
            } catch (Exception ex) {
                checkpoint(checkpointer.failure(), msg).block();
                throw ex;
            }
        };
    }

    private Mono<Void> checkpoint(Mono<Void> checkpoint, ServiceBusReceivedMessage msg) {
        return checkpoint
                .doOnSuccess(s -> {
                    LOGGER.atInfo()
                            .addKeyValue("messageId", msg.getMessageId())
                            .addKeyValue("deliveryCount", msg.getDeliveryCount())
                            .addKeyValue("status", "success")
                            .log("message settlement");
                })
                .doOnError(e -> {
                    LOGGER.atError()
                            .addKeyValue("messageId", msg.getMessageId())
                            .addKeyValue("deliveryCount", msg.getDeliveryCount())
                            .addKeyValue("status", "failed")
                            .log("message settlement", e);
                })
                .doOnCancel(() -> {
                    LOGGER.atError()
                            .addKeyValue("messageId", msg.getMessageId())
                            .addKeyValue("deliveryCount", msg.getDeliveryCount())
                            .addKeyValue("status", "cancelled")
                            .log("message settlement");
                });
    }

    @Override
    public RunResult run() throws InterruptedException {
        blockingWait(options.getTestDuration());

        int activeMessages = getRemainingQueueMessages();
        for (int extraMinutes = 0; extraMinutes < 3 && activeMessages > 0; extraMinutes++) {
            blockingWait(Duration.ofMinutes(1));
            activeMessages = getRemainingQueueMessages();
        }

        return activeMessages != 0 ? RunResult.WARNING : runResult.get();
    }

    @Override
    public void afterRun(RunResult runResult) {
        logRemainingQueueMessages();
        binderLifecycle.stop("consume-in-0");
        super.afterRun(runResult);
    }

    private boolean checkMessage(ServiceBusReceivedMessage message) {
        LOGGER.atInfo()
            .addKeyValue("messageId", message.getMessageId())
            .addKeyValue("traceparent", message.getApplicationProperties().get("traceparent"))
            .addKeyValue("deliveryCount", message.getDeliveryCount())
            .addKeyValue("lockToken", message.getLockToken())
            .addKeyValue("lockedUntil", message.getLockedUntil())
            .log("message received");

        if (message.getLockedUntil().isBefore(OffsetDateTime.now())) {
            LOGGER.atError()
                .addKeyValue("messageId", message.getMessageId())
                .addKeyValue("deliveryCount", message.getDeliveryCount())
                .log("message lock expired");
            runResult.set(RunResult.ERROR);
            return false;
        }

        byte[] payload = message.getBody().toBytes();
        if (payload.length != expectedPayload.length) {
            LOGGER.atError()
                .addKeyValue("messageId", message.getMessageId())
                .addKeyValue("actualSize", payload.length)
                .addKeyValue("expectedSize", expectedPayload.length)
                .log("message corrupted");
            runResult.set(RunResult.ERROR);
        }

        for (int i = 0; i < payload.length && i < expectedPayload.length; i++) {
            if (payload[i] != expectedPayload[i]) {
                LOGGER.atError()
                    .addKeyValue("messageId", message.getMessageId())
                    .addKeyValue("index", i)
                    .addKeyValue("actual", payload[i])
                    .addKeyValue("expected", expectedPayload[i])
                    .log("message corrupted");
                runResult.set(RunResult.ERROR);
            }
        }

        return true;
    }
}
