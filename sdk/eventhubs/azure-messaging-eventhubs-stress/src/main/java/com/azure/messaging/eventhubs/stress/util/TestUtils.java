// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.messaging.eventhubs.stress.util;

import com.azure.core.util.BinaryData;
import com.azure.core.util.logging.ClientLogger;
import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class TestUtils {
    private static final byte[] PAYLOAD = "this is a circular payload that is used to fill up the message".getBytes(StandardCharsets.UTF_8);
    private static final Tracer TRACER = GlobalOpenTelemetry.getTracer("EventHubsScenarioRunner");
    private static final ClientLogger LOGGER = new ClientLogger(TestUtils.class);

    public static EventHubClientBuilder getReceiverBuilder(ScenarioOptions options, int prefetchCount) {
        return getBuilder(options)
            .prefetchCount(prefetchCount)
            .consumerGroup(options.getEventHubsConsumerGroup());
    }

    public static EventProcessorClientBuilder getProcessorBuilder(ScenarioOptions options, int prefetchCount) {
        return new EventProcessorClientBuilder()
            .prefetchCount(prefetchCount)
            .consumerGroup(options.getEventHubsConsumerGroup())
            .connectionString(options.getEventHubsConnectionString(), options.getEventHubsEventHubName())
            .checkpointStore(new BlobCheckpointStore(getContainerClient(options)));
    }

    public static EventDataBatch createBatchSync(EventHubProducerClient client, BinaryData messagePayload, int batchSize) {
        try {
            EventDataBatch batch = client.createBatch();
            for (int i = 0; i < batchSize; i++) {
                batch.tryAdd(new EventData(messagePayload));
            }

            return batch;
        } catch (Exception e) {
            throw LOGGER.logExceptionAsError(new RuntimeException("Error creating batch", e));
        }
    }

    public static BinaryData createMessagePayload(int messageSize) {
        final StringBuilder body = new StringBuilder(messageSize);
        for (int i = 0; i < messageSize; i++) {
            body.append(PAYLOAD[i % PAYLOAD.length]);
        }
        return BinaryData.fromString(body.toString());
    }

    public static EventHubClientBuilder getBuilder(ScenarioOptions options) {
        return new EventHubClientBuilder()
            .connectionString(options.getEventHubsConnectionString())
            .eventHubName(options.getEventHubsEventHubName());
    }

    private static BlobContainerAsyncClient getContainerClient(ScenarioOptions options) {
        return new BlobContainerClientBuilder()
            .connectionString(options.getStorageConnectionString())
            .containerName(options.getStorageContainerName())
            .buildAsyncClient();
    }

    public static boolean blockingWait(Duration duration) {
        if (duration.toMillis() <= 0) {
            return true;
        }

        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            LOGGER.warning("wait interrupted");
            return false;
        }

        return true;
    }

    public static Span startSampledInSpan(String name) {
        return TRACER.spanBuilder(name)
            // guarantee that we have before/after spans sampled in
            // and record duration/result of the test
            .setAttribute("sample.in", "true")
            .startSpan();
    }

    private TestUtils() {
    }
}
