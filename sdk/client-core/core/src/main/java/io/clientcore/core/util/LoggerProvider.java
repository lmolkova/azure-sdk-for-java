// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.clientcore.core.util;

import io.clientcore.core.implementation.util.DefaultLoggerProvider;

/**
 * The provider for the logger.
 */
public interface LoggerProvider {
    /**
     * Create a logger for the specified class.
     *
     * @param className The name of the class.
     * @return The logger.
     */
    LoggerSpi createLogger(String className, LoggingOptions options);

    /**
     * Get the provider.
     *
     * @return The provider.
     */
    static LoggerProvider getProvider() {
        return DefaultLoggerProvider.getInstance();
    }
}
