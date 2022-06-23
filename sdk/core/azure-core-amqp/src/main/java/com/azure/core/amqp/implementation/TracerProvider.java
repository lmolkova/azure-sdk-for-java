// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.core.amqp.implementation;

import com.azure.core.amqp.exception.AmqpException;
import com.azure.core.util.Context;
import com.azure.core.util.logging.ClientLogger;
import com.azure.core.util.tracing.ProcessKind;
import com.azure.core.util.tracing.SpanKind;
import com.azure.core.util.tracing.StartSpanOptions;
import com.azure.core.util.tracing.Tracer;
import reactor.core.publisher.Signal;

import java.util.Objects;

public class TracerProvider {
    private static final ClientLogger LOGGER = new ClientLogger(TracerProvider.class);
    private Tracer tracer;
    private static final StartSpanOptions COMMON_CLIENT_OPTIONS = new StartSpanOptions(SpanKind.CLIENT);

    public TracerProvider(Iterable<Tracer> tracers) {
        Objects.requireNonNull(tracers, "'tracers' cannot be null.");
        if (tracers.iterator().hasNext()) {
            tracer = tracers.iterator().next();
        }
    }

    public boolean isEnabled() {
        return tracer != null;
    }

    /**
     *  For a plugged tracer implementation a new tracing span is created.
     *
     * The {@code context} will be checked for containing information about a parent span. If a parent span is found the
     * new span will be added as a child, otherwise the span will be created and added to the context and any downstream
     * start calls will use the created span as the parent.
     *
     * @param serviceBaseName the service name to be appended to the span name.
     * @param context Additional metadata that is passed through the call stack.
     * @param processKind the invoking process type.
     * @return An updated context object.
     */
    public Context startSpan(String serviceBaseName, Context context, ProcessKind processKind) {
        Objects.requireNonNull(context, "'context' cannot be null.");
        Objects.requireNonNull(processKind, "'processKind' cannot be null.");

        if (tracer == null) {
            return context;
        }
        String spanName = getSpanName(serviceBaseName, processKind);

        return tracer.start(spanName, context, processKind);
    }

    /**
     * Given a context containing the current tracing span the span is marked completed with status info from
     * {@link Signal}.  For each tracer plugged into the SDK the current tracing span is marked as completed.
     *
     * @param context Additional metadata that is passed through the call stack.
     * @param signal The signal indicates the status and contains the metadata we need to end the tracing span.
     */
    public void endSpan(Context context, Signal<Void> signal) {
        Objects.requireNonNull(context, "'context' cannot be null.");
        Objects.requireNonNull(signal, "'signal' cannot be null.");

        if (tracer == null) {
            return;
        }

        switch (signal.getType()) {
            case ON_COMPLETE:
                end("success", null, context);
                break;
            case ON_ERROR:
                String errorCondition = "";
                Throwable throwable = null;
                if (signal.hasError()) {
                    // The last status available is on error, this contains the thrown error.
                    throwable = signal.getThrowable();

                    if (throwable instanceof AmqpException) {
                        AmqpException exception = (AmqpException) throwable;
                        errorCondition = exception.getErrorCondition().getErrorCondition();
                    }
                }
                end(errorCondition, throwable, context);
                break;
            default:
                // ON_SUBSCRIBE and ON_NEXT don't have the information to end the span so just return.
                break;
        }
    }

    /**
     *  For a plugged tracer implementation a link is created between the parent tracing span and
     * the current service call.
     *
     * @param context Additional metadata that is passed through the call stack.
     */
    public void addSpanLinks(Context context) {
        Objects.requireNonNull(context, "'context' cannot be null.");
        if (tracer == null) {
            return;
        }
        tracer.addLink(context);
    }

    /**
     *  For a plugged tracer implementation a new context is extracted from the event's diagnostic Id.
     *
     * @param diagnosticId Unique identifier of an external call from producer to the queue.
     */
    public Context extractContext(String diagnosticId, Context context) {
        Objects.requireNonNull(context, "'context' cannot be null.");
        Objects.requireNonNull(diagnosticId, "'diagnosticId' cannot be null.");
        if (tracer == null) {
            return context;
        }
        return tracer.extractContext(diagnosticId, context);
    }

    /**
     * For a plugged tracer implementation a new context containing the span builder is returned.
     *
     * @param serviceBaseName the service name to be appended to the span name.
     * @param context Additional metadata containing the span name for creating the span builder.
     *
     * @deprecated use {@link TracerProvider#createSpanBuilder(String, StartSpanOptions, Context)} (String, StartSpanOptions, Context)}.
     */
    @Deprecated
    public Context getSharedSpanBuilder(String serviceBaseName, Context context) {
        Objects.requireNonNull(context, "'context' cannot be null.");
        if (tracer == null) {
            return context;
        }

        String spanName = getSpanName(serviceBaseName, ProcessKind.SEND);
        return tracer.createSpanBuilder(spanName, COMMON_CLIENT_OPTIONS, context);
    }

    /**
     * For a plugged tracer implementation a new context containing the span builder is returned.
     *
     * @param spanName Span name following Class.method pattern.
     * @param options span options.
     * @param context Additional context.
     */
    public Context createSpanBuilder(String spanName, StartSpanOptions options, Context context) {
        Objects.requireNonNull(context, "'context' cannot be null.");
        if (tracer == null) {
            return context;
        }

        return tracer.createSpanBuilder(spanName, options, context);
    }

    private void end(String statusMessage, Throwable throwable, Context context) {
        tracer.end(statusMessage, throwable, context);
    }

    private String getSpanName(String serviceBaseName, ProcessKind processKind) {
        switch (processKind) {
            case SEND:
                serviceBaseName += "send";
                break;
            case MESSAGE:
                serviceBaseName += "message";
                break;
            case PROCESS:
                serviceBaseName += "process";
                break;
            default:
                LOGGER.warning("Unknown processKind type: {}", processKind);
                break;
        }
        return serviceBaseName;
    }
}
