// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.messaging.eventhubs.stress.scenarios;

import com.azure.core.util.BinaryData;
import com.azure.core.util.logging.ClientLogger;
import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.messaging.eventhubs.models.EventBatchContext;
import com.azure.messaging.eventhubs.stress.util.TestUtils;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
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
    @Value("${PROCESS_CALLBACK_DURATION_MAX_IN_MS:50}")
    private int processMessageDurationMaxInMs;

    @Value("${MAX_BATCH_SIZE:100}")
    private int maxBatchSize;

    @Value("${MAX_WAIT_TIME_IN_MS:#{null}}")
    private Integer maxWaitTimeInMs;

    @Value("${PREFETCH_COUNT:0}")
    private int prefetchCount;

    private BinaryData expectedPayload;
    @Override
    public void run() {
        expectedPayload = createMessagePayload(options.getMessageSize());
        Duration maxWaitTime = maxWaitTimeInMs == null ? null : Duration.ofMillis(maxWaitTimeInMs);
        EventProcessorClient eventProcessorClient = getProcessorBuilder(options, prefetchCount)
            .processEventBatch(this::process, maxBatchSize, maxWaitTime)
            .processError(err -> recordError(err.getThrowable().getClass().getName(), err.getThrowable(), "processError"))
            .buildEventProcessorClient();

        eventProcessorClient.start();
        blockingWait(options.getTestDuration());
    }

    private void process(EventBatchContext batchContext) {
        for(EventData eventData : batchContext.getEvents()) {
            checkEvent(eventData);
            blockingWait(Duration.ofMillis(getWaitTime()));
            try {
                batchContext.updateCheckpoint();
            } catch (Exception e) {
                recordError(e.getClass().getName(), e, "updateCheckpoint");
            }
        }
    }

    private boolean checkEvent(EventData message) {
        LOGGER.atInfo()
            .addKeyValue("traceparent", message.getProperties().get("traceparent"))
            .addKeyValue("offset", message.getOffset())
            .addKeyValue("messageId", message.getMessageId())
            .log("message received");

        String payload = message.getBody().toString();
        if (!payload.equals(expectedPayload.toString())) {
            recordError("message corrupted", null, "checkMessage");
            startSampledInSpan("message corrupted")
                .setAttribute("actualPayload", payload)
                .end();
        }

        return true;
    }

    private int getWaitTime() {
        return processMessageDurationMaxInMs == 0 ? 0 : ThreadLocalRandom.current().nextInt(processMessageDurationMaxInMs);
    }

    @Override
    public void recordRunOptions(Span span) {
        super.recordRunOptions(span);
        span.setAttribute(AttributeKey.longKey("processMessageDurationMaxInMs"), processMessageDurationMaxInMs);
        span.setAttribute(AttributeKey.longKey("maxBatchSize"), maxBatchSize);
        span.setAttribute(AttributeKey.longKey("prefetchCount"), prefetchCount);
    }
}
