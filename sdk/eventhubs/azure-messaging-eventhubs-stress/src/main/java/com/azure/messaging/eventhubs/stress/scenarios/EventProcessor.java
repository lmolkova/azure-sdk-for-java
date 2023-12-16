// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.messaging.eventhubs.stress.scenarios;

import com.azure.core.util.BinaryData;
import com.azure.core.util.logging.ClientLogger;
import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.models.EventBatchContext;
import com.azure.messaging.eventhubs.models.EventContext;
import com.azure.messaging.eventhubs.models.PartitionContext;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import static com.azure.messaging.eventhubs.stress.util.TestUtils.blockingWait;
import static com.azure.messaging.eventhubs.stress.util.TestUtils.createMessagePayload;
import static com.azure.messaging.eventhubs.stress.util.TestUtils.getProcessorBuilder;
import static com.azure.messaging.eventhubs.stress.util.TestUtils.startSampledInSpan;

/**
 * Test for EventProcessorClient
 */
@Component("EventProcessor")
public class EventProcessor extends EventHubsScenario {
    private static final ClientLogger LOGGER = new ClientLogger(EventProcessor.class);

    @Value("${PROCESS_CALLBACK_DURATION_MAX_IN_MS:0}")
    private int processMessageDurationMaxInMs;

    @Value("${MAX_BATCH_SIZE:100}")
    private int maxBatchSize;

    @Value("${MAX_WAIT_TIME_IN_MS:0}")
    private int maxWaitTimeInMs;

    @Value("${PREFETCH_COUNT:0}")
    private int prefetchCount;

    @Value("${CHECKPOINT_TIMEOUT_IN_SECONDS:0}")
    private int checkpointTimeoutInSeconds;

    @Value("${ENABLE_CHECKPOINT:true}")
    private boolean enableCheckpoint;

    private BinaryData expectedPayload;

    @Override
    public void run() {
        expectedPayload = createMessagePayload(options.getMessageSize());
        Duration maxWaitTime = maxWaitTimeInMs > 0 ? Duration.ofMillis(maxWaitTimeInMs) : null;

        EventProcessorClientBuilder eventProcessorClientBuilder = getProcessorBuilder(options, prefetchCount);

        if (maxBatchSize > 1) {
            eventProcessorClientBuilder.processEventBatch(this::processBatch, maxBatchSize, maxWaitTime);
        } else {
            eventProcessorClientBuilder.processEvent(this::processEvent, maxWaitTime);
        }

        EventProcessorClient eventProcessorClient = eventProcessorClientBuilder
            .processError(err -> recordError(err.getThrowable().getClass().getName(), err.getThrowable(), "processError"))
            .buildEventProcessorClient();

        eventProcessorClient.start();
        blockingWait(options.getTestDuration());
        eventProcessorClient.stop();
    }

    private void processBatch(EventBatchContext batchContext) {
        for (EventData eventData : batchContext.getEvents()) {
            checkEvent(eventData, batchContext.getPartitionContext());
            blockingWait(Duration.ofMillis(getWaitTime()));
        }
        checkpointWithTimeout(batchContext.updateCheckpointAsync());
    }

    private void processEvent(EventContext eventContext) {
        checkEvent(eventContext.getEventData(), eventContext.getPartitionContext());
        blockingWait(Duration.ofMillis(getWaitTime()));
        checkpointWithTimeout(eventContext.updateCheckpointAsync());
    }

    private void checkpointWithTimeout(Mono<Void> checkpoint) {
        if (enableCheckpoint) {
            try {
                if (checkpointTimeoutInSeconds > 0) {
                    checkpoint.block(Duration.ofSeconds(checkpointTimeoutInSeconds));
                } else {
                    checkpoint.block();
                }
            } catch (Throwable t) {
                recordError(t.getClass().getName(), t, "updateCheckpoint");
            }
        }
    }

    private void checkEvent(EventData message, PartitionContext partitionContext) {
        LOGGER.atInfo()
            .addKeyValue("traceparent", message.getProperties().get("traceparent"))
            .addKeyValue("offset", message.getOffset())
            .addKeyValue("messageId", message.getMessageId())
            .addKeyValue("partitionId", partitionContext.getPartitionId())
            .log("message received");

        String payload = message.getBodyAsString();
        if (!payload.equals(expectedPayload.toString())) {
            recordError("message corrupted", null, "checkMessage");
            startSampledInSpan("message corrupted")
                .setAttribute("expectedPayload", expectedPayload.toString())
                .setAttribute("actualPayload", payload)
                .end();
        }
    }

    private int getWaitTime() {
        return processMessageDurationMaxInMs == 0 ? 0 : ThreadLocalRandom.current().nextInt(processMessageDurationMaxInMs);
    }

    @Override
    public void recordRunOptions(Span span) {
        super.recordRunOptions(span);
        span.setAttribute(AttributeKey.longKey("processMessageDurationMaxInMs"), processMessageDurationMaxInMs);
        span.setAttribute(AttributeKey.longKey("maxBatchSize"), maxBatchSize);
        span.setAttribute(AttributeKey.longKey("maxWaitTimeInMs"), maxWaitTimeInMs);
        span.setAttribute(AttributeKey.booleanKey("enableCheckpoint"), enableCheckpoint);
        span.setAttribute(AttributeKey.longKey("checkpointTimeoutInSeconds"), checkpointTimeoutInSeconds);
        span.setAttribute(AttributeKey.longKey("prefetchCount"), prefetchCount);
    }
}
