// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.clientcore.core.implementation.util;

import io.clientcore.core.json.implementation.jackson.core.io.JsonStringEncoder;
import io.clientcore.core.util.Context;
import io.clientcore.core.util.LoggerSpi;
import io.clientcore.core.util.LoggingOptions;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import static io.clientcore.core.util.ClientLogger.LogLevel;

/**
 * This class is an internal implementation of logger.
 */
public final class JULLogger implements LoggerSpi {
    private static final char CR = '\r';
    private static final char LF = '\n';
    private static final String SDK_LOG_MESSAGE_KEY = "{\"message\":\"";
    private static final JsonStringEncoder JSON_STRING_ENCODER = JsonStringEncoder.getInstance();
    private final String canonicalName;
    private final Logger logger;
    private final StreamHandler streamHandler;

    public JULLogger(String className, LoggingOptions options) {
        Class<?> clazz = getClassPathFromClassName(className);
        canonicalName = clazz == null ? className : clazz.getCanonicalName();
        logger = Logger.getLogger(canonicalName);
        streamHandler = configureCaptureStreamIfNeeded(logger, options);
    }

    private static StreamHandler configureCaptureStreamIfNeeded(Logger logger, LoggingOptions options) {
        PrintStream logLocation = System.out;
        // this is effectively a test code - it allows to capture the logs into a stream
        // which is only possible via specific logging options
        if (options instanceof DefaultLogger.Options) {
            DefaultLogger.Options defaultOptions = (DefaultLogger.Options) options;
            logLocation = defaultOptions.getLogLocation() == null ? System.out : defaultOptions.getLogLocation();


            StreamHandler streamHandler = new StreamHandler(logLocation, new SimpleFormatter());
            // this allows handler to get all messages that are enabled on the logger.
            streamHandler.setLevel(Level.ALL);
            logger.addHandler(streamHandler);

            return streamHandler;
        }
        return null;
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


    public String getName() {
        return canonicalName;
    }

    @Override
    public boolean isEnabled(LogLevel level) {
        if (level == null) {
            return false;
        }
        switch (level) {
            case VERBOSE:
                return logger.isLoggable(Level.FINE);
            case INFORMATIONAL:
                return logger.isLoggable(Level.INFO);
            case WARNING:
                return logger.isLoggable(Level.WARNING);
            case ERROR:
                return logger.isLoggable(Level.SEVERE);
            default:
                return false;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void log(Instant eventTime, LogLevel level, String body, Throwable throwable, List<LoggingAttribute> attributes, Context context) {
        Level julLevel = toJulLevel(level);
        if (logger.isLoggable(julLevel)) {
            LogRecord record = new LogRecord(julLevel, getMessageWithContext(attributes, body));
            record.setThrown(logger.isLoggable(Level.FINE) ? throwable : null);
            record.setLoggerName(canonicalName);
            record.setSourceClassName(canonicalName);

            // TODO:
            record.setMillis(eventTime.toEpochMilli());
            //record.setInstant(eventTime);

            logger.log(record);
            if (streamHandler != null) {
                streamHandler.flush();
            }
        }
    }

    private String getMessageWithContext(List<LoggingAttribute> attributes, String message) {
        if (message == null) {
            message = "";
        }
        message = removeNewLinesFromLogMessage(message);

        StringBuilder sb = new StringBuilder(20 + (attributes == null ? 0 : attributes.size()) * 20 + message.length());
        // message must be first for log parsing tooling to work, key also works as a
        // marker for SDK logs so we'll write it even if there is no message
        sb.append(SDK_LOG_MESSAGE_KEY);
        JSON_STRING_ENCODER.quoteAsString(message, sb);
        sb.append('"');

        if (attributes != null) {
            for (LoggingAttribute attribute : attributes) {
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
            return null;
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

    /**
     * Removes CR, LF or CRLF pattern in the {@code logMessage}.
     *
     * @param logMessage The log message to sanitize.
     * @return The updated logMessage.
     */
    private static String removeNewLinesFromLogMessage(String logMessage) {
        if (CoreUtils.isNullOrEmpty(logMessage)) {
            return logMessage;
        }

        StringBuilder sb = null;
        int prevStart = 0;

        for (int i = 0; i < logMessage.length(); i++) {
            if (logMessage.charAt(i) == CR || logMessage.charAt(i) == LF) {
                if (sb == null) {
                    sb = new StringBuilder(logMessage.length());
                }

                if (prevStart != i) {
                    sb.append(logMessage, prevStart, i);
                }
                prevStart = i + 1;
            }
        }

        if (sb == null) {
            return logMessage;
        }
        sb.append(logMessage, prevStart, logMessage.length());
        return sb.toString();
    }
}
