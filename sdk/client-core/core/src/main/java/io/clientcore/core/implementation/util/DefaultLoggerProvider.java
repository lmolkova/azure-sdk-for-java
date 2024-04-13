package io.clientcore.core.implementation.util;

import io.clientcore.core.util.LoggerProvider;
import io.clientcore.core.util.LoggerSpi;

public class DefaultLoggerProvider implements LoggerProvider {
    private static final LoggerProvider INSTANCE = new DefaultLoggerProvider();
    private static final Providers<LoggerProvider, LoggerSpi> LOGGER_PROVIDERS
            = new Providers<>(LoggerProvider.class, null, "TODO", null);

    private DefaultLoggerProvider() {
    }

    public static LoggerProvider getInstance() {
        return INSTANCE;
    }

    public LoggerSpi createLogger(String className) {
        return LOGGER_PROVIDERS.create(
                (provider) -> provider.createLogger(className), () -> new DefaultLogger(className), null);
    }
}
