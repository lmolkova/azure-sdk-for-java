// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.clientcore.core.opentelemetry;


import io.clientcore.core.http.pipeline.HttpPipelinePolicy;
import io.clientcore.core.opentelemetry.implementation.LogicalInstrumentationPolicy;
import io.clientcore.core.opentelemetry.implementation.OpenTelemetryLogger;
import io.clientcore.core.util.InstrumentationOptions;
import io.clientcore.core.util.InstrumentationProvider;
import io.clientcore.core.util.LoggerProvider;
import io.clientcore.core.util.LoggerSpi;
import io.clientcore.core.util.LoggingOptions;
import io.opentelemetry.api.GlobalOpenTelemetry;

/**
 * {@link LoggerProvider} implementation.
 */
public final class OpenTelemetryInstrumentationProvider implements LoggerProvider, InstrumentationProvider {
    public OpenTelemetryInstrumentationProvider() {
    }

    @Override
    public LoggerSpi createLogger(String className, LoggingOptions options) {
        return new OpenTelemetryLogger(className, options);
    }

    // todo scope
    @Override
    public HttpPipelinePolicy createPhysicalPolicy(InstrumentationOptions options) {
        return new LogicalInstrumentationPolicy(fromOptions(options));
    }

    @Override
    public HttpPipelinePolicy createLogicalPolicy(InstrumentationOptions options) {
        return null;
    }

    private static OpenTelemetryInstrumentationOptions fromOptions(InstrumentationOptions options) {
        if (options instanceof OpenTelemetryInstrumentationOptions) {
            return (OpenTelemetryInstrumentationOptions) options;
        }

        OpenTelemetryInstrumentationOptions otelOptions = new OpenTelemetryInstrumentationOptions()
            .setOpenTelemetry(GlobalOpenTelemetry.get());

        if (options == null) {
            return otelOptions;
        }

        otelOptions
                .setTracingEnabled(options.isTracingEnabled())
                .setMetricsEnabled(options.isMetricsEnabled());
        return otelOptions;
    }
}
