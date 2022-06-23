// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.tracing.opentelemetry;

import com.azure.core.tracing.opentelemetry.implementation.AmqpPropagationFormatUtil;
import com.azure.core.tracing.opentelemetry.implementation.AmqpTraceUtil;
import com.azure.core.tracing.opentelemetry.implementation.HttpTraceUtil;
import com.azure.core.util.Context;
import com.azure.core.util.CoreUtils;
import com.azure.core.util.logging.ClientLogger;
import com.azure.core.util.tracing.ProcessKind;
import com.azure.core.util.tracing.StartSpanOptions;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Basic tracing implementation class for use with REST and AMQP Service Clients to create {@link Span} and in-process
 * context propagation. Singleton OpenTelemetry tracer capable of starting and exporting spans.
 * <p>
 * This helper class supports W3C distributed tracing protocol and injects SpanContext into the outgoing HTTP and AMQP
 * requests.
 */
public class OpenTelemetryTracer implements com.azure.core.util.tracing.Tracer {
    private static final StartSpanOptions DEFAULT_OPTIONS = new StartSpanOptions(com.azure.core.util.tracing.SpanKind.INTERNAL);
    private final Tracer tracer;

    /**
     * Creates new {@link OpenTelemetryTracer} using default global tracer -
     * {@link GlobalOpenTelemetry#getTracer(String)}
     *
     */
    public OpenTelemetryTracer() {
        this(GlobalOpenTelemetry.getTracer("Azure-OpenTelemetry"));
    }

    /**
     * Creates new {@link OpenTelemetryTracer} that wraps {@link io.opentelemetry.api.trace.Tracer}.
     * Use it for tests.
     *
     * @param tracer {@link io.opentelemetry.api.trace.Tracer} instance.
     */
    OpenTelemetryTracer(Tracer tracer) {
        this.tracer = tracer;
    }

    static final String AZ_NAMESPACE_KEY = "az.namespace";

    // standard attributes with AMQP request
    static final String MESSAGE_BUS_DESTINATION = "message_bus.destination";
    static final String PEER_ENDPOINT = "peer.address";

    private static final ClientLogger LOGGER = new ClientLogger(OpenTelemetryTracer.class);
    private static final AutoCloseable NOOP_CLOSEABLE = () -> { };
    private static final SpanKind SHARED_SPAN_BUILDER_KIND = SpanKind.CLIENT;
    private static final String SUPPRESSED_SPAN_FLAG = "suppressed-span-flag";
    private static final String CLIENT_METHOD_CALL_FLAG = "client-method-call-flag";

    /**
     * {@inheritDoc}
     */
    @Override
    public Context start(String spanName, Context context) {
        return start(spanName, DEFAULT_OPTIONS, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Context start(String spanName, StartSpanOptions options, Context context) {
        Objects.requireNonNull(spanName, "'spanName' cannot be null.");
        Objects.requireNonNull(options, "'options' cannot be null.");

        return buildAndStart(spanName, convertToOtelKind(options.getSpanKind()), options.getAttributes(), context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Context start(String spanName, Context context, ProcessKind processKind) {
        Objects.requireNonNull(spanName, "'spanName' cannot be null.");
        Objects.requireNonNull(context, "'context' cannot be null.");
        Objects.requireNonNull(processKind, "'processKind' cannot be null.");

        switch (processKind) {
            case SEND:
                return buildAndStart(spanName, SpanKind.CLIENT, null, context);
            case MESSAGE:
                context = buildAndStart(spanName, SpanKind.PRODUCER, null, context);
                return setDiagnosticId(context);
            case PROCESS:
                context = buildAndStart(spanName, SpanKind.CONSUMER, null, context);
                // TODO (limolkova) we should do this in the EventHub/ServiceBus SDK instead to make sure scope is
                //  closed in the same thread where it was started to prevent leaking the context.
                return context.addData(SCOPE_KEY, makeSpanCurrent(context));
            default:
                LOGGER.atWarning()
                    .addKeyValue("spanName", spanName)
                    .addKeyValue("processKind", processKind)
                    .log("Start span is called with unknown process kind, suppressing the span.");
                return startSuppressedSpan(context);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void end(int responseCode, Throwable throwable, Context context) {
        Objects.requireNonNull(context, "'context' cannot be null.");

        final Span span = getSpanOrNull(context);
        if (span == null) {
            return;
        }

        if (span.isRecording()) {
            HttpTraceUtil.setSpanStatus(span, responseCode, throwable);
        }
        span.end();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttribute(String key, String value, Context context) {
        Objects.requireNonNull(context, "'context' cannot be null");

        if (CoreUtils.isNullOrEmpty(value)) {
            LOGGER.verbose("Failed to set span attribute since value is null or empty.");
            return;
        }

        final Span span = getSpanOrNull(context);
        if (span == null) {
            return;
        }

        if (span.isRecording()) {
            span.setAttribute(key, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Context setSpanName(String spanName, Context context) {
        return context.addData(USER_SPAN_NAME_KEY, spanName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void end(String statusMessage, Throwable throwable, Context context) {
        Span span = getSpanOrNull(context);
        if (span == null) {
            return;
        }

        if (span.isRecording()) {
            span = AmqpTraceUtil.parseStatusMessage(span, statusMessage, throwable);
        }

        span.end();

        // TODO (limolkova) remove once ServiceBus/EventHub start making span current explicitly.
        endScope(context);
    }

    @Override
    public void addLink(Context context) {
        final SpanBuilder spanBuilder = getOrNull(context, SPAN_BUILDER_KEY, SpanBuilder.class);
        if (spanBuilder == null) {
            return;
        }

        final SpanContext spanContext = getOrNull(context, SPAN_CONTEXT_KEY, SpanContext.class);
        if (spanContext == null) {
            return;
        }
        spanBuilder.addLink(spanContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Context extractContext(String diagnosticId, Context context) {
        return AmqpPropagationFormatUtil.extractContext(diagnosticId, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("deprecation")
    public Context getSharedSpanBuilder(String spanName, Context context) {
        // this is used to create messaging send spanBuilder, and it's a CLIENT span
        SpanBuilder spanBuilder = getOrCreateSpanBuilder(spanName, convertToOtelKind(com.azure.core.util.tracing.SpanKind.CLIENT), getTraceContextOrNull(context), context);
        return context.addData(SPAN_BUILDER_KEY, spanBuilder);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Context createSpanBuilder(String spanName, StartSpanOptions startOptions, Context context) {
        SpanBuilder spanBuilder = getOrCreateSpanBuilder(spanName, convertToOtelKind(startOptions.getSpanKind()), getTraceContextOrNull(context), context);
        setAttributes(spanBuilder, startOptions.getAttributes());
        return context.addData(SPAN_BUILDER_KEY, spanBuilder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AutoCloseable makeSpanCurrent(Context context) {
        if (getBoolean(SUPPRESSED_SPAN_FLAG, context)) {
            return NOOP_CLOSEABLE;
        }

        io.opentelemetry.context.Context traceContext = getTraceContextOrNull(context);
        if (traceContext == null) {
            LOGGER.verbose("There is no OpenTelemetry Context on the context, cannot make it current");
            return NOOP_CLOSEABLE;
        }
        return traceContext.makeCurrent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("deprecation")
    public void addEvent(String eventName, Map<String, Object> traceEventAttributes, OffsetDateTime timestamp) {
        addEvent(eventName, traceEventAttributes, timestamp, new Context(PARENT_TRACE_CONTEXT_KEY, io.opentelemetry.context.Context.current()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addEvent(String eventName, Map<String, Object> traceEventAttributes, OffsetDateTime timestamp, Context context) {
        Objects.requireNonNull(eventName, "'eventName' cannot be null.");
        Span currentSpan = getSpanOrNull(context);

        if (currentSpan == null) {
            LOGGER.verbose("There is no OpenTelemetry Span or Context on the context, cannot add event");
            return;
        }

        if (timestamp == null) {
            currentSpan.addEvent(
                eventName,
                traceEventAttributes == null ? Attributes.empty() : convertToOtelAttributes(traceEventAttributes));
        } else {
            currentSpan.addEvent(
                eventName,
                traceEventAttributes == null ? Attributes.empty() : convertToOtelAttributes(traceEventAttributes),
                timestamp.toInstant()
            );
        }
    }

    private Context buildAndStart(String spanName, SpanKind spanKind, Map<String, Object> beforeSamplingAttributes, Context context) {
        if (shouldSuppress(spanKind, context)) {
            return startSuppressedSpan(context);
        }

        context = unsuppress(context);

        if (isClientCall(spanKind) && !context.getData(CLIENT_METHOD_CALL_FLAG).isPresent()) {
            context = context.addData(CLIENT_METHOD_CALL_FLAG, true);
        }

        io.opentelemetry.context.Context parentContext = getTraceContextOrNull(context);
        if (parentContext == null) {
            parentContext = io.opentelemetry.context.Context.current();
        }

        SpanBuilder spanBuilder = getOrCreateSpanBuilder(spanName, spanKind, parentContext, context);
        setAttributes(spanBuilder, beforeSamplingAttributes);

        Span span = spanBuilder.startSpan();

        return context
            .addData(PARENT_TRACE_CONTEXT_KEY, parentContext.with(span))
            .addData(SPAN_BUILDER_KEY, null);
    }

    /**
     * Extracts request attributes from the given {@link Context} and adds it to the span builder
     *
     * @param spanBuilder The span to which request attributes are to be added.
     * @param context The context containing the request attributes.
     */
    private void setAttributesFromContext(SpanBuilder spanBuilder, Context context) {
        String tracingNamespace = getOrNull(context, AZ_TRACING_NAMESPACE_KEY, String.class);
        if (tracingNamespace != null) {
            spanBuilder.setAttribute(AZ_NAMESPACE_KEY, tracingNamespace);
        }
        String entityPath = getOrNull(context, ENTITY_PATH_KEY, String.class);
        if (entityPath != null) {
            spanBuilder.setAttribute(MESSAGE_BUS_DESTINATION, entityPath);
        }
        String hostName = getOrNull(context, HOST_NAME_KEY, String.class);
        if (hostName != null) {
            spanBuilder.setAttribute(PEER_ENDPOINT, hostName);
        }

        Long messageEnqueuedTime = getOrNull(context, MESSAGE_ENQUEUED_TIME, Long.class);
        if (messageEnqueuedTime != null) {
            spanBuilder.setAttribute(MESSAGE_ENQUEUED_TIME, messageEnqueuedTime);
        }

        return;
    }

    /**
     * Returns a {@link SpanBuilder} to create and start a new child {@link Span} with parent being the designated
     * {@link Span}.
     *
     * @param spanName The name of the returned Span.
     * @param spanKind Kind of the span to create.
     * @param context The context containing the span and the span name.
     * @return A {@link SpanBuilder} to create and start a new {@link Span}.
     */
    @SuppressWarnings("unchecked")
    private SpanBuilder getOrCreateSpanBuilder(String spanName, SpanKind spanKind, io.opentelemetry.context.Context parentContext, Context context) {
        SpanBuilder spanBuilder = getOrNull(context, SPAN_BUILDER_KEY, SpanBuilder.class);
        if (spanBuilder != null) {
            return spanBuilder;
        }

        String spanNameKey = getOrNull(context, USER_SPAN_NAME_KEY, String.class);

        if (spanNameKey == null) {
            spanNameKey = spanName;
        }

        spanBuilder = tracer.spanBuilder(spanNameKey)
            .setSpanKind(spanKind);

        if (parentContext != null) {
            spanBuilder.setParent(parentContext);
        }

        setAttributesFromContext(spanBuilder, context);
        return spanBuilder;
    }

    /**
     * Ends current scope on the context.
     *
     * @param context Context instance with the scope to end.
     */
    private void endScope(Context context) {
        Scope scope = getOrNull(context, SCOPE_KEY, Scope.class);
        if (scope != null) {
            scope.close();
        }
    }

    /*
     * Converts our SpanKind to OpenTelemetry SpanKind.
     */
    private SpanKind convertToOtelKind(com.azure.core.util.tracing.SpanKind kind) {
        switch (kind) {
            case CLIENT:
                return SpanKind.CLIENT;

            case SERVER:
                return SpanKind.SERVER;

            case CONSUMER:
                return SpanKind.CONSUMER;

            case PRODUCER:
                return SpanKind.PRODUCER;

            default:
                return SpanKind.INTERNAL;
        }
    }

    /**
     * Maps span/event properties to OpenTelemetry attributes.
     *
     * @param attributes the attributes provided by the client SDK's.
     * @return the OpenTelemetry typed {@link Attributes}.
     */
    private SpanBuilder setAttributes(SpanBuilder builder, Map<String, Object> attributes) {
        if (CoreUtils.isNullOrEmpty(attributes)) {
            return builder;
        }

        attributes.forEach((key, value) -> {
            if (value instanceof Boolean) {
                builder.setAttribute(key, (boolean) value);
            } else if (value instanceof String) {
                builder.setAttribute(key, String.valueOf(value));
            } else if (value instanceof Double) {
                builder.setAttribute(key, (Double) value);
            } else if (value instanceof Long) {
                builder.setAttribute(key, (Long) value);
            } else if (value instanceof String[]) {
                builder.setAttribute(AttributeKey.stringArrayKey(key), Arrays.asList((String[]) value));
            } else if (value instanceof long[]) {
                long[] val = (long[]) value;
                List<Long> converted = new ArrayList<>();
                for (int i = 0; i < val.length; i++) {
                    converted.add(val[i]);
                }

                builder.setAttribute(AttributeKey.longArrayKey(key), converted);
            } else if (value instanceof double[]) {
                double[] val = (double[]) value;
                List<Double> converted = new ArrayList<>();
                for (int i = 0; i < val.length; i++) {
                    converted.add(val[i]);
                }

                builder.setAttribute(AttributeKey.doubleArrayKey(key), converted);
            } else if (value instanceof boolean[]) {
                boolean[] val = (boolean[]) value;
                List<Boolean> converted = new ArrayList<>();
                for (int i = 0; i < val.length; i++) {
                    converted.add(val[i]);
                }

                builder.setAttribute(AttributeKey.booleanArrayKey(key), converted);
            } else {
                LOGGER.warning("Could not populate attribute with key '{}', type is not supported.");
            }
        });
        return builder;
    }

    /**
     * Maps span/event properties to OpenTelemetry attributes.
     *
     * @param attributes the attributes provided by the client SDK's.
     * @return the OpenTelemetry typed {@link Attributes}.
     */
    private Attributes convertToOtelAttributes(Map<String, Object> attributes) {
        AttributesBuilder attributesBuilder = Attributes.builder();
        attributes.forEach((key, value) -> {
            if (value instanceof Boolean) {
                attributesBuilder.put(key, (boolean) value);
            } else if (value instanceof String) {
                attributesBuilder.put(key, String.valueOf(value));
            } else if (value instanceof Double) {
                attributesBuilder.put(key, (Double) value);
            } else if (value instanceof Long) {
                attributesBuilder.put(key, (Long) value);
            } else if (value instanceof String[]) {
                attributesBuilder.put(key, (String[]) value);
            } else if (value instanceof long[]) {
                attributesBuilder.put(key, (long[]) value);
            } else if (value instanceof double[]) {
                attributesBuilder.put(key, (double[]) value);
            } else if (value instanceof boolean[]) {
                attributesBuilder.put(key, (boolean[]) value);
            } else {
                LOGGER.warning("Could not populate attribute with key '{}', type is not supported.");
            }
        });
        return attributesBuilder.build();
    }

    /**
     * Extracts the {@link SpanContext trace identifiers} and the {@link SpanContext} of the current tracing span as
     * text and returns in a {@link Context} object.
     *
     * @param context The context with current tracing span describing unique message context.
     * @return The {@link Context} containing the {@link SpanContext} and trace-parent of the current span.
     */
    private Context setDiagnosticId(Context context) {
        Span span = getSpanOrNull(context);
        if (span == null) {
            return context;
        }

        SpanContext spanContext = span.getSpanContext();
        if (spanContext.isValid()) {
            final String traceparent = AmqpPropagationFormatUtil.getDiagnosticId(spanContext);
            if (traceparent == null) {
                return context;
            }
            return context.addData(DIAGNOSTIC_ID_KEY, traceparent).addData(SPAN_CONTEXT_KEY, spanContext);
        }

        return context;
    }

    /**
     * Returns the value of the specified key from the context.
     *
     * @param key The name of the attribute that needs to be extracted from the {@link Context}.
     * @param clazz clazz the type of raw class to find data for.
     * @param context The context containing the specified key.
     * @return The T type of raw class object
     */
    @SuppressWarnings("unchecked")
    private static <T> T getOrNull(Context context, String key, Class<T> clazz) {
        final Optional<Object> optional = context.getData(key);
        final Object result = optional.filter(value -> clazz.isAssignableFrom(value.getClass())).orElseGet(() -> {
            LOGGER.verbose("Could not extract key '{}' of type '{}' from context.", key, clazz);
            return null;
        });

        return (T) result;
    }

    /**
     * Returns OpenTelemetry trace context from given com.azure.core.Context under PARENT_TRACE_CONTEXT_KEY
     * or PARENT_SPAN_KEY (for backward-compatibility) or default value.
     */
    @SuppressWarnings("deprecation")
    private static io.opentelemetry.context.Context getTraceContextOrNull(Context azContext) {
        io.opentelemetry.context.Context traceContext = getOrNull(azContext,
            PARENT_TRACE_CONTEXT_KEY,
            io.opentelemetry.context.Context.class);

        if (traceContext == null) {
            Span parentSpan = getOrNull(azContext,
                PARENT_SPAN_KEY,
                Span.class);

            if (parentSpan != null) {
                traceContext = io.opentelemetry.context.Context.current().with(parentSpan);
            }
        }
        return traceContext;
    }

    /**
     * Returns OpenTelemetry trace context from given com.azure.core.Context under PARENT_TRACE_CONTEXT_KEY
     * or PARENT_SPAN_KEY (for backward-compatibility)
     */
    @SuppressWarnings("deprecation")
    private Span getSpanOrNull(Context azContext) {
        if (getBoolean(SUPPRESSED_SPAN_FLAG, azContext)) {
            return null;
        }

        io.opentelemetry.context.Context traceContext = getOrNull(azContext,
            PARENT_TRACE_CONTEXT_KEY,
            io.opentelemetry.context.Context.class);

        if (traceContext == null) {
            Span parentSpan = getOrNull(azContext,
                PARENT_SPAN_KEY,
                Span.class);

            if (parentSpan != null) {
                return parentSpan;
            }
        }

        return traceContext == null ? null : Span.fromContext(traceContext);
    }

    private Context startSuppressedSpan(Context context) {
        return context.addData(SUPPRESSED_SPAN_FLAG, true);
    }

    private static boolean shouldSuppress(SpanKind kind, Context context) {
        return isClientCall(kind) && getBoolean(CLIENT_METHOD_CALL_FLAG, context);
    }

    private static Context unsuppress(Context context) {
        if (getBoolean(SUPPRESSED_SPAN_FLAG, context)) {
            return context.addData(SUPPRESSED_SPAN_FLAG, false);
        }

        return context;
    }

    private static boolean getBoolean(String key, Context context) {
        Optional<Object> flag = context.getData(key);
        return flag.isPresent() && Boolean.TRUE.equals(flag.get());
    }

    private static boolean isClientCall(SpanKind kind) {
        return kind == SpanKind.CLIENT || kind == SpanKind.INTERNAL;
    }
}
