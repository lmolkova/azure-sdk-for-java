// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.messaging.servicebus.stress.scenarios;

import com.azure.core.util.logging.ClientLogger;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import static com.azure.messaging.servicebus.stress.scenarios.TestUtils.blockingWait;
import static com.azure.messaging.servicebus.stress.scenarios.TestUtils.getProcessorBuilder;

/**
 * Test ServiceBusProcessorClient
 */
@Component("MessageProcessor")
public class MessageProcessor extends ServiceBusScenario {
    private static final ClientLogger LOGGER = new ClientLogger(MessageProcessor.class);

    @Value("${PROCESS_CALLBACK_DURATION_MAX_IN_MS:1000}")
    private int processMessageDurationMaxInMs;

    @Value("${MAX_CONCURRENT_CALLS:100}")
    private int maxConcurrentCalls;

    @Value("${PREFETCH_COUNT:0}")
    private int prefetchCount;

    @Override
    public void run() {
        beforeRun();

        ServiceBusProcessorClient processor = getProcessorBuilder(options)
            .maxAutoLockRenewDuration(Duration.ofMillis(processMessageDurationMaxInMs + 1000))
            .maxConcurrentCalls(maxConcurrentCalls)
            .prefetchCount(prefetchCount)
            .processMessage(this::process)
            .processError(err -> {
                throw LOGGER.logExceptionAsError(new RuntimeException(err.getException()));
            })
            .buildProcessorClient();

        processor.start();
        blockingWait(options.getTestDuration());
        processor.close();
    }

    private void process(ServiceBusReceivedMessageContext messageContext) {
        if (processMessageDurationMaxInMs != 0) {
            int processTimeMs = ThreadLocalRandom.current().nextInt(processMessageDurationMaxInMs);
            blockingWait(Duration.ofMillis(processTimeMs));
        }
        messageContext.complete();
    }
}
