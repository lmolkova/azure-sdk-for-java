// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.clientcore.core.util;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

/**
 * The SPI for the logger.
 */
public interface LoggerSpi {

    /**
     * Check if the specified level is enabled.
     *
     * @param level The level to check.
     * @return True if the level is enabled, false otherwise.
     */
    boolean isEnabled(ClientLogger.LogLevel level);

    /**
     * Log the message with the specified level.
     *
     * @param level The level of the message.
     * @param body The message to log.
     * @param throwable The exception to log.
     * @param attributes The attributes to log.
     * @param context The context to log.
     */
    void log(Instant eventTime, ClientLogger.LogLevel level, String body, Throwable throwable, List<LoggingAttribute> attributes, Context context);

    /**
     * logging attribute
     */
    final class LoggingAttribute {
        private final String key;
        private final Object value;
        private final Supplier<Object> valueSupplier;

        private LoggingAttribute(String key, Object value, Supplier<Object> valueSupplier) {
            this.key = key;
            this.value = value;
            this.valueSupplier = valueSupplier;
        }

        /**
         * Creates a {@link LoggingAttribute} with a value.
         *
         * @param key The key of the attribute.
         * @param value The value of the attribute.
         * @return The created {@link LoggingAttribute}.
         */
        public static LoggingAttribute fromValue(String key, Object value) {
            return new LoggingAttribute(key, value, null);
        }

        /**
         * Creates a {@link LoggingAttribute} with a supplier for the value.
         *
         * @param key The key of the attribute.
         * @param supplier The supplier for the value of the attribute.
         * @return The created {@link LoggingAttribute}.
         */
        public static LoggingAttribute fromSupplier(String key, Supplier<Object> supplier) {
            return new LoggingAttribute(key, null, supplier);
        }

        /**
         * Gets the key of the attribute.
         *
         * @return The key of the attribute.
         */
        public String getKey() {
            return key;
        }

        /**
         * Gets the value of the attribute.
         *
         * @return The value of the attribute.
         */
        public Object getValue() {
            return valueSupplier != null ? valueSupplier.get() : value;
        }
    }
}
