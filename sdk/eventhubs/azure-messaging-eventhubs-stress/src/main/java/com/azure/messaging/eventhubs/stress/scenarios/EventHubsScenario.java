// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.messaging.eventhubs.stress.scenarios;

import com.azure.core.util.logging.ClientLogger;
import com.azure.core.util.logging.LoggingEventBuilder;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.stress.util.ScenarioOptions;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.Disposable;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for event hubs test scenarios
 */
public abstract class EventHubsScenario implements AutoCloseable {
    private static final ClientLogger LOGGER = new ClientLogger(EventHubsScenario.class);
    private static final Meter METER = GlobalOpenTelemetry.getMeter("EventHubsScenario");
    private static final LongCounter ERROR_COUNTER = METER.counterBuilder("stress_test.errors").build();

    @Autowired
    protected ScenarioOptions options;

    private final List<AutoCloseable> toClose = new ArrayList<>();

    public void beforeRun() {
    }

    public abstract void run();

    public void afterRun() {
    }

    protected <T extends AutoCloseable> T toClose(T closeable) {
        toClose.add(closeable);
        return closeable;
    }

    protected Disposable toClose(Disposable closeable) {
        toClose.add(() -> closeable.dispose());
        return closeable;
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
                LOGGER.atError()
                    .addKeyValue("testClass", options.getTestClass())
                    .addKeyValue("closeable", closeable.getClass().getSimpleName())
                    .log("Couldn't close closeable", error);
            }
        }

        toClose.clear();
    }


    public void recordRunOptions(Span span) {
        String packageVersion = "unknown";
        try {
            Class<?> serviceBusPackage = Class.forName(EventHubClientBuilder.class.getName());
            packageVersion = serviceBusPackage.getPackage().getImplementationVersion();
            if (packageVersion == null) {
                packageVersion = "null";
            }
        } catch (ClassNotFoundException e) {
            LOGGER.warning("could not find EventHubClientBuilder class", e);
        }

        span.setAttribute(AttributeKey.stringKey("duration"), options.getTestDuration().toString());
        span.setAttribute(AttributeKey.stringKey("testClass"), options.getTestClass());
        span.setAttribute(AttributeKey.stringKey("eventHubName"), options.getEventHubsEventHubName());
        span.setAttribute(AttributeKey.stringKey("consumerGroupName"), options.getEventHubsConsumerGroup());
        span.setAttribute(AttributeKey.stringKey("eventHubsPackageVersion"), packageVersion);
        span.setAttribute(AttributeKey.stringKey("annotation"), options.getAnnotation());
        span.setAttribute(AttributeKey.longKey("messageSize"), options.getMessageSize());
        span.setAttribute(AttributeKey.stringKey("hostname"), System.getenv().get("HOSTNAME"));
    }

    public void recordError(String errorReason, Throwable ex, String method) {
        Attributes attributes = Attributes.builder()
            .put(AttributeKey.stringKey("error.type"), errorReason)
            .put(AttributeKey.stringKey("method"), method)
            .build();
        ERROR_COUNTER.add(1, attributes);
        LoggingEventBuilder log = LOGGER.atError()
            .addKeyValue("error.type", errorReason)
            .addKeyValue("method", method);
        if (ex != null) {
            log.log("test error", ex);
        } else {
            log.log("test error");
        }
    }
}
