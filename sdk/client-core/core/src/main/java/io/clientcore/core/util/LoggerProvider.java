package io.clientcore.core.util;

import io.clientcore.core.implementation.util.DefaultLoggerProvider;

public interface LoggerProvider {
    LoggerSpi createLogger(String className);

    static LoggerProvider getProvider() {
        return DefaultLoggerProvider.getInstance();
    }
}
