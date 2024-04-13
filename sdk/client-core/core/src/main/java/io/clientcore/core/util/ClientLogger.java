// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.clientcore.core.util;

import io.clientcore.core.annotation.Metadata;
import io.clientcore.core.implementation.util.CoreUtils;
import io.clientcore.core.implementation.util.DefaultLogger;
import io.clientcore.core.util.configuration.Configuration;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static io.clientcore.core.annotation.TypeConditions.FLUENT;

/**
 * This is a fluent logger helper class that wraps a pluggable {@link java.util.logging.Logger}.
 *
 * <p>This logger logs format-able messages that use {@code {}} as the placeholder. When a {@link Throwable throwable}
 * is the last argument of the format varargs and the logger is enabled for the stack trace for the throwable is
 * logged.</p>
 *
 * <p>A minimum logging level threshold is determined by the
 * {@link Configuration#PROPERTY_LOG_LEVEL AZURE_LOG_LEVEL} environment configuration. By default logging is
 * <b>disabled</b>.</p>
 *
 * <p>The logger is capable of producing json-formatted messages enriched with key value pairs.
 * Context can be provided in the constructor and populated on every message or added per each log record.</p>
 * @see Configuration
 */
public class ClientLogger {
    private static final LoggingOptions DEFAULT_OPTIONS = new LoggingOptions();
    private final LoggerSpi logger;
    private static final char CR = '\r';
    private static final char LF = '\n';

    private final Map<String, Object> globalContext;
    /**
     * Retrieves a logger for the passed class using the {@link java.util.logging.Logger#getLogger(String)}.
     *
     * @param clazz Class creating the logger.
     */
    public ClientLogger(Class<?> clazz) {
        this(clazz.getName());
    }

    /**
     * Retrieves a logger for the passed class name using the {@link java.util.logging.Logger#getLogger(String)}.
     *
     * @param className Class name creating the logger.
     * @throws RuntimeException when logging configuration is invalid.
     */
    public ClientLogger(String className) {
        this(className, Collections.emptyMap());
    }

    /**
     * Retrieves a logger for the passed class using the {@link java.util.logging.Logger#getLogger(String)}.
     *
     * @param clazz Class creating the logger.
     * @param context Context to be populated on every log record written with this logger.
     * Objects are serialized with {@code toString()} method.
     */
    public ClientLogger(Class<?> clazz, Map<String, Object> context) {
        this(clazz.getName(), context);
    }

    /**
     * Retrieves a logger for the passed class name using the {@link java.util.logging.Logger#getLogger(String)} with
     * context that will be populated on all log records produced with this logger.
     *
     * <p><strong>Code samples</strong></p>
     *
     * <p>Logging with context.</p>
     * * <!-- src_embed io.clientcore.core.util.logging.clientlogger#globalcontext -->
     * <pre>
     * Map&lt;String, Object&gt; context = new HashMap&lt;&gt;&#40;&#41;;
     * context.put&#40;&quot;connectionId&quot;, &quot;95a47cf&quot;&#41;;
     *
     * ClientLogger loggerWithContext = new ClientLogger&#40;ClientLoggerJavaDocCodeSnippets.class, context&#41;;
     * loggerWithContext.info&#40;&quot;A formattable message. Hello, &#123;&#125;&quot;, name&#41;;
     * </pre>
     * <!-- end io.clientcore.core.util.logging.clientlogger#globalcontext -->
     *
     * @param className Class name creating the logger.
     * @param context Context to be populated on every log record written with this logger.
     * Objects are serialized with {@code toString()} method.
     * @throws RuntimeException when logging configuration is invalid.
     */
    public ClientLogger(String className, Map<String, Object> context) {
        logger = LoggerProvider.getProvider().createLogger(className, DEFAULT_OPTIONS);
        globalContext = context;
    }

    /**
     * Retrieves a logger for the passed class using the {@link java.util.logging.Logger#getLogger(String)} with
     * context that will be populated on all log records produced with this logger.
     *
     * @param className Class name creating the logger.
     * @param context Context to be populated on every log record written with this logger.
     * @param options The logging options to use.
     * Objects are serialized with {@code toString()} method.
     * @throws RuntimeException when logging configuration is invalid.
     */
    ClientLogger(String className, Map<String, Object> context, LoggingOptions options) {
        this.logger = LoggerProvider.getProvider().createLogger(className, options);
        this.globalContext = context;
    }

    /**
     * Logs the {@link Throwable} at the warning level and returns it to be thrown.
     * <p>
     * This API covers the cases where a checked exception type needs to be thrown and logged.
     *
     * @param throwable Throwable to be logged and returned.
     * @param <T> Type of the Throwable being logged.
     * @return The passed {@link Throwable}.
     * @throws NullPointerException If {@code throwable} is {@code null}.
     */
    public <T extends Throwable> T logThrowableAsWarning(T throwable) {
        Objects.requireNonNull(throwable, "'throwable' cannot be null.");
        if (logger.isEnabled(LogLevel.WARNING)) {
            LoggingEventBuilder.create(logger, LogLevel.WARNING, globalContext, true)
                .log(throwable.getMessage(), throwable);
        }

        return throwable;
    }

    /**
     * Logs the {@link Throwable} at the error level and returns it to be thrown.
     * <p>
     * This API covers the cases where a checked exception type needs to be thrown and logged.
     *
     * @param throwable Throwable to be logged and returned.
     * @param <T> Type of the Throwable being logged.
     * @return The passed {@link Throwable}.
     * @throws NullPointerException If {@code throwable} is {@code null}.
     */
    public <T extends Throwable> T logThrowableAsError(T throwable) {
        Objects.requireNonNull(throwable, "'throwable' cannot be null.");
        if (logger.isEnabled(LogLevel.ERROR)) {
            LoggingEventBuilder.create(logger, LogLevel.ERROR, globalContext, true)
                .log(throwable.getMessage(), throwable);
        }
        return throwable;
    }

    /**
     * Determines if the app or environment logger support logging at the given log level.
     *
     * @param logLevel Logging level for the log message.
     * @return Flag indicating if the environment and logger are configured to support logging at the given log level.
     */
    public boolean canLogAtLevel(LogLevel logLevel) {
        return logger.isEnabled(logLevel);
    }

    /**
     * Creates {@link LoggingEventBuilder} for {@code error} log level that can be
     * used to enrich log with additional context.
     * <p><strong>Code samples</strong></p>
     *
     * <p>Logging with context at error level.</p>
     *
     * <!-- src_embed io.clientcore.core.util.logging.clientlogger.atverbose.addKeyValue#primitive -->
     * <pre>
     * logger.atVerbose&#40;&#41;
     *     .addKeyValue&#40;&quot;key&quot;, 1L&#41;
     *     .log&#40;&quot;A structured log message.&quot;&#41;;
     * </pre>
     * <!-- end io.clientcore.core.util.logging.clientlogger.atverbose.addKeyValue#primitive -->
     *
     * @return instance of {@link LoggingEventBuilder}  or no-op if error logging is disabled.
     */
    public LoggingEventBuilder atError() {
        return LoggingEventBuilder.create(logger, LogLevel.ERROR, globalContext, canLogAtLevel(LogLevel.ERROR));
    }

    /**
     * Creates {@link LoggingEventBuilder} for {@code warning} log level that can be
     * used to enrich log with additional context.

     * <p><strong>Code samples</strong></p>
     *
     * <p>Logging with context at warning level.</p>
     *
     * <!-- src_embed io.clientcore.core.util.logging.clientlogger.atWarning -->
     * <pre>
     * logger.atWarning&#40;&#41;
     *     .addKeyValue&#40;&quot;key&quot;, &quot;value&quot;&#41;
     *     .log&#40;&quot;A structured log message with exception.&quot;, exception&#41;;
     * </pre>
     * <!-- end io.clientcore.core.util.logging.clientlogger.atWarning -->
     *
     * @return instance of {@link LoggingEventBuilder} or no-op if warn logging is disabled.
     */
    public LoggingEventBuilder atWarning() {
        return LoggingEventBuilder.create(logger, LogLevel.WARNING, globalContext,
            canLogAtLevel(LogLevel.WARNING));
    }

    /**
     * Creates {@link LoggingEventBuilder} for {@code info} log level that can be
     * used to enrich log with additional context.
     *
     * <p><strong>Code samples</strong></p>
     *
     * <p>Logging with context at info level.</p>
     *
     * <!-- src_embed io.clientcore.core.util.logging.clientlogger.atInfo -->
     * <pre>
     * logger.atInfo&#40;&#41;
     *     .addKeyValue&#40;&quot;key&quot;, &quot;value&quot;&#41;
     *     .addKeyValue&#40;&quot;hello&quot;, name&#41;
     *     .log&#40;&quot;A structured log message.&quot;&#41;;
     * </pre>
     * <!-- end io.clientcore.core.util.logging.clientlogger.atInfo -->
     *
     * @return instance of {@link LoggingEventBuilder} or no-op if info logging is disabled.
     */
    public LoggingEventBuilder atInfo() {
        return LoggingEventBuilder.create(logger, LogLevel.INFORMATIONAL, globalContext,
            canLogAtLevel(LogLevel.INFORMATIONAL));
    }

    /**
     * Creates {@link LoggingEventBuilder} for {@code verbose} log level that can be
     * used to enrich log with additional context.
     * <p><strong>Code samples</strong></p>
     *
     * <p>Logging with context at verbose level.</p>
     *
     * <!-- src_embed io.clientcore.core.util.logging.clientlogger.atverbose.addKeyValue#primitive -->
     * <pre>
     * logger.atVerbose&#40;&#41;
     *     .addKeyValue&#40;&quot;key&quot;, 1L&#41;
     *     .log&#40;&quot;A structured log message.&quot;&#41;;
     * </pre>
     * <!-- end io.clientcore.core.util.logging.clientlogger.atverbose.addKeyValue#primitive -->
     *
     * @return instance of {@link LoggingEventBuilder} or no-op if verbose logging is disabled.
     */
    public LoggingEventBuilder atVerbose() {
        return LoggingEventBuilder.create(logger, LogLevel.VERBOSE, globalContext,
            canLogAtLevel(LogLevel.VERBOSE));
    }

    /**
     * Creates {@link LoggingEventBuilder} for log level that can be
     * used to enrich log with additional context.
     *
     * <p><strong>Code samples</strong></p>
     *
     * <p>Logging with context at provided level.</p>
     *
     * <!-- src_embed io.clientcore.core.util.logging.clientlogger.atLevel -->
     * <pre>
     * ClientLogger.LogLevel level = response.getStatusCode&#40;&#41; == 200
     *     ? ClientLogger.LogLevel.INFORMATIONAL : ClientLogger.LogLevel.WARNING;
     * logger.atLevel&#40;level&#41;
     *     .addKeyValue&#40;&quot;key&quot;, &quot;value&quot;&#41;
     *     .log&#40;&quot;message&quot;&#41;;
     * </pre>
     * <!-- end io.clientcore.core.util.logging.clientlogger.atLevel -->
     *
     * @param level log level.
     * @return instance of {@link LoggingEventBuilder} or no-op if logging at provided level is disabled.
     */
    public LoggingEventBuilder atLevel(LogLevel level) {
        return LoggingEventBuilder.create(logger, level, globalContext,
            canLogAtLevel(level));
    }

    /**
     * This class provides fluent API to write logs using {@link ClientLogger} and
     * enrich them with additional context.
     *
     * <p><strong>Code samples</strong></p>
     *
     * <p>Logging event with context.</p>
     *
     * <!-- src_embed io.clientcore.core.util.logging.loggingeventbuilder -->
     * <pre>
     * logger.atInfo&#40;&#41;
     *     .addKeyValue&#40;&quot;key1&quot;, &quot;value1&quot;&#41;
     *     .addKeyValue&#40;&quot;key2&quot;, true&#41;
     *     .addKeyValue&#40;&quot;key3&quot;, this::getName&#41;
     *     .log&#40;&quot;A structured log message.&quot;&#41;;
     * </pre>
     * <!-- end io.clientcore.core.util.logging.loggingeventbuilder -->
     */
    @Metadata(conditions = FLUENT)
    public static final class LoggingEventBuilder {
        private static final LoggingEventBuilder NOOP = new LoggingEventBuilder(null, null, null, false);

        private final LoggerSpi logger;
        private final LogLevel level;
        private List<LoggerSpi.LoggingAttribute> attributes;
        private Context context;


        // Flag for no-op instance instead of inheritance
        private final boolean isEnabled;

        /**
         * Creates {@code LoggingEventBuilder} for provided level and  {@link ClientLogger}.
         * If level is disabled, returns no-op instance.
         */
        static LoggingEventBuilder create(LoggerSpi logger, LogLevel level, Map<String, Object> globalContext,
                                          boolean canLogAtLevel) {
            if (canLogAtLevel) {
                return new LoggingEventBuilder(logger, level, globalContext, true);
            }

            return NOOP;
        }

        private LoggingEventBuilder(LoggerSpi logger, LogLevel level, Map<String, Object> globalContext, boolean isEnabled) {
            this.logger = logger;
            this.level = level;
            this.isEnabled = isEnabled;
            if (isEnabled && globalContext != null) {
                attributes = new ArrayList<>(globalContext.size());
                for (Map.Entry<String, Object> entry : globalContext.entrySet()) {
                    attributes.add(LoggerSpi.LoggingAttribute.fromValue(entry.getKey(), entry.getValue()));
                }
            }
        }

        /**
         * Adds key with String value pair to the context of current log being created.
         *
         * <p><strong>Code samples</strong></p>
         *
         * <p>Adding string value to logging event context.</p>
         *
         * <!-- src_embed io.clientcore.core.util.logging.clientlogger.atInfo -->
         * <pre>
         * logger.atInfo&#40;&#41;
         *     .addKeyValue&#40;&quot;key&quot;, &quot;value&quot;&#41;
         *     .addKeyValue&#40;&quot;hello&quot;, name&#41;
         *     .log&#40;&quot;A structured log message.&quot;&#41;;
         * </pre>
         * <!-- end io.clientcore.core.util.logging.clientlogger.atInfo -->
         *
         * @param key String key.
         * @param value String value.
         * @return The updated {@code LoggingEventBuilder} object.
         */
        public LoggingEventBuilder addKeyValue(String key, String value) {
            if (this.isEnabled) {
                addKeyValueInternal(key, value);
            }

            return this;
        }

        /**
         * Adds context to the log message.
         *
         * @param context The context to add to the log message.
         * @return The updated {@link LoggingEventBuilder} object.
         */
        public LoggingEventBuilder withContext(Context context) {
            this.context = context;
            return this;
        }

        /**
         * Adds key with Object value to the context of current log being created.
         * If logging is enabled at given level, and object is not null, uses {@code value.toString()} to
         * serialize object.
         *
         * <p><strong>Code samples</strong></p>
         *
         * <p>Adding string value to logging event context.</p>
         *
         * <!-- src_embed io.clientcore.core.util.logging.clientlogger.atverbose.addKeyValue#object -->
         * <pre>
         * logger.atVerbose&#40;&#41;
         *     &#47;&#47; equivalent to addKeyValue&#40;&quot;key&quot;, &#40;&#41; -&gt; new LoggableObject&#40;&quot;string representation&quot;&#41;.toString&#40;&#41;
         *     .addKeyValue&#40;&quot;key&quot;, new LoggableObject&#40;&quot;string representation&quot;&#41;&#41;
         *     .log&#40;&quot;A structured log message.&quot;&#41;;
         * </pre>
         * <!-- end io.clientcore.core.util.logging.clientlogger.atverbose.addKeyValue#object -->
         *
         * @param key String key.
         * @param value Object value.
         * @return The updated {@code LoggingEventBuilder} object.
         */
        public LoggingEventBuilder addKeyValue(String key, Object value) {
            if (this.isEnabled) {
                // Previously this eagerly called toString() on the value, but that can be expensive and unnecessary.
                // This is now deferred until the value is being logged, which was calling toString() anyway.
                addKeyValueInternal(key, value);
            }

            return this;
        }

        /**
         * Adds a key with a boolean value to the context of the current log being created.
         *
         * @param key Key to associate the provided {@code value} with.
         * @param value The boolean value.
         * @return The updated {@link LoggingEventBuilder} object.
         */
        public LoggingEventBuilder addKeyValue(String key, boolean value) {
            if (this.isEnabled) {
                addKeyValueInternal(key, value);
            }
            return this;
        }

        /**
         * Adds key with long value to the context of current log event being created.
         *
         * <p><strong>Code samples</strong></p>
         *
         * <p>Adding a long value to the logging event context.</p>
         *
         * <!-- src_embed io.clientcore.core.util.logging.clientlogger.atverbose.addKeyValue#primitive -->
         * <pre>
         * logger.atVerbose&#40;&#41;
         *     .addKeyValue&#40;&quot;key&quot;, 1L&#41;
         *     .log&#40;&quot;A structured log message.&quot;&#41;;
         * </pre>
         * <!-- end io.clientcore.core.util.logging.clientlogger.atverbose.addKeyValue#primitive -->
         *
         * @param key Key to associate the provided {@code value} with.
         * @param value The long value.
         * @return The updated {@link LoggingEventBuilder} object.
         */
        public LoggingEventBuilder addKeyValue(String key, long value) {
            if (this.isEnabled) {
                addKeyValueInternal(key, value);
            }
            return this;
        }

        /**
         * Adds key with String value supplier to the context of current log event being created.
         *
         * @param key String key.
         * @param valueSupplier String value supplier function.
         * @return The updated {@code LoggingEventBuilder} object.
         */
        public LoggingEventBuilder addKeyValue(String key, Supplier<Object> valueSupplier) {
            if (this.isEnabled) {
                if (this.attributes == null) {
                    this.attributes = new ArrayList<>(1);
                }

                this.attributes.add(LoggerSpi.LoggingAttribute.fromSupplier(key, valueSupplier));
            }
            return this;
        }

        /**
         * Logs message annotated with context.
         *
         * @param message log message.
         */
        public void log(String message) {
            if (this.isEnabled) {
                message = removeNewLinesFromLogMessage(message);
                logger.log(level, message, null, attributes, context);
            }
        }

        /**
         * Logs message annotated with context.
         *
         * @param message log message.
         * @param throwable {@link Throwable} for the message.
         * @param <T> Type of the Throwable being logged.
         *
         * @return The passed {@link Throwable}.
         */
        public <T extends Throwable> T log(String message, T throwable) {
            if (this.isEnabled) {
                message = removeNewLinesFromLogMessage(message);
                if (throwable != null) {
                    addKeyValueInternal("exception.message", throwable.getMessage());
                    if (logger.isEnabled(LogLevel.VERBOSE)) {
                        addKeyValue("exception.stacktrace", getStackTrace(throwable));
                    }
                }

                logger.log(level, message, logger.isEnabled(LogLevel.VERBOSE) ? throwable : null, attributes, context);
            }
            return throwable;
        }

        private String getStackTrace(Throwable t) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            return sw.toString().trim();
        }

        private void addKeyValueInternal(String key, Object value) {
            if (this.attributes == null) {
                this.attributes = new ArrayList<>(1);
            }
            this.attributes.add(LoggerSpi.LoggingAttribute.fromValue(key, value));
        }
    }

    /**
     * Enum which represent logging levels used in Azure SDKs.
     */
    public enum LogLevel {
        /**
         * Indicates that no log level is set.
         */
        NOTSET(0, "0", "notSet"),

        /**
         * Indicates that log level is at verbose level.
         */
        VERBOSE(1, "1", "verbose", "debug"),

        /**
         * Indicates that log level is at information level.
         */
        INFORMATIONAL(2, "2", "info", "information", "informational"),

        /**
         * Indicates that log level is at warning level.
         */
        WARNING(3, "3", "warn", "warning"),

        /**
         * Indicates that log level is at error level.
         */
        ERROR(4, "4", "err", "error");
        private final int numericValue;
        private final String[] allowedLogLevelVariables;
        private static final HashMap<String, LogLevel> LOG_LEVEL_STRING_MAPPER = new HashMap<>();
        private final String caseSensitive;

        static {
            for (LogLevel logLevel: LogLevel.values()) {
                for (String val: logLevel.allowedLogLevelVariables) {
                    LOG_LEVEL_STRING_MAPPER.put(val, logLevel);
                }
            }
        }

        LogLevel(int numericValue, String... allowedLogLevelVariables) {
            this.numericValue = numericValue;
            this.allowedLogLevelVariables = allowedLogLevelVariables;
            this.caseSensitive = allowedLogLevelVariables[0];
        }

        /**
         * Converts the passed log level string to the corresponding {@link LogLevel}.
         *
         * @param logLevelVal The log level value which needs to convert
         * @return The LogLevel Enum if pass in the valid string.
         * The valid strings for {@link LogLevel} are:
         * <ul>
         * <li>VERBOSE: "verbose", "debug"</li>
         * <li>INFO: "info", "information", "informational"</li>
         * <li>WARNING: "warn", "warning"</li>
         * <li>ERROR: "err", "error"</li>
         * </ul>
         * Returns NOT_SET if null is passed in.
         * @throws IllegalArgumentException if the log level value is invalid.
         */
        public static LogLevel fromString(String logLevelVal) {
            if (logLevelVal == null) {
                return LogLevel.NOTSET;
            }
            String caseInsensitiveLogLevel = logLevelVal.toLowerCase(Locale.ROOT);
            if (!LOG_LEVEL_STRING_MAPPER.containsKey(caseInsensitiveLogLevel)) {
                throw new IllegalArgumentException("We currently do not support the log level you set. LogLevel: "
                    + logLevelVal);
            }
            return LOG_LEVEL_STRING_MAPPER.get(caseInsensitiveLogLevel);
        }

        /**
         * Converts the log level to a string representation.
         *
         * @return The string representation of the log level.
         */
        public String toString() {
            return caseSensitive;
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
