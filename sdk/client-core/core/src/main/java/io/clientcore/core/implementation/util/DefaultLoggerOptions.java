// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.clientcore.core.implementation.util;

import io.clientcore.core.util.LoggingOptions;

import java.io.PrintStream;

import static io.clientcore.core.util.ClientLogger.LogLevel;

public final class DefaultLoggerOptions extends LoggingOptions {
    private final PrintStream logLocation;
    private final LogLevel logLevel;
    public DefaultLoggerOptions(LogLevel logLevel, PrintStream logLocation) {
        this.logLocation = logLocation;
        this.logLevel = logLevel;
    }

    public PrintStream getLogLocation() {
        return logLocation;
    }

    @Override
    public LogLevel getLogLevel() {
        return logLevel;
    }
}
