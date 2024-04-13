package io.clientcore.core.opentelemetry.implementation;

import io.clientcore.core.util.ClientLogger;
import io.clientcore.core.util.Context;
import io.clientcore.core.util.LoggerSpi;
import io.clientcore.core.util.LoggingOptions;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

import java.util.List;

public class OpenTelemetryLogger implements LoggerSpi {
    private final Logger logger;
    private final LoggingOptions options;

    public OpenTelemetryLogger(String className, LoggingOptions options) {
        this.options = options;
        logger = GlobalOpenTelemetry.get().getLogsBridge().loggerBuilder(className)
                .build();
    }

    @Override
    public boolean isEnabled(ClientLogger.LogLevel level) {
        return options.getLogLevel().compareTo(level) <= 0;
    }

    @Override
    public void log(ClientLogger.LogLevel level, String body, Throwable throwable, List<LoggingAttribute> attributes, Context context) {
        LogRecordBuilder logRecord = logger.logRecordBuilder()
                .setContext(toOtelContext(context))
                .setBody(body)
                .setSeverity(toOtelSeverity(level));

        for (LoggingAttribute attribute : attributes) {
            setAttribute(logRecord, attribute.getKey(), attribute.getValue());
        }

        logRecord.emit();
    }

    private static io.opentelemetry.context.Context toOtelContext(Context context) {
        Object ctx = context.get("trace-context");
        if (ctx instanceof io.opentelemetry.context.Context) {
            return (io.opentelemetry.context.Context) ctx;
        }

        return null;
    }

    static Severity toOtelSeverity(ClientLogger.LogLevel level) {
        switch (level) {
            case VERBOSE:
                return Severity.DEBUG;
            case INFORMATIONAL:
                return Severity.INFO;
            case WARNING:
                return Severity.WARN;
            case ERROR:
                return Severity.ERROR;
            default:
                return Severity.UNDEFINED_SEVERITY_NUMBER;
        }
    }

    private ClientLogger.LogLevel toLogLevel(Severity severity) {
        switch (severity) {
            case TRACE:
            case TRACE2:
            case TRACE3:
            case TRACE4:
            case DEBUG:
            case DEBUG2:
            case DEBUG3:
            case DEBUG4:
                return ClientLogger.LogLevel.VERBOSE;
            case INFO:
            case INFO2:
            case INFO3:
            case INFO4:
                return ClientLogger.LogLevel.INFORMATIONAL;
            case WARN:
            case WARN2:
            case WARN3:
            case WARN4:
                return ClientLogger.LogLevel.WARNING;
            case ERROR:
            case ERROR2:
            case ERROR3:
            case ERROR4:
            case FATAL:
            case FATAL2:
            case FATAL3:
            case FATAL4:
                return ClientLogger.LogLevel.ERROR;
            default:
                return ClientLogger.LogLevel.NOTSET;
        }
    }

    static void setAttribute(LogRecordBuilder logRecord, String key,
                             Object value) {
        if (value instanceof Boolean) {
            logRecord.setAttribute(AttributeKey.booleanKey(key), (Boolean) value);
        } else if (value instanceof String) {
            logRecord.setAttribute(AttributeKey.stringKey(key), (String) value);
        } else if (value instanceof Double) {
            logRecord.setAttribute(AttributeKey.doubleKey(key), (Double) value);
        } else if (value instanceof Float) {
            logRecord.setAttribute(AttributeKey.doubleKey(key), ((Float) value).doubleValue());
        } else if (value instanceof Long) {
            logRecord.setAttribute(AttributeKey.longKey(key), (Long) value);
        } else if (value instanceof Integer) {
            logRecord.setAttribute(AttributeKey.longKey(key), Long.valueOf((Integer) value));
        } else if (value instanceof Short) {
            logRecord.setAttribute(AttributeKey.longKey(key), Long.valueOf((Short) value));
        } else if (value instanceof Byte) {
            logRecord.setAttribute(AttributeKey.longKey(key), Long.valueOf((Byte) value));
        } else {
            // TODO
        }
    }
}
