// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.clientcore.core.implementation.util;

import io.clientcore.core.json.implementation.jackson.core.io.JsonStringEncoder;
import io.clientcore.core.util.ClientLogger;
import io.clientcore.core.util.Context;
import io.clientcore.core.util.configuration.Configuration;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

import static io.clientcore.core.util.ClientLogger.LogLevel;

/**
 * This class is an internal implementation of logger.
 */
public final class DefaultLogger {
    private static final String SDK_LOG_MESSAGE_KEY = "{\"message\":\"";
    private static final JsonStringEncoder JSON_STRING_ENCODER = JsonStringEncoder.getInstance();
    private final String canonicalName;
    private final java.util.logging.Logger logger;
    private StreamHandler streamHandler = null;
    public DefaultLogger(Class<?> clazz) {
        this(clazz.getCanonicalName(), null, fromEnvironment());
    }

    public DefaultLogger(String className) {
        this(className, null, fromEnvironment());
    }

    public DefaultLogger(String className, PrintStream logLocation, ClientLogger.LogLevel logLevel) {
        Class<?> clazz = getClassPathFromClassName(className);
        canonicalName = clazz == null ? className : clazz.getCanonicalName();
        this.logger = java.util.logging.Logger.getLogger(canonicalName);
        if (logLevel != LogLevel.NOTSET) {
            Level level = toJulLevel(logLevel);
            logger.setLevel(level);
        }

        if (logLocation != null) {
            streamHandler = new StreamHandler(logLocation, new DefaultFormatter());
            // this allows handler to get all messages that are enabled on the logger.
            streamHandler.setLevel(Level.ALL);
            logger.addHandler(streamHandler);
        }
     }

    private static Class<?> getClassPathFromClassName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException | InvalidPathException e) {
            // Swallow ClassNotFoundException as the passed class name may not correlate to an actual class.
            // Swallow InvalidPathException as the className may contain characters that aren't legal file characters.
            return null;
        }
    }

    private static LogLevel fromEnvironment() {
        // LogLevel is so basic, we can't use configuration to read it (since Configuration needs to log too)
        String level = EnvironmentConfiguration.getGlobalConfiguration().get(Configuration.PROPERTY_LOG_LEVEL);
        return LogLevel.fromString(level);
    }

    public String getName() {
        return canonicalName;
    }

    public boolean isEnabled(LogLevel level) {
        if (level == LogLevel.NOTSET || level == null) {
            return false;
        }

        return logger.isLoggable(toJulLevel(level));
    }

    public void log(LogLevel level, String body, Throwable throwable, List<ClientLogger.LoggingAttribute> attributes, Context context) {
        if (isEnabled(level)) {
            LogRecord record = new LogRecord(toJulLevel(level), getMessageWithContext(attributes, body));
            record.setThrown(logger.isLoggable(Level.FINE) ? throwable : null);
            record.setLoggerName(canonicalName);
            record.setSourceClassName(canonicalName);

            logger.log(record);
            if (streamHandler != null) {
                streamHandler.flush();
            }
        }
    }

    private String getMessageWithContext(List<ClientLogger.LoggingAttribute> attributes, String message) {
        if (message == null) {
            message = "";
        }

        StringBuilder sb = new StringBuilder(20 + (attributes == null ? 0 : attributes.size()) * 20 + message.length());
        // message must be first for log parsing tooling to work, key also works as a
        // marker for SDK logs so we'll write it even if there is no message
        sb.append(SDK_LOG_MESSAGE_KEY);
        JSON_STRING_ENCODER.quoteAsString(message, sb);
        sb.append('"');

        if (attributes != null) {
            for (ClientLogger.LoggingAttribute attribute : attributes) {
                writeKeyAndValue(attribute.getKey(), attribute.getValue(), sb.append(','));
            }
        }

        sb.append('}');
        return sb.toString();
    }


    private static void writeKeyAndValue(String key, Object value, StringBuilder formatter) {
        formatter.append('"');
        JSON_STRING_ENCODER.quoteAsString(key, formatter);
        formatter.append("\":");

        if (value == null) {
            formatter.append("null");
        } else if (isUnquotedType(value)) {
            JSON_STRING_ENCODER.quoteAsString(value.toString(), formatter);
        } else {
            formatter.append('"');
            JSON_STRING_ENCODER.quoteAsString(value.toString(), formatter);
            formatter.append('"');
        }
    }

    private static boolean isUnquotedType(Object value) {
        return value instanceof Boolean || value instanceof Number;
    }

    private static Level toJulLevel(LogLevel level) {
        if (level == null) {
            return Level.OFF;
        }

        switch (level) {
            case VERBOSE:
                return Level.FINE;
            case INFORMATIONAL:
                return Level.INFO;
            case WARNING:
                return Level.WARNING;
            case ERROR:
                return Level.SEVERE;
            default:
                return Level.OFF;
        }
    }

    private static class DefaultFormatter extends Formatter {
        // The template for the log message:
        // YYYY-MM-DD HH:MM:ss.SSS [thread] [level] classpath - message
        // E.g: 2020-01-09 12:35:14.232 [main] [WARN] com.azure.core.DefaultLogger - This is my log message.
        private static final String WHITESPACE = " ";
        private static final String HYPHEN = " - ";
        private static final String OPEN_BRACKET = " [";
        private static final String CLOSE_BRACKET = "]";


        @Override
        public String format(LogRecord record) {
            String dateTime = getFormattedDate();
            String threadName = Thread.currentThread().getName();
            // Use a larger initial buffer for the StringBuilder as it defaults to 16 and non-empty information is expected
            // to be much larger than that. This will reduce the amount of resizing and copying needed to be done.
            StringBuilder stringBuilder = new StringBuilder(256);
            stringBuilder.append(dateTime)
                    .append(OPEN_BRACKET)
                    .append(threadName)
                    .append(CLOSE_BRACKET)
                    .append(OPEN_BRACKET)
                    .append(record.getLevel())
                    .append(CLOSE_BRACKET)
                    .append(WHITESPACE)
                    .append(record.getLoggerName())
                    .append(HYPHEN)
                    .append(record.getMessage())
                    .append(System.lineSeparator());

            return stringBuilder.toString();
        }


        /**
         * Get the current time in Local time zone.
         *
         * @return The current time in {@code DATE_FORMAT}
         */
        private static String getFormattedDate() {
            LocalDateTime now = LocalDateTime.now();

            // yyyy-MM-dd HH:mm:ss.SSS
            // 23 characters that will be ASCII
            byte[] bytes = new byte[23];

            // yyyy-
            int year = now.getYear();
            int round = year / 1000;
            bytes[0] = (byte) ('0' + round);
            year = year - (1000 * round);
            round = year / 100;
            bytes[1] = (byte) ('0' + round);
            year = year - (100 * round);
            round = year / 10;
            bytes[2] = (byte) ('0' + round);
            bytes[3] = (byte) ('0' + (year - (10 * round)));
            bytes[4] = '-';

            // MM-
            zeroPad(now.getMonthValue(), bytes, 5);
            bytes[7] = '-';

            // dd
            zeroPad(now.getDayOfMonth(), bytes, 8);
            bytes[10] = ' ';

            // HH:
            zeroPad(now.getHour(), bytes, 11);
            bytes[13] = ':';

            // mm:
            zeroPad(now.getMinute(), bytes, 14);
            bytes[16] = ':';

            // ss.
            zeroPad(now.getSecond(), bytes, 17);
            bytes[19] = '.';

            // SSS
            int millis = now.get(ChronoField.MILLI_OF_SECOND);
            round = millis / 100;
            bytes[20] = (byte) ('0' + round);
            millis = millis - (100 * round);
            round = millis / 10;
            bytes[21] = (byte) ('0' + round);
            bytes[22] = (byte) ('0' + (millis - (10 * round)));

            // Use UTF-8 as it's more performant than ASCII in Java 8
            return new String(bytes, StandardCharsets.UTF_8);
        }

        private static void zeroPad(int value, byte[] bytes, int index) {
            if (value < 10) {
                bytes[index++] = '0';
                bytes[index] = (byte) ('0' + value);
            } else {
                int high = value / 10;
                bytes[index++] = (byte) ('0' + high);
                bytes[index] = (byte) ('0' + (value - (10 * high)));
            }
        }
    }

    private static boolean isJulConfigured() {
        return System.getProperty("java.util.logging.config.file") != null
            || System.getProperty("java.util.logging.config.class") != null;
    }
}
