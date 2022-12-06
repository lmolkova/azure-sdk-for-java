// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.messaging.servicebus.implementation.instrumentation;

import com.azure.core.amqp.exception.AmqpException;
import com.azure.core.util.Context;
import com.azure.core.util.logging.ClientLogger;
import com.azure.core.util.tracing.SpanKind;
import com.azure.core.util.tracing.StartSpanOptions;
import com.azure.core.util.tracing.Tracer;
import com.azure.core.util.tracing.TracingLink;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.azure.core.util.tracing.Tracer.ENTITY_PATH_KEY;
import static com.azure.core.util.tracing.Tracer.HOST_NAME_KEY;
import static com.azure.core.util.tracing.Tracer.SPAN_CONTEXT_KEY;

/**
 * Tracing helper.
 */
public class ServiceBusTracer {
    public static final String START_TIME_KEY = "span-start-time";
    public static final String REACTOR_PARENT_TRACE_CONTEXT_KEY = "otel-context-key";
    public static final String REACTOR_SPAN_START_OPTIONS_KEY = "otel-span-options-key";
    private static final AutoCloseable NOOP_CLOSEABLE = () -> {
    };
    private static final ClientLogger LOGGER = new ClientLogger(ServiceBusTracer.class);
    protected static final String TRACEPARENT_KEY = "traceparent";
    private static final String MESSAGE_ENQUEUED_TIME = "x-opt-enqueued-time";
    private static final String DIAGNOSTIC_ID_KEY = "Diagnostic-Id";


    protected final Tracer tracer;
    protected final String fullyQualifiedName;
    protected final String entityPath;
    private final StartSpanOptions clientStartOptions;

    public ServiceBusTracer(Tracer tracer, String fullyQualifiedName, String entityPath) {
        this.tracer = tracer;
        this.fullyQualifiedName = Objects.requireNonNull(fullyQualifiedName, "'fullyQualifiedName' cannot be null");
        this.entityPath = Objects.requireNonNull(entityPath, "'entityPath' cannot be null");
        this.clientStartOptions = createOptionsWithCommonAttributes(SpanKind.CLIENT);
    }

    /**
     * Checks if tracing is enabled.
     */
    public boolean isEnabled() {
        return tracer != null && tracer.isEnabled();
    }

    /**
     * Makes span in provided context (if any) current. Caller is responsible to close the returned scope.
     */
    public AutoCloseable makeSpanCurrent(Context span) {
        return tracer == null ? NOOP_CLOSEABLE : tracer.makeSpanCurrent(span);
    }

    /**
     * Traces arbitrary mono. No special send or receive semantics is applied.
     */
    public <T> Mono<T> traceMono(String spanName, Mono<T> publisher) {
        if (tracer != null) {
            return publisher
                .doOnEach(this::endSpan)
                .contextWrite(ctx -> ctx.put(REACTOR_PARENT_TRACE_CONTEXT_KEY, tracer.start(spanName, clientStartOptions, Context.NONE)));
        }

        return publisher;
    }

    /**
     * Traces arbitrary mono that operates with received message as input, e.g. renewLock. No special send or receive semantics is applied.
     */
    public <T> Mono<T> traceMonoWithLink(String spanName, Mono<T> publisher, ServiceBusReceivedMessage message, Context messageContext) {
        if (tracer != null) {
            return publisher
                .doOnEach(this::endSpan)
                .contextWrite(ctx -> ctx.put(REACTOR_PARENT_TRACE_CONTEXT_KEY, startSpanWithLink(spanName, message, messageContext, Context.NONE)));
        }

        return publisher;
    }

    /**
     * Traces arbitrary mono that operates with sent message as input, e.g. schedule. No special send or receive semantics is applied.
     */
    public <T> Mono<T> traceMonoWithLink(String spanName, Mono<T> publisher, ServiceBusMessage message, Context messageContext) {
        if (tracer != null) {
            return publisher
                .doOnEach(this::endSpan)
                .contextWrite(reactor.util.context.Context.of(REACTOR_PARENT_TRACE_CONTEXT_KEY,
                    startSpanWithLink(spanName, message, messageContext, Context.NONE)));
        }

        return publisher;
    }

    /**
     * Traces arbitrary mono that operates with batch of sent message as input, e.g. schedule. No special send or receive semantics is applied.
     */
    public <T> Flux<T> traceFluxWithLinks(String spanName, Flux<T> publisher, List<ServiceBusMessage> batch, Function<ServiceBusMessage, Context> getContext) {
        if (tracer != null) {
            return publisher
                .doOnEach(this::endSpan)
                .contextWrite(reactor.util.context.Context.of(REACTOR_PARENT_TRACE_CONTEXT_KEY, startSpanWithLinks(spanName, batch, getContext, Context.NONE)));
        }
        return publisher;
    }

    /**
     * Ends span and scope.
     */
    public void endSpan(Throwable throwable, Context span, AutoCloseable scope) {
        if (tracer != null) {
            String errorCondition = "success";
            if (throwable instanceof AmqpException) {
                AmqpException exception = (AmqpException) throwable;
                errorCondition = exception.getErrorCondition().getErrorCondition();
            }

            try {
                if (scope != null) {
                    scope.close();
                }
            } catch (Exception e) {
                LOGGER.warning("Can't close scope", e);
            } finally {
                tracer.end(errorCondition, throwable, span);
            }
        }
    }

    /**
     * Used in ServiceBusMessageBatch.tryAddMessage() to start tracing for to-be-sent out messages.
     */
    public void reportMessageSpan(ServiceBusMessage serviceBusMessage, Context messageContext) {
        if (tracer == null || messageContext == null || messageContext.getData(SPAN_CONTEXT_KEY).isPresent()) {
            // if message has context (in case of retries), don't start a message span or add a new context
            return;
        }

        if (serviceBusMessage.getApplicationProperties().get("traceparent") != null) {
            // if message has context (in case of retries) or if user supplied it, don't start a message span or add a new context
            return;
        }

        StartSpanOptions options = createOptionsWithCommonAttributes(SpanKind.PRODUCER);
        Context messageSpan = tracer.start("ServiceBus.message", options, messageContext);
        tracer.injectContext((name, value) -> {
            serviceBusMessage.getApplicationProperties().put(name, value);
            if (TRACEPARENT_KEY.equals(name)) {
                serviceBusMessage.getApplicationProperties().put(DIAGNOSTIC_ID_KEY, value);
            }
        }, messageSpan);

        tracer.end(null, null, messageSpan);
    }

    /**
     * Instruments peek or receiveDeferred that return a single message. Creates a single span, does not report any metrics
     */
    public Mono<ServiceBusReceivedMessage> traceManagementReceive(String spanName, Mono<ServiceBusReceivedMessage> publisher,
        Function<ServiceBusReceivedMessage, Context> getMessageContext) {
        if (tracer != null) {
            AtomicLong startTime = new AtomicLong();
            AtomicReference<ServiceBusReceivedMessage> message = new AtomicReference<>();
            return publisher.doOnEach(signal -> {
                if (signal.hasValue()) {
                    message.set(signal.get());
                }

                if (signal.isOnComplete() || signal.isOnError()) {
                    ServiceBusReceivedMessage msg = message.get();
                    Context messageContext = msg == null ? null : getMessageContext.apply(msg);

                    Context span = startSpanWithLink(spanName, msg, messageContext, new Context(START_TIME_KEY, startTime.get()));
                    endSpan(null, span, null);
                }
            })
            .doOnSubscribe(s -> {
                startTime.set(Instant.now().toEpochMilli());
            });
        }
        return publisher;
    }


    /**
     * Traces receive, peek, receiveDeferred that return a flux of messages, but has a limited lifetime - such as sync receive case or peek many
     * or receive deferred.
     *
     * Don't use it for async receive when Flux has unknown lifetime!!!
     *
     * Creates a single span with links to each message being received.
     */
    public Flux<ServiceBusReceivedMessage> traceSyncReceive(String spanName, Flux<ServiceBusReceivedMessage> messages) {
        if (tracer != null) {
            return messages
                .doOnEach(signal -> {
                    StartSpanOptions startOptions = signal.getContextView().getOrDefault(REACTOR_SPAN_START_OPTIONS_KEY, null);
                    if (signal.hasValue()) {
                        ServiceBusReceivedMessage message = signal.get();
                        if (message != null) {
                            addLink(message.getApplicationProperties(), message.getEnqueuedTime(), startOptions, Context.NONE);
                        }
                    } else if (signal.isOnComplete() || signal.isOnError()) {
                        Context span = tracer.start(spanName, startOptions, Context.NONE);
                        endSpan(signal.getThrowable(), span, null);
                    }
                })
                .contextWrite(
                    reactor.util.context.Context.of(REACTOR_SPAN_START_OPTIONS_KEY,
                        createOptionsWithCommonAttributes(SpanKind.CLIENT)
                            .setStartTimestamp(Instant.now())));
        }
        return messages;
    }

    public Context startSpanWithLinks(String spanName, List<ServiceBusMessage> batch, Function<ServiceBusMessage, Context> getMessageContext, Context parent) {
        if (tracer != null) {
            StartSpanOptions options = createOptionsWithCommonAttributes(SpanKind.CLIENT);
            for (ServiceBusMessage message : batch) {
                Context messageContext = getMessageContext.apply(message);
                reportMessageSpan(message, messageContext);
                addLink(message.getApplicationProperties(), null, options, messageContext);
            }

            return tracer.start(spanName, options, parent);
        }

        return parent;
    }

    Context startSpanWithLink(String spanName, ServiceBusReceivedMessage message, Context messageContext, Context parent) {
        if (tracer != null) {
            StartSpanOptions options = createOptionsWithCommonAttributes(SpanKind.CLIENT);
            if (message != null) {
                addLink(message.getApplicationProperties(), message.getEnqueuedTime(), options, messageContext);
            }

            return tracer.start(spanName, options, parent);
        }

        return parent;
    }

    /**
     * Starts span. Used by ServiceBus*Instrumentations.
     */
    Context startProcessSpan(String spanName, ServiceBusReceivedMessage message, Context parent) {
        if (tracer != null) {
            StartSpanOptions options = createOptionsWithCommonAttributes(SpanKind.CONSUMER);
            if (message.getEnqueuedTime() != null) {
                options.setAttribute(MESSAGE_ENQUEUED_TIME, message.getEnqueuedTime().toInstant().atOffset(ZoneOffset.UTC).toEpochSecond());
            }

            Context messageTraceContext = tracer.extractContext(name -> getStringValue(name, message.getApplicationProperties()));
            options.setRemoteParent(messageTraceContext);

            return tracer.start(spanName, options, parent);
        }

        return parent;
    }

    private Context startSpanWithLink(String spanName, ServiceBusMessage message, Context messageContext, Context parent) {
        if (tracer != null) {
            StartSpanOptions options = createOptionsWithCommonAttributes(SpanKind.CLIENT);
            if (message != null) {
                reportMessageSpan(message, messageContext);
                addLink(message.getApplicationProperties(), null, options, messageContext);
            }

            return tracer.start(spanName, options, parent);
        }

        return parent;
    }

    private void addLink(Map<String, Object> applicationProperties, OffsetDateTime enqueuedTime, StartSpanOptions spanBuilder, Context messageContext) {
        Map<String, Object> linkAttributes = new HashMap<>(1);
        if (enqueuedTime != null) {
            linkAttributes.put(MESSAGE_ENQUEUED_TIME, enqueuedTime.toInstant().atOffset(ZoneOffset.UTC).toEpochSecond());
        }

        if (!messageContext.getData(SPAN_CONTEXT_KEY).isPresent()) {
            messageContext = tracer.extractContext(name -> getStringValue(name, applicationProperties));
        }

        spanBuilder.addLink(new TracingLink(messageContext, linkAttributes));
    }

    private static String getStringValue(String name, Map<String, Object> applicationProperties) {
        Object value = applicationProperties.get(name);
        return value instanceof String ? ((String)value) : null;
    }

    private StartSpanOptions createOptionsWithCommonAttributes(SpanKind kind) {
        return new StartSpanOptions(kind).setAttribute(ENTITY_PATH_KEY, entityPath)
            .setAttribute(HOST_NAME_KEY, fullyQualifiedName);
    }

    private <T> void endSpan(Signal<T> signal) {
        if (tracer == null) {
            return;
        }

        Context span = signal.getContextView().getOrDefault(REACTOR_PARENT_TRACE_CONTEXT_KEY, Context.NONE);
        endSpan(signal.getThrowable(), span, null);
    }
}
