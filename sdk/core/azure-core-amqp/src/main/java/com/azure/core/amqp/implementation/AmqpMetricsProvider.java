// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.amqp.implementation;

import com.azure.core.util.Context;
import com.azure.core.util.CoreUtils;
import com.azure.core.util.MetricsOptions;
import com.azure.core.util.TelemetryAttributes;
import com.azure.core.util.logging.ClientLogger;
import com.azure.core.util.metrics.DoubleHistogram;
import com.azure.core.util.metrics.LongCounter;
import com.azure.core.util.metrics.Meter;
import com.azure.core.util.metrics.MeterProvider;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.message.Message;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.azure.core.amqp.AmqpMessageConstant.ENQUEUED_TIME_UTC_ANNOTATION_NAME;

/**
 * Helper class responsible for efficient reporting metrics in AMQP core. It's efficient and safe to use when there is no
 * meter configured by client SDK when metrics are disabled.
 */
public class AmqpMetricsProvider {
    private static final ClientLogger LOGGER = new ClientLogger(AmqpMetricsProvider.class);
    private static final AmqpMetricsProvider NOOP = new AmqpMetricsProvider();
    private static final Symbol ENQUEUED_TIME_ANNOTATION = Symbol.valueOf(ENQUEUED_TIME_UTC_ANNOTATION_NAME.getValue());

    private static final String AZURE_CORE_AMQP_PROPERTIES_NAME = "azure-core.properties";
    private static final String AZURE_CORE_AMQP_PROPERTIES_VERSION_KEY = "version";

    private static final String AZURE_CORE_VERSION = CoreUtils
        .getProperties(AZURE_CORE_AMQP_PROPERTIES_NAME)
        .getOrDefault(AZURE_CORE_AMQP_PROPERTIES_VERSION_KEY, null);

    private static final Meter DEFAULT_METER = MeterProvider.getDefaultProvider().createMeter("azure-core-amqp", AZURE_CORE_VERSION, new MetricsOptions());

    private final boolean isEnabled;
    private final Meter meter;
    private Map<String, Object> commonAttributesMap;
    private DoubleHistogram sendDuration = null;
    private LongCounter activeConnections = null;
    private LongCounter closedConnections = null;
    private LongCounter sessionErrors = null;
    private LongCounter linkErrors = null;
    private DoubleHistogram receivedLag = null;
    private LongCounter addCredits = null;
    private AttributeCache sendDeliveryAttributeCache = null;
    private AttributeCache amqpErrorAttributeCache = null;
    private TelemetryAttributes commonAttributes = null;

    private AmqpMetricsProvider() {
        this.isEnabled = false;
        this.meter = null;
    }

    public AmqpMetricsProvider(Meter meter, String namespace, String entityPath) {
        Objects.requireNonNull(namespace, "'namespace' cannot be null");

        this.meter = meter != null ? meter : DEFAULT_METER;
        this.isEnabled = this.meter.isEnabled();

        if (isEnabled) {
            this.commonAttributesMap = new HashMap<>();
            commonAttributesMap.put(ClientConstants.HOSTNAME_KEY, namespace);

            if (entityPath != null) {
                int entityNameEnd = entityPath.indexOf('/');
                if (entityNameEnd > 0) {
                    commonAttributesMap.put(ClientConstants.ENTITY_NAME_KEY,  entityPath.substring(0, entityNameEnd));
                    commonAttributesMap.put(ClientConstants.ENTITY_PATH_KEY, entityPath);
                } else {
                    commonAttributesMap.put(ClientConstants.ENTITY_NAME_KEY,  entityPath);
                }
            }

            this.commonAttributes = this.meter.createAttributes(commonAttributesMap);
            this.sendDeliveryAttributeCache = new AttributeCache(ClientConstants.DELIVERY_STATE_KEY);
            this.amqpErrorAttributeCache = new AttributeCache(ClientConstants.ERROR_CONDITION_KEY);
            this.sendDuration = this.meter.createDoubleHistogram("messaging.az.amqp.client.duration", "AMQP request client call duration", "ms");
            this.activeConnections = this.meter.createLongUpDownCounter("messaging.az.amqp.client.connections.usage", "Active connections", "connections");
            this.closedConnections = this.meter.createLongCounter("messaging.az.amqp.client.connections.closed", "Closed connections", "connections");
            this.sessionErrors = this.meter.createLongCounter("messaging.az.amqp.client.session.errors", "AMQP session errors", "errors");
            this.linkErrors = this.meter.createLongCounter("messaging.az.amqp.client.link.errors", "AMQP link errors", "errors");
            this.addCredits = this.meter.createLongCounter("messaging.az.amqp.consumer.credits.requested", "Number of requested credits", "credits");
            this.receivedLag = this.meter.createDoubleHistogram("messaging.az.amqp.consumer.lag", "Approximate lag between time message was received and time it was enqueued on the broker.", "sec");
        }
    }

    public static AmqpMetricsProvider noop() {
        return NOOP;
    }

    /**
     * Checks if record delivers is enabled (for micro-optimizations).
     */
    public boolean isSendDeliveryEnabled() {
        return isEnabled && sendDuration.isEnabled();
    }

    /**
     * Records duration of AMQP send call.
     */
    public void recordSendDelivery(long start, DeliveryState.DeliveryStateType deliveryState) {
        if (isEnabled && sendDuration.isEnabled()) {
            String deliveryStateStr = deliveryStateToLowerCaseString(deliveryState);
            TelemetryAttributes attributes = sendDeliveryAttributeCache.getOrCreate(deliveryStateStr);
            sendDuration.record(Instant.now().toEpochMilli() - start, attributes, Context.NONE);
        }
    }

    /**
     * Records connection init.
     */
    public void recordConnectionInit() {
        if (isEnabled && activeConnections.isEnabled()) {
            activeConnections.add(1, commonAttributes, Context.NONE);
        }
    }

    /**
     * Records connection close.
     */
    public void recordConnectionClosed(ErrorCondition condition) {
        if (isEnabled) {
            if (activeConnections.isEnabled()) {
                activeConnections.add(-1, commonAttributes, Context.NONE);
            }

            if (closedConnections.isEnabled()) {
                Symbol conditionSymbol = condition != null ? condition.getCondition() : null;
                String conditionStr = conditionSymbol != null ? conditionSymbol.toString() : "ok";
                closedConnections.add(1, amqpErrorAttributeCache.getOrCreate(conditionStr), Context.NONE);
            }
        }
    }

    /**
     * Records the message was received.
     */
    public void recordReceivedMessage(Message message) {
        if (!isEnabled || !receivedLag.isEnabled()
            || message == null
            || message.getMessageAnnotations() == null
            || message.getBody() == null) {
            return;
        }

        Map<Symbol, Object> properties = message.getMessageAnnotations().getValue();
        Object enqueuedTimeDate = properties != null ? properties.get(ENQUEUED_TIME_ANNOTATION) : null;
        if (enqueuedTimeDate instanceof Date) {
            Instant enqueuedTime = ((Date) enqueuedTimeDate).toInstant();
            long deltaMs = Instant.now().toEpochMilli() - enqueuedTime.toEpochMilli();
            if (deltaMs < 0) {
                deltaMs = 0;
            }
            receivedLag.record(deltaMs / 1000d, commonAttributes, Context.NONE);
        } else {
            LOGGER.verbose("Received message has unexpected `x-opt-enqueued-time` annotation value - `{}`. Ignoring it.", enqueuedTimeDate);
        }
    }

    /**
     * Records that credits were added to link
     */
    public void recordAddCredits(int credits) {
        if (isEnabled && addCredits.isEnabled()) {
            addCredits.add(credits, commonAttributes, Context.NONE);
        }
    }

    /**
     * Records link error. Noop if condition is null (no error).
     */
    public void recordLinkError(ErrorCondition condition) {
        if (isEnabled && linkErrors.isEnabled() && condition != null && condition.getCondition() != null) {
            linkErrors.add(1,
                amqpErrorAttributeCache.getOrCreate(condition.getCondition().toString()),
                Context.NONE);
        }
    }

    /**
     * Records session error. Noop if condition is null (no error).
     */
    public void recordSessionError(ErrorCondition condition) {
        if (isEnabled && sessionErrors.isEnabled() && condition != null && condition.getCondition() != null) {
            sessionErrors.add(1,
                amqpErrorAttributeCache.getOrCreate(condition.getCondition().toString()),
                Context.NONE);
        }
    }

    private static String deliveryStateToLowerCaseString(DeliveryState.DeliveryStateType state) {
        if (state == null) {
            return "unknown";
        }

        switch (state) {
            case Accepted:
                return "accepted";
            case Declared:
                return "declared";
            case Modified:
                return "modified";
            case Received:
                return "received";
            case Rejected:
                return "rejected";
            case Released:
                return "released";
            case Transactional:
                return "transactional";
            default:
                return "unknown";
        }
    }

    private class AttributeCache {
        private final Map<String, TelemetryAttributes> attr = new ConcurrentHashMap<>();
        private final String dimensionName;
        AttributeCache(String dimensionName) {
            this.dimensionName = dimensionName;
        }

        public TelemetryAttributes getOrCreate(String value) {
            return attr.computeIfAbsent(value, this::create);
        }

        private TelemetryAttributes create(String value) {
            Map<String, Object> attributes = new HashMap<>(commonAttributesMap);
            attributes.put(dimensionName, value);
            return meter.createAttributes(attributes);
        }
    }
}
