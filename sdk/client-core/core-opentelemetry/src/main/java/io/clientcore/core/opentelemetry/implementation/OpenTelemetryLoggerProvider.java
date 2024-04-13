// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.clientcore.core.opentelemetry.implementation;


import io.clientcore.core.util.LoggerProvider;
import io.clientcore.core.util.LoggerSpi;
import io.clientcore.core.util.LoggingOptions;

/**
 * Default {@link LoggerProvider} implementation.
 */
public final class OpenTelemetryLoggerProvider implements LoggerProvider {
    @Override
    public LoggerSpi createLogger(String className, LoggingOptions options) {
        return new OpenTelemetryLogger(className, options);
    }
}
