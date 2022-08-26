// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.messaging.eventhubs.perf;

import com.azure.perf.test.core.PerfStressProgram;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.runtimemetrics.Cpu;
import io.opentelemetry.instrumentation.runtimemetrics.MemoryPools;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.CollectionRegistration;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.internal.export.MetricProducer;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

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
        InMemoryMetricReader reader = new InMemoryMetricReader();
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
            .registerMetricReader(reader)
            //.registerMetricReader(PrometheusHttpServer.create())
            .build();
        OpenTelemetry otel = OpenTelemetrySdk.builder().setMeterProvider(meterProvider).buildAndRegisterGlobal();

        Cpu.registerObservers(otel);
        MemoryPools.registerObservers(otel);

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                reader.collect();
            }
        }, 1000, 1000);

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

        timer.cancel();
        reader.print();
    }

    static class InMemoryMetricReader implements MetricReader {
        private final AggregationTemporality aggregationTemporality;
        private final AtomicBoolean isShutdown = new AtomicBoolean(false);
        private volatile MetricProducer metricProducer = MetricProducer.noop();
        private final ConcurrentLinkedDeque<MetricData> jvmMem = new ConcurrentLinkedDeque<>();
        private final ConcurrentLinkedDeque<MetricData> cpuUtilization = new ConcurrentLinkedDeque<>();
        private static final AttributeKey<String> POOL_NAME = AttributeKey.stringKey("pool");
        private static final double MB = 1024 * 1024d;

        public InMemoryMetricReader() {
            this.aggregationTemporality = AggregationTemporality.CUMULATIVE;
        }

        /** Returns all metrics accumulated since the last call. */
        public void collect() {
            if (isShutdown.get()) {
                return;
            }
            Collection<MetricData> metrics =  metricProducer.collectAllMetrics();
            AtomicBoolean memfound = new AtomicBoolean(false);
            AtomicBoolean cpufound = new AtomicBoolean(false);

            metrics.stream().forEach(d -> {
                if (d.getName().equals("process.runtime.jvm.memory.usage")) {
                    memfound.set(true);
                    jvmMem.add(d);
                } else if (d.getName().equals("process.runtime.jvm.cpu.utilization")) {
                    cpufound.set(true);
                    cpuUtilization.add(d);
                }
            });

            if (!memfound.get()) {
                jvmMem.add(null);
            }

            if (!cpufound.get()) {
                cpuUtilization.add(null);
            }
        }

        void print() {

            System.out.println("| JVM memory usage: G1 Eden Space | JVM memory usage: G1 Survivor Space | JVM memory usage: G1 Old Gen | CPU | ");
            System.out.println("|---------------------------------|-------------------------------------|------------------------------|-----|");

            MetricData mem = jvmMem.poll(), cpu = cpuUtilization.poll();
            while (mem!= null && cpu != null) {
                double maxG1Eden = 0;
                double maxG1Surv = 0;
                double maxG1Old = 0;
                double cpuUt = 0;

                if (mem != null) {
                    for (LongPointData p : mem.getLongSumData().getPoints()) {
                        String pool = p.getAttributes().get(POOL_NAME);

                        if (pool.equals("G1 Eden Space")) {
                            if (p.getValue() > maxG1Eden) {
                                maxG1Eden = p.getValue();
                            }
                        } else if (pool.equals("G1 Survivor Space")) {
                            if (p.getValue() > maxG1Surv) {
                                maxG1Surv = p.getValue();
                            }
                        } else if (pool.equals("G1 Old Gen")) {
                            if (p.getValue() > maxG1Old) {
                                maxG1Old = p.getValue();
                            }
                        }
                    }
                }

                if (cpu != null) {
                    DoublePointData ut = cpu.getDoubleGaugeData().getPoints().stream().findFirst().orElse(null);
                    if (ut != null) {
                        cpuUt = ut.getValue();
                    }
                }


                System.out.printf("|     %28f|     %32f|     %25f| %f |\n", maxG1Eden / MB, maxG1Surv / MB, maxG1Old / MB, cpuUt);
                mem = jvmMem.poll();
                cpu = cpuUtilization.poll();
            }
        }


        @Override
        public void register(CollectionRegistration registration) {
            this.metricProducer = MetricProducer.asMetricProducer(registration);
        }

        @Override
        public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
            return aggregationTemporality;
        }

        @Override
        public CompletableResultCode forceFlush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            isShutdown.set(true);
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public String toString() {
            return "InMemoryMetricReader{aggregationTemporality=" + aggregationTemporality + "}";
        }
    }
}
