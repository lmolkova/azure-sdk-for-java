package com.azure.spring.cloud.trace.sleuth;

import com.azure.core.util.Context;
import com.azure.core.util.TracingOptions;
import com.azure.core.util.tracing.StartSpanOptions;
import com.azure.core.util.tracing.Tracer;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceContext;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

class SleuthTracer implements Tracer {
    private static final StartSpanOptions DEFAULT_SPAN_START_OPTIONS = new StartSpanOptions(com.azure.core.util.tracing.SpanKind.INTERNAL);
    private static final AutoCloseable NOOP_CLOSEABLE = () -> { };
    private static final String AZ_TRACING_NAMESPACE_KEY = "az.namespace";
    private final String azNamespace;
    private final org.springframework.cloud.sleuth.Tracer tracer;
    SleuthTracer(String azNamespace, TracingOptions options) {
        if (options != null && options.isEnabled() && options instanceof SleuthTracingOptions) {
            SleuthTracingOptions sleuthOptions = (SleuthTracingOptions) options;
            tracer = sleuthOptions.getTracer();
        } else {
            // is there a way to create a tracer?
            // since micrometer tracing is a facade over otel or brave, it makes no sense to use this impl
            // with OTel bridge. should it be specific to brave-only? Does it make sense to do anything for brave
            // considering it's state?
            tracer = null;
        }

        this.azNamespace = azNamespace;
    }

    @Override
    public Context start(String methodName, Context context) {
        return start(methodName, DEFAULT_SPAN_START_OPTIONS, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Context start(String spanName, StartSpanOptions options, Context context) {
        Objects.requireNonNull(spanName, "'spanName' cannot be null.");
        Objects.requireNonNull(options, "'options' cannot be null.");

        if (tracer == null) {
            return context;
        }

        Span.Builder spanBuilder = tracer.spanBuilder()
            .name(spanName)
            .kind(convertToSleuthKind(options.getSpanKind()));

        TraceContext parentContext = getTraceContextOrDefault(options.getRemoteParent(), null);
        if (parentContext != null) {
            spanBuilder.setParent(parentContext);
        } else {
            Span parent = getSpanOrDefault(context, null);
                if (parent != null) {
                    spanBuilder.setParent(parent.context());
                }
        }

        if (options.getAttributes() != null) {
            for (Map.Entry<String, Object> kvp : options.getAttributes().entrySet()) {
                spanBuilder.tag(kvp.getKey(), kvp.getValue().toString());
            }
        }

        if (azNamespace != null) {
            spanBuilder.tag(AZ_TRACING_NAMESPACE_KEY, azNamespace);
        }

        Span span = spanBuilder.start();
        return context.addData(PARENT_TRACE_CONTEXT_KEY, span);
    }

    @Override
    public AutoCloseable makeSpanCurrent(Context context) {
        if (tracer != null) {
            Span span = getSpanOrDefault(context, null);
            if (span != null) {
                return tracer.withSpan(span);
            }
        }

        return NOOP_CLOSEABLE;
    }

    @Override
    public void setAttribute(String key, long value, Context context) {
        Objects.requireNonNull(context, "'context' cannot be null");
        if (tracer != null) {
            Span span = getSpanOrDefault(context, null);
            if (span != null && !span.isNoop()) {
                span.tag(key, Long.toString(value));
            }
        }
    }

    @Override
    public void setAttribute(String key, String value, Context context) {
        Objects.requireNonNull(context, "'context' cannot be null");
        if (tracer != null) {
            Span span = getSpanOrDefault(context, null);
            if (span != null && !span.isNoop()) {
                span.tag(key, value);
            }
        }
    }

    @Override
    public void end(String errorMessage, Throwable throwable, Context context) {
        if (tracer == null) {
            return;
        }

        final Span span = getSpanOrDefault(context, null);
        if (span == null || span.isNoop()) {
            return;
        }

        if (throwable != null) {
            span.error(throwable).end();
            return;
        }

        span.end();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addEvent(String eventName, Map<String, Object> traceEventAttributes, OffsetDateTime timestamp, Context context) {
        Objects.requireNonNull(eventName, "'eventName' cannot be null.");
        if (tracer == null) {
            return;
        }

        Span currentSpan = getSpanOrDefault(context, null);
        if (currentSpan == null) {
            return;
        }

        currentSpan.event(eventName);
    }

    @Override
    public boolean isEnabled() {
        return tracer != null;
    }

    @Override
    public void injectContext(BiConsumer<String, String> headerSetter, Context context) {
        if (tracer == null) {
            return;
        }

        final Span span = getSpanOrDefault(context, null);
        if (span == null || span.isNoop()) {
            return;
        }

        // TODO reuse W3CPropagation from brave bridge if this should be specific to brave?
        String traceparent = "00-" + span.context().traceId() + "_" + span.context().spanId() + "-" + "00";
        headerSetter.accept("traceparent", traceparent);
    }

    @Override
    public Context extractContext(Function<String, String> headerGetter) {
        if (tracer == null) {
            return Context.NONE;
        }

        // TODO reuse W3CPropagation from brave bridge if this should be specific to brave?
        String traceparent = headerGetter.apply("traceparent");
        if (traceparent == null || traceparent.length() < 55) {
            return Context.NONE;
        }

        TraceContext traceContext = tracer.traceContextBuilder()
            .traceId(traceparent.substring(3, 35))
            .spanId(traceparent.substring(36, 52))
            .sampled(traceparent.endsWith("01"))
            .build();

        return new Context(SPAN_CONTEXT_KEY, traceContext);
    }
    private static Span getSpanOrDefault(Context azContext, Span defaultSpan) {
        Span span = getOrNull(azContext, PARENT_TRACE_CONTEXT_KEY, Span.class);

        return span == null ? defaultSpan : span;
    }

    private static TraceContext getTraceContextOrDefault(Context azContext, TraceContext defaultContext) {
        TraceContext context = getOrNull(azContext, SPAN_CONTEXT_KEY, TraceContext.class);

        return context == null ? defaultContext : context;
    }

    private static <T> T getOrNull(Context context, String key, Class<T> clazz) {
        final Object data = context.getData(key).orElse(null);
        if (data != null && clazz.isAssignableFrom(data.getClass())) {
            return  (T) data;
        }

        return null;
    }

    private Span.Kind convertToSleuthKind(com.azure.core.util.tracing.SpanKind kind) {
        switch (kind) {
            case SERVER:
                return Span.Kind.SERVER;

            case CONSUMER:
                return Span.Kind.CONSUMER;

            case PRODUCER:
                return Span.Kind.PRODUCER;

            case CLIENT:
            default:
                return Span.Kind.CLIENT;
        }
    }
}
