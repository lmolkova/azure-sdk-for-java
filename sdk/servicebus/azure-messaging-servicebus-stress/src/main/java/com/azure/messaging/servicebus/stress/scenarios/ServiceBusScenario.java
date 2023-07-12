// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.messaging.servicebus.stress.scenarios;

import com.azure.core.util.CoreUtils;
import com.azure.core.util.logging.ClientLogger;
import com.azure.messaging.servicebus.stress.util.ScenarioOptions;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for service bus test scenarios
 */
public abstract class ServiceBusScenario {
    public ClientLogger LOGGER = new ClientLogger(ServiceBusScenario.class);

    @Autowired
    protected ScenarioOptions options;

    /**
     * Run test scenario
     */
    public abstract void run();

    public void beforeRun() {
        LOGGER.atInfo()
            .addKeyValue("duration", options.getTestDuration())
            .addKeyValue("tryTimeout", options.getTryTimeout())
            .addKeyValue("testClass", options.getTestClass())
            .addKeyValue("entityType", options.getServiceBusEntityType())
            .addKeyValue("queueName", options.getServiceBusQueueName())
            .addKeyValue("sessionQueueName", options.getServiceBusSessionQueueName())
            .addKeyValue("topicName", options.getServiceBusTopicName())
            .addKeyValue("subscriptionName", options.getServiceBusSubscriptionName())
            .addKeyValue("connectionStringProvided", !CoreUtils.isNullOrEmpty(options.getServiceBusConnectionString()))
            .log("starting test");
    }
}
