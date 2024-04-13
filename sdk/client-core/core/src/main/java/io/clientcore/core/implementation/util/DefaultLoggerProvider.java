// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.clientcore.core.implementation.util;

import io.clientcore.core.util.LoggerProvider;
import io.clientcore.core.util.LoggerSpi;
import io.clientcore.core.util.LoggingOptions;

public final class DefaultLoggerProvider implements LoggerProvider {
    private static final LoggerProvider INSTANCE = new DefaultLoggerProvider();
    private static final Providers<LoggerProvider, LoggerSpi> LOGGER_PROVIDERS
            = new Providers<>(LoggerProvider.class, null, "TODO", null);

    private DefaultLoggerProvider() {
    }

    public static LoggerProvider getInstance() {
        return INSTANCE;
    }

    public LoggerSpi createLogger(String className, LoggingOptions options) {
        return LOGGER_PROVIDERS.create(
                (provider) -> provider.createLogger(className, options),
                () -> new DefaultLogger(className, options), options.getLoggerProviderClass());
    }
}
