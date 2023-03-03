// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.messaging.eventhubs.implementation.instrumentation;

import com.azure.core.util.Context;
import com.azure.core.util.logging.ClientLogger;
import com.azure.core.util.metrics.Meter;
import com.azure.core.util.tracing.SpanKind;
import com.azure.core.util.tracing.StartSpanOptions;
import com.azure.core.util.tracing.Tracer;
import com.azure.messaging.eventhubs.implementation.MessageUtils;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.message.Message;

import java.time.Instant;
import java.time.ZoneOffset;

import static com.azure.core.amqp.AmqpMessageConstant.ENQUEUED_TIME_UTC_ANNOTATION_NAME;
import static com.azure.messaging.eventhubs.implementation.instrumentation.EventHubsTracer.MESSAGE_ENQUEUED_TIME_ATTRIBUTE_NAME;

public class EventHubsConsumerInstrumentation {
    private static final ClientLogger LOGGER = new ClientLogger(EventHubsConsumerInstrumentation.class);
    private static final Symbol ENQUEUED_TIME_UTC_ANNOTATION_NAME_SYMBOL = Symbol.valueOf(ENQUEUED_TIME_UTC_ANNOTATION_NAME.getValue());
    private final EventHubsTracer tracer;
    private final EventHubsMetricsProvider meter;
    private final boolean isSync;

    public EventHubsConsumerInstrumentation(Tracer tracer, Meter meter, String fullyQualifiedName, String entityName, String consumerGroup, boolean isSyncConsumer) {
        this.tracer = new EventHubsTracer(tracer, fullyQualifiedName, entityName);
        this.meter = new EventHubsMetricsProvider(meter, fullyQualifiedName, entityName, consumerGroup);
        this.isSync = isSyncConsumer;
    }

    public EventHubsTracer getTracer() {
        return tracer;
    }

    public Context asyncConsume(String spanName, Message message, String partitionId, Context parent) {
        LOGGER.atInfo()
            .addKeyValue("spanName", spanName)
            .addKeyValue("partitionId", partitionId)
            .log("asyncConsume - start");
        if (!meter.isConsumerLagEnabled() && !tracer.isEnabled()) {
            return parent;
        }

        LOGGER.info("asyncConsume - enabled");

        Instant enqueuedTime = MessageUtils.getEnqueuedTime(message.getMessageAnnotations().getValue(), ENQUEUED_TIME_UTC_ANNOTATION_NAME_SYMBOL);
        LOGGER.atInfo()
            .addKeyValue("enqueuedTime", enqueuedTime)
            .log("asyncConsume - enqueuedTime");

        Context child = parent;
        if (tracer.isEnabled() && !isSync) {
            LOGGER.atInfo()
                .addKeyValue("message", message)
                .addKeyValue("message.getApplicationProperties()", message == null ? null : message.getApplicationProperties())
                .log("message.getApplicationProperties().getValue()");

            StartSpanOptions options = tracer.createStartOption(SpanKind.CONSUMER, EventHubsTracer.OperationName.PROCESS)
                .setAttribute(MESSAGE_ENQUEUED_TIME_ATTRIBUTE_NAME, enqueuedTime.atOffset(ZoneOffset.UTC).toEpochSecond())
                .setRemoteParent(tracer.extractContext(message.getApplicationProperties().getValue()));

            LOGGER.info("asyncConsume - options");

            child = tracer.startSpan(spanName, options, parent);
            LOGGER.info("asyncConsume - startSpan");
        }

        meter.reportReceive(enqueuedTime, partitionId, child);

        return child;
    }
}
