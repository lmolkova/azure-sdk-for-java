// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.clientcore.core.implementation.util;

import io.clientcore.core.util.ClientLogger;
import io.clientcore.core.util.Context;
import io.clientcore.core.util.LoggerProvider;
import io.clientcore.core.util.LoggerSpi;
import io.clientcore.core.util.LoggingOptions;

import java.time.Instant;
import java.util.List;

public final class LoggerProviderImpl implements LoggerProvider {
    private static final LoggerSpi NOOP_LOGGER = new NoopLogger();
    private static final LoggerProvider INSTANCE = new LoggerProviderImpl();
    private static final Providers<LoggerProvider> LOGGER_PROVIDERS
            = new Providers<>(LoggerProvider.class, null, "TODO", null);

    private LoggerProviderImpl() {
    }

    public static LoggerProvider getInstance() {
        return INSTANCE;
    }

    private static LoggerSpi getFallback(String className, LoggingOptions options) {
        if (options.getLogLevel() == ClientLogger.LogLevel.NOTSET) {
            return new JULLogger(className, options);
        }

        return NOOP_LOGGER;
    }

    public LoggerSpi createLogger(String className, LoggingOptions options) {
        LoggerSpi resolvedLogger = LOGGER_PROVIDERS.create(
                (provider) -> provider.createLogger(className, options),
                () -> getFallback(className, options), options.getLoggerProviderClass());
        if (options.getLogLevel() == ClientLogger.LogLevel.NOTSET) {
            return resolvedLogger;
        }
        return new CompositeLogger(resolvedLogger, new DefaultLogger(className, options));
    }

    private static class CompositeLogger implements LoggerSpi {
        private final LoggerSpi[] loggers;

        CompositeLogger(LoggerSpi... loggers) {
            this.loggers = loggers;
        }

        @Override
        public boolean isEnabled(ClientLogger.LogLevel level) {
            for (LoggerSpi logger : loggers) {
                if (logger.isEnabled(level)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public void log(Instant eventTime, ClientLogger.LogLevel level, String body, Throwable throwable, List<LoggingAttribute> attributes, Context context) {
            for (LoggerSpi logger : loggers) {
                if (logger.isEnabled(level)) {
                    logger.log(eventTime, level, body, throwable, attributes, context);
                }
            }
        }
    }

    private static class NoopLogger implements LoggerSpi {
        @Override
        public boolean isEnabled(ClientLogger.LogLevel level) {
            return false;
        }

        @Override
        public void log(Instant eventTime, ClientLogger.LogLevel level, String body, Throwable throwable, List<LoggingAttribute> attributes, Context context) {
        }
    }
}
