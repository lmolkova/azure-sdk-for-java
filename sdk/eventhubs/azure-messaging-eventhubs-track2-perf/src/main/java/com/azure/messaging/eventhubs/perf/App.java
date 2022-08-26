// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.messaging.eventhubs.perf;

import com.azure.perf.test.core.PerfStressProgram;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.runtimemetrics.Cpu;
import io.opentelemetry.instrumentation.runtimemetrics.GarbageCollector;
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

import java.lang.management.GarbageCollectorMXBean;
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

        GarbageCollector.registerObservers(otel);
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
        private final ConcurrentLinkedDeque<MetricData> gcc = new ConcurrentLinkedDeque<>();
        private final ConcurrentLinkedDeque<MetricData> gct = new ConcurrentLinkedDeque<>();
        private static final AttributeKey<String> POOL_NAME = AttributeKey.stringKey("pool");
        private static final AttributeKey<String> GC_NAME = AttributeKey.stringKey("gc");
        private static final double MB = 1024 * 1024d;

        public InMemoryMetricReader() {
            this.aggregationTemporality = AggregationTemporality.CUMULATIVE;
        }

        /** Returns all metrics accumulated since the last call. */
        public void collect() {
            Collection<MetricData> metrics =  metricProducer.collectAllMetrics();
            boolean memfound = false;
            boolean cpufound = false;
            boolean gccfound  = false;
            boolean gctfound  = false;
            for (MetricData d : metrics) {
                if (d.getName().equals("process.runtime.jvm.memory.usage")) {
                    memfound = true;
                    jvmMem.add(d);
                } else if (d.getName().equals("process.runtime.jvm.cpu.utilization")) {
                    cpufound = true;
                    cpuUtilization.add(d);
                } else if (d.getName().equals("runtime.jvm.gc.count")) {
                    gccfound = true;
                    gcc.add(d);
                } else if (d.getName().equals("runtime.jvm.gc.time")) {
                    gctfound = true;
                    gct.add(d);
                } else {
                    //System.out.println(d.getName());
                }
            }

            if (!memfound) {
                jvmMem.add(null);
            }

            if (!cpufound) {
                cpuUtilization.add(null);
            }

            if (!gctfound) {
                gct.add(null);
            }

            if (!gccfound) {
                gcc.add(null);
            }
        }

        void print() {

            System.out.println("| JVM memory usage: G1 Eden Space | JVM memory usage: G1 Survivor Space | JVM memory usage: G1 Old Gen |   CPU    |  GC Count Old  | GC Count Young | GC Time Old | GC Time Young |");
            System.out.println("|---------------------------------|-------------------------------------|------------------------------|----------|----------------|----------------|-------------|---------------|");

            MetricData mem = jvmMem.poll(), cpu = cpuUtilization.poll(), gccp = gcc.poll(), gctp = gct.poll();
            while (mem!= null && cpu != null) {
                double maxG1Eden = 0;
                double maxG1Surv = 0;
                double maxG1Old = 0;
                double cpuUt = 0;
                long gccOld = 0;
                long gccYoung = 0;

                long gctOld = 0;
                long gctYoung = 0;

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

                if (gctp != null) {
                    for (LongPointData p : gctp.getLongSumData().getPoints()) {
                        String gc = p.getAttributes().get(GC_NAME);

                        if (gc.equals("G1 Old Generation")) {
                            if (p.getValue() > gctOld) {
                                gctOld = p.getValue();
                            }
                        } else if (gc.equals("G1 Young Generation")) {
                            if (p.getValue() > gctYoung) {
                                gctYoung = p.getValue();
                            }
                        } else {
                            System.out.println(gc);
                        }
                    }
                }

                if (gccp != null) {
                    for (LongPointData p : gccp.getLongSumData().getPoints()) {
                        String gc = p.getAttributes().get(GC_NAME);

                        if (gc.equals("G1 Old Generation")) {
                            if (p.getValue() > gccOld) {
                                gccOld = p.getValue();
                            }
                        } else if (gc.equals("G1 Young Generation")) {
                            if (p.getValue() > gccYoung) {
                                gccYoung = p.getValue();
                            }
                        }  else {
                            System.out.println(gc);
                        }
                    }
                }

                System.out.printf("|     %27d |     %31d |     %24d | %8d | %14d | %14d | %12d | %12d |\n", (long)(maxG1Eden / MB), (long)(maxG1Surv / MB), (long)(maxG1Old / MB), (int)(cpuUt * 100),
                    gccOld, gccYoung, gctOld, gctYoung);
                mem = jvmMem.poll();
                cpu = cpuUtilization.poll();
                gccp = gcc.poll();
                gctp = gct.poll();
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
