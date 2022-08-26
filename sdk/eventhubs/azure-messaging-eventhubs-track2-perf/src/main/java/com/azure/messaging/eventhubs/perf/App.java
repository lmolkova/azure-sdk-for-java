// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.messaging.eventhubs.perf;

import com.azure.perf.test.core.PerfStressProgram;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.OpenTelemetrySdk;

/**
 * Runs the Event Hubs performance tests.
 */
public class App {
    /**
     * Starts running a performance test.
     *
     * @param args Unused command line arguments.
     * @throws RuntimeException If not able to load test classes.
     */
    public static void main(String[] args) {
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
            .registerMetricReader(PrometheusHttpServer.create())
            .build();
        OpenTelemetrySdk.builder().setMeterProvider(meterProvider).buildAndRegisterGlobal();

        final Class<?>[] testClasses = new Class<?>[]{
            ReceiveEventsTest.class,
            SendEventDataTest.class,
            SendEventDataBatchTest.class,
            EventProcessorTest.class,
            GetPartitionInformationTest.class,
            ReactorReceiveEventsTest.class,
            EventProcessorJedisTest.class
        };

        PerfStressProgram.run(testClasses, args);
    }
}
