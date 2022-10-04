package com.azure.messaging.servicebus;

import com.azure.core.amqp.implementation.ClientConstants;
import com.azure.core.util.Context;
import com.azure.core.util.TelemetryAttributes;
import com.azure.core.util.metrics.DoubleHistogram;
import com.azure.core.util.metrics.LongCounter;
import com.azure.core.util.metrics.Meter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.azure.core.amqp.implementation.ClientConstants.ENTITY_NAME_KEY;
import static com.azure.core.amqp.implementation.ClientConstants.ENTITY_PATH_KEY;
import static com.azure.core.amqp.implementation.ClientConstants.HOSTNAME_KEY;

public class ServiceBusMetricsProvider {
    private static final String GENERIC_STATUS_KEY = "status";
    private final Meter meter;
    private final boolean isEnabled;

    private TelemetryAttributes sendAttributesSuccess;
    private TelemetryAttributes sendAttributesFailure;
    private TelemetryAttributes receiveAttributes;
    private LongCounter sentMessagesCounter;
    private DoubleHistogram consumerLag;

    public ServiceBusMetricsProvider(Meter meter, String namespace, String entityPath, String subscriptionName) {
        this.meter = meter;
        this.isEnabled = meter != null && meter.isEnabled();
        if (this.isEnabled) {
            Map<String, Object> commonAttributesMap = new HashMap<>(3);
            commonAttributesMap.put(HOSTNAME_KEY, namespace);
            int entityNameEnd = entityPath.indexOf('/');
            if (entityNameEnd > 0) {
                commonAttributesMap.put(ClientConstants.ENTITY_NAME_KEY,  entityPath.substring(0, entityNameEnd));
            } else {
                commonAttributesMap.put(ClientConstants.ENTITY_NAME_KEY,  entityPath);
            }

            if (subscriptionName != null) {
                commonAttributesMap.put("subscriptionName",  subscriptionName);
            }

            Map<String, Object> successMap = new HashMap<>(commonAttributesMap);
            successMap.put(GENERIC_STATUS_KEY, "ok");
            this.sendAttributesSuccess = meter.createAttributes(successMap);

            Map<String, Object> failureMap = new HashMap<>(commonAttributesMap);
            failureMap.put(GENERIC_STATUS_KEY, "error");
            this.sendAttributesFailure = new meter.createAttributes(failureMap);

            this.receiveAttributes = meter.createAttributes(commonAttributesMap);
            this.sentMessagesCounter = meter.createLongCounter("messaging.servicebus.messages.sent", "Number of sent messages", "messages");
            this.consumerLag = meter.createDoubleHistogram("messaging.servicebus.consumer.lag", "Difference between local time when event was received and the local time it was enqueued on broker.", "sec");
        }
    }

    public void reportBatchSend(ServiceBusMessageBatch batch, Throwable throwable, Context context) {
        if (isEnabled && sentMessagesCounter.isEnabled()) {
            TelemetryAttributes attributes = throwable == null ? sendAttributesSuccess : sendAttributesFailure;
            sentMessagesCounter.add(batch.getCount(), attributes, context);
        }
    }

    public void reportReceive(ServiceBusMessageContext messageContext) {
        if (isEnabled && consumerLag.isEnabled()) {
            OffsetDateTime enqueuedTime = messageContext.getMessage().getEnqueuedTime();
            double diff = 0d;
            if (enqueuedTime != null) {
                diff = Instant.now().toEpochMilli() - enqueuedTime.toInstant().toEpochMilli();
                if (diff < 0) {
                    // time skew on machines
                    diff = 0;
                }
            }
            consumerLag.record(diff / 1000d, receiveAttributes, Context.NONE);
        }
    }

    class AttributeCache {
        private final Map<String, TelemetryAttributes> attr = new ConcurrentHashMap<>();
        private final TelemetryAttributes commonAttr;
        private final Map<String, Object> commonMap;
        private final String dimensionName;

        AttributeCache(String dimensionName, Map<String, Object> common) {
            this.dimensionName = dimensionName;
            this.commonMap = common;
            this.commonAttr = meter.createAttributes(commonMap);
        }

        public TelemetryAttributes getOrCreate(String value) {
            if (value == null) {
                return commonAttr;
            }

            return attr.computeIfAbsent(value, this::create);
        }

        private TelemetryAttributes create(String value) {
            Map<String, Object> attributes = new HashMap<>(commonMap);
            attributes.put(dimensionName, value);
            return meter.createAttributes(attributes);
        }
    }

}
