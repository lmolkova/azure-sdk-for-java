package io.clientcore.core.opentelemetry.implementation;

import io.clientcore.core.http.models.HttpHeaderName;
import io.clientcore.core.http.models.HttpHeaders;
import io.clientcore.core.http.models.HttpRequest;
import io.clientcore.core.http.models.Response;
import io.clientcore.core.http.pipeline.HttpPipelineNextPolicy;
import io.clientcore.core.opentelemetry.OpenTelemetryInstrumentationOptions;
import io.clientcore.core.util.InstrumentationContext;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;

import static io.clientcore.core.opentelemetry.implementation.OTelUtils.toOtelContext;

public class LogicalInstrumentationPolicy implements io.clientcore.core.http.pipeline.HttpPipelinePolicy {
    private static final AttributeKey<String> ERROR_TYPE = AttributeKey.stringKey("error.type");
    private static final AttributeKey<String> SERVER_ADDRESS = AttributeKey.stringKey("server.address");
    private static final AttributeKey<Long> SERVER_PORT = AttributeKey.longKey("server.port");
    private static final AttributeKey<String > OPERATION_NAME = AttributeKey.stringKey("client_core.operation.name");
    private static final AttributeKey<Long> HTTP_REQUEST_BODY_SIZE = AttributeKey.longKey("http.request.body.size");
    private static final AttributeKey<Long> HTTP_RESPONSE_BODY_SIZE = AttributeKey.longKey("http.response.body.size");
    private static final AttributeKey<Long> HTTP_RESEND_COUNT = AttributeKey.longKey("http.request.resend_count");
    private final Tracer tracer;
    private final Meter meter;
    private final DoubleHistogram durationHistogram;
    public LogicalInstrumentationPolicy(OpenTelemetryInstrumentationOptions options) {
        this.tracer = options.getOpenTelemetry().getTracer("client-core-logical");
        this.meter = options.getOpenTelemetry().getMeter("client-core-logical");
        this.durationHistogram = meter.histogramBuilder("client_core.logical.operation.duration")
            .setDescription("The duration of client-core logical operations")
            .setUnit("s")
            .build();
    }

    @SuppressWarnings("try")
    @Override
    public Response<?> process(HttpRequest httpRequest, HttpPipelineNextPolicy next) {
        io.clientcore.core.util.Context coreCtx = httpRequest.getRequestOptions().getContext();
        InstrumentationContext instContext = coreCtx.getInstrumentationContext();
        String operationName = instContext.getOperationName();
        SpanBuilder spanBuilder = tracer.spanBuilder(operationName);

        Context parentContext = toOtelContext(instContext.getTraceContext());
        if (parentContext != null) {
            spanBuilder.setParent(parentContext);
        }
        Span span = spanBuilder.startSpan();
        Instant start = Instant.now();
        AttributesBuilder attributesBuilder = Attributes.builder()
            .put(SERVER_ADDRESS, httpRequest.getUrl().getHost())
            .put(SERVER_PORT, getPort(httpRequest.getUrl()))
            .put(OPERATION_NAME, operationName);

        Context traceContext = null;
        try (Scope scope = span.makeCurrent()) {
            traceContext = Context.current();
            coreCtx.setInstrumentationContext(instContext.setTraceContext(traceContext));
            if (span.isRecording()) {
                span.setAttribute(HTTP_REQUEST_BODY_SIZE, getContentLength(httpRequest.getHeaders()));
            }
            Response<?> response = next.process();
            if (response.getStatusCode() >= 400) {
                attributesBuilder.put(ERROR_TYPE, String.valueOf(response.getStatusCode()));
                span.setStatus(StatusCode.ERROR);
            }
            if (span.isRecording()) {
                span.setAttribute(HTTP_RESPONSE_BODY_SIZE, getContentLength(response.getHeaders()));
                span.setAttribute(HTTP_RESEND_COUNT, instContext.getRetryCount());
            }
            return response;
        } catch (Exception e) {
            attributesBuilder.put(ERROR_TYPE, e.getClass().getName());
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            Attributes attributes = attributesBuilder.build();
            span.setAllAttributes(attributes);
            durationHistogram.record(getDuration(start), attributes, traceContext);
            span.end();
        }
    }

    private long getContentLength(HttpHeaders headers) {
        String contentLength = headers.getValue(HttpHeaderName.CONTENT_LENGTH);
        return contentLength == null ? 0 : Long.parseLong(contentLength);
    }

    private static double getDuration(Instant startTime) {
        return Duration.between(startTime, Instant.now()).toNanos() / 1e9;
    }

    private static int getPort(URL url) {
        int port = url.getPort();
        if (port == -1) {
            port = url.getDefaultPort();
        }
        return port;
    }
}
