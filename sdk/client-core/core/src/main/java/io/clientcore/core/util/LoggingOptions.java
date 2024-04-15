// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.clientcore.core.util;

import io.clientcore.core.implementation.util.EnvironmentConfiguration;
import io.clientcore.core.util.configuration.Configuration;

import java.util.Objects;

/**
 * The options for logging.
 */
public class LoggingOptions {
    private final ClientLogger.LogLevel level;
    private final Class<? extends LoggerProvider> loggerProviderClass;

    private LoggingOptions(ClientLogger.LogLevel level, Class<? extends LoggerProvider> loggerProviderClass) {
        this.loggerProviderClass = loggerProviderClass;
        this.level = level;
    }

    /**
     * Creates an instance of LoggingOptions.
     */
    public LoggingOptions() {
        this(levelFromEnvironment(), providerFromEnvironment());
    }

    /**
     * Gets the log level.
     * @return The log level.
     */
    public ClientLogger.LogLevel getLogLevel() {
        return level;
    }

    /**
     * Gets the logger provider class.
     * @return The logger provider class.
     */
    public Class<? extends LoggerProvider> getLoggerProviderClass() {
        return loggerProviderClass;
    }

    private static ClientLogger.LogLevel levelFromEnvironment() {
        // LogLevel is so basic, we can't use configuration to read it (since Configuration needs to log too)
        String level = EnvironmentConfiguration.getGlobalConfiguration().get(Configuration.PROPERTY_LOG_LEVEL);
        return ClientLogger.LogLevel.fromString(level);
    }

    private static Class<? extends LoggerProvider> providerFromEnvironment() {
        // LogLevel is so basic, we can't use configuration to read it (since Configuration needs to log too)
        String className = EnvironmentConfiguration.getGlobalConfiguration().get(Configuration.PROPERTY_LOGGING_IMPLEMENTATION);
        return className != null ? getClassByName(className) : null;
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<? extends T> getClassByName(String className) {
        Objects.requireNonNull(className, "'className' cannot be null");
        try {
            return (Class<? extends T>) Class.forName(className, false, LoggingOptions.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class '" + className + "' is not found on the classpath.", e);
        }
    }
}
