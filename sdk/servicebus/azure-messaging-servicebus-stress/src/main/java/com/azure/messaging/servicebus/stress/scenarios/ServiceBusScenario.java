// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.messaging.servicebus.stress.scenarios;

import com.azure.core.util.ClientOptions;
import com.azure.core.util.IterableStream;
import com.azure.core.util.TracingOptions;
import com.azure.core.util.logging.ClientLogger;
import com.azure.core.util.logging.LogLevel;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import com.azure.messaging.servicebus.administration.models.QueueRuntimeProperties;
import com.azure.messaging.servicebus.stress.util.EntityType;
import com.azure.messaging.servicebus.stress.util.RunResult;
import com.azure.messaging.servicebus.stress.util.ScenarioOptions;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.Disposable;

import java.util.ArrayList;
import java.util.List;

import static com.azure.messaging.servicebus.stress.scenarios.TestUtils.blockingWait;

/**
 * Base class for service bus test scenarios
 */
public abstract class ServiceBusScenario implements AutoCloseable {
    private static final ClientLogger LOGGER = new ClientLogger(ServiceBusScenario.class);

    @Autowired
    protected ScenarioOptions options;

    protected ServiceBusAdministrationClient adminClient;
    protected ServiceBusReceiverClient receiverClient;

    private final List<AutoCloseable> toClose = new ArrayList<>();
    protected <T extends AutoCloseable> T toClose(T closeable) {
        toClose.add(closeable);
        return closeable;
    }

    protected Disposable toClose(Disposable closeable) {
        toClose.add(() -> closeable.dispose());
        return closeable;
    }

    /**
     * Run test scenario
     * @return test result
     */
    public abstract RunResult run() throws InterruptedException;

    public void beforeRun() {
        adminClient = new ServiceBusAdministrationClientBuilder()
            .connectionString(options.getServiceBusConnectionString())
            .buildClient();

        receiverClient = options.getServiceBusEntityType() == EntityType.QUEUE ? new ServiceBusClientBuilder()
            .connectionString(options.getServiceBusConnectionString())
            .clientOptions(new ClientOptions().setTracingOptions(new TracingOptions().setEnabled(false)))
            .receiver()
            .disableAutoComplete()
            .queueName(options.getServiceBusQueueName())
            .buildClient() : null;

        blockingWait(options.getStartDelay());
    }

    public void afterRun(RunResult result) {
    }

    @Override
    public synchronized void close() {
        if (toClose == null || toClose.size() == 0) {
            return;
        }

        for (final AutoCloseable closeable : toClose) {
            if (closeable == null) {
                continue;
            }

            try {
                closeable.close();
            } catch (Exception error) {
                LOGGER.error("[{}]: {} didn't close properly.", options.getTestClass(), closeable.getClass().getSimpleName(), error);
            }
        }

        toClose.clear();
        receiverClient.close();
    }

    protected int getRemainingQueueMessages() {
        if (options.getServiceBusEntityType() == EntityType.QUEUE) {
            QueueRuntimeProperties properties = adminClient.getQueueRuntimeProperties(options.getServiceBusQueueName());
            LOGGER.atInfo()
                .addKeyValue("activeCount", properties.getActiveMessageCount())
                .addKeyValue("scheduledCount", properties.getScheduledMessageCount())
                .addKeyValue("deadLetteredCount", properties.getDeadLetterMessageCount())
                .addKeyValue("transferredCount", properties.getTransferMessageCount())
                .addKeyValue("totalCount", properties.getTotalMessageCount())
                .log("Queue runtime properties");

            return properties.getActiveMessageCount();
        }

        return -1;
    }

    protected void logRemainingQueueMessages() {
        if (receiverClient != null) {
            int activeMessages = getRemainingQueueMessages();

            if (activeMessages > 0 && LOGGER.canLogAtLevel(LogLevel.VERBOSE)) {
                IterableStream<ServiceBusReceivedMessage> messages = receiverClient.peekMessages(activeMessages);
                messages.forEach(message -> LOGGER.atVerbose()
                    .addKeyValue("messageId", message.getMessageId())
                    .addKeyValue("traceparent", message.getApplicationProperties().get("traceparent"))
                    .addKeyValue("deliveryCount", message.getDeliveryCount())
                    .addKeyValue("lockToken", message.getLockToken())
                    .addKeyValue("lockedUntil", message.getLockedUntil())
                    .log("active message"));
            }
        }
    }
}
