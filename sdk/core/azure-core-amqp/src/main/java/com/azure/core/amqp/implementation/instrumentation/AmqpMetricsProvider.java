// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.amqp.implementation.instrumentation;

import com.azure.core.amqp.exception.AmqpResponseCode;
import com.azure.core.amqp.implementation.ClientConstants;
import com.azure.core.util.Context;
import com.azure.core.util.CoreUtils;
import com.azure.core.util.MetricsOptions;
import com.azure.core.util.TelemetryAttributes;
import com.azure.core.util.logging.ClientLogger;
import com.azure.core.util.metrics.DoubleHistogram;
import com.azure.core.util.metrics.LongCounter;
import com.azure.core.util.metrics.LongGauge;
import com.azure.core.util.metrics.Meter;
import com.azure.core.util.metrics.MeterProvider;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static com.azure.core.amqp.implementation.instrumentation.InstrumentationUtils.ERROR_TYPE;
import static com.azure.core.amqp.implementation.instrumentation.InstrumentationUtils.MANAGEMENT_OPERATION_KEY;
import static com.azure.core.amqp.implementation.instrumentation.InstrumentationUtils.MESSAGING_DESTINATION_NAME;
import static com.azure.core.amqp.implementation.instrumentation.InstrumentationUtils.NETWORK_PEER_PORT;
import static com.azure.core.amqp.implementation.instrumentation.InstrumentationUtils.SERVER_ADDRESS;
import static com.azure.core.amqp.implementation.instrumentation.InstrumentationUtils.STATUS_CODE_KEY;
import static com.azure.core.amqp.implementation.instrumentation.InstrumentationUtils.getDurationInSeconds;

/**
 * Helper class responsible for efficient reporting metrics in AMQP core. It's efficient and safe to use when there is no
 * meter configured by client SDK when metrics are disabled.
 */
public class AmqpMetricsProvider {
    private static final ClientLogger LOGGER = new ClientLogger(AmqpMetricsProvider.class);

    private static final String AZURE_CORE_AMQP_PROPERTIES_NAME = "azure-core.properties";
    private static final String AZURE_CORE_AMQP_PROPERTIES_VERSION_KEY = "version";

    private static final String AZURE_CORE_VERSION = CoreUtils
        .getProperties(AZURE_CORE_AMQP_PROPERTIES_NAME)
        .getOrDefault(AZURE_CORE_AMQP_PROPERTIES_VERSION_KEY, null);

    private static final AutoCloseable NOOP_CLOSEABLE = () -> {
    };

    // all delivery state + 1 for `null` - we'll treat it as an error - no delivery was received
    private static final int DELIVERY_STATES_COUNT = DeliveryState.DeliveryStateType.values().length + 1;

    // all error codes + 1 for `null` - error, no response was received
    private static final int RESPONSE_CODES_COUNT = AmqpResponseCode.values().length + 1;
    private static final Meter DEFAULT_METER = MeterProvider.getDefaultProvider().createMeter("azure-core-amqp", AZURE_CORE_VERSION, new MetricsOptions());
    private static final AmqpMetricsProvider NOOP = new AmqpMetricsProvider();
    private final boolean isEnabled;
    private final Meter meter;
    private final Map<String, Object> commonAttributesMap;
    private final DoubleHistogram sendDuration;
    private final DoubleHistogram requestResponseDuration;
    private final LongCounter openConnections;
    private final DoubleHistogram connectDuration;
    private final DoubleHistogram connectionDuration;
    private final LongCounter sessionErrors;
    private final LongCounter linkErrors;
    private final LongCounter transportErrors;
    private final LongGauge prefetchedSequenceNumber;
    private final LongCounter addCredits;

    /**
     * Cache of sendDuration attributes. Each element has
     * namespace, entity name and path, and also a delivery state.
     * Element index is ordinal number of state in the enum definition.
     * <p>
     * The last element in the array represents no delivery (e.g. timeout or network issues)
     * case and w ill be stored as last element in the array.
      */
    private final TelemetryAttributes[] sendAttributeCache;

    /**
     * Stores attribute caches with Management operation and response code.
     * AmqpResponseCode ordinal number serves as index in this array,
     * (the last element represents no response).
     * <p>
     * Each element is a cache on its own that holds attribute sets for
     * namespace, entity name and path, and management operation.
     */
    private final AttributeCache[] requestResponseAttributeCache;

    /**
     * There is no enum for AMQP condition, so we just use a cache
     * that holds attribute sets representing namespace, entity name and path,
     * and error condition.
     * Error condition serves as a key, and other attributes are shared across all attribute sets.
     */
    private final AttributeCache amqpErrorAttributeCache;
    private final TelemetryAttributes commonAttributes;

    private AmqpMetricsProvider() {
        this.isEnabled = false;
        this.meter = DEFAULT_METER;
        this.commonAttributesMap = null;
        this.sendDuration = null;
        this.requestResponseDuration = null;

        this.openConnections = null;
        this.connectDuration = null;
        this.connectionDuration = null;

        this.sessionErrors = null;
        this.linkErrors = null;
        this.transportErrors = null;
        this.prefetchedSequenceNumber = null;
        this.addCredits = null;
        this.sendAttributeCache = null;
        this.requestResponseAttributeCache = null;
        this.amqpErrorAttributeCache = null;
        this.commonAttributes = null;
    }

    /**
     * The source of the error.
     */
    public enum ErrorSource {
        LINK,
        SESSION,
        TRANSPORT
    }

    /**
     * Creates an instance of {@link AmqpMetricsProvider}.
     *
     * @param meter The meter to use for metrics.
     * @param hostname The remote endpoint domain name.
     * @param port The remote endpoint port.
     * @param entityPath The entity path to use for metrics.
     */
    public AmqpMetricsProvider(Meter meter, String hostname, int port, String entityPath) {
        this.meter = meter != null ? meter : DEFAULT_METER;
        this.isEnabled = this.meter.isEnabled();

        if (isEnabled) {
            this.commonAttributesMap = new HashMap<>();
            commonAttributesMap.put(SERVER_ADDRESS, hostname);
            commonAttributesMap.put(NETWORK_PEER_PORT, port);

            if (entityPath != null) {
                int entityNameEnd = entityPath.indexOf('/');
                if (entityNameEnd > 0) {
                    commonAttributesMap.put(MESSAGING_DESTINATION_NAME,  entityPath.substring(0, entityNameEnd));
                    commonAttributesMap.put(ClientConstants.ENTITY_PATH_KEY, entityPath);
                } else {
                    commonAttributesMap.put(MESSAGING_DESTINATION_NAME,  entityPath);
                }
            }

            this.commonAttributes = this.meter.createAttributes(commonAttributesMap);
            this.requestResponseAttributeCache = new AttributeCache[RESPONSE_CODES_COUNT];
            this.sendAttributeCache = new TelemetryAttributes[DELIVERY_STATES_COUNT];
            this.amqpErrorAttributeCache = new AttributeCache(ERROR_TYPE, commonAttributesMap);
            this.sendDuration = this.meter.createDoubleHistogram("messaging.az.amqp.producer.send.duration", "Duration of AMQP-level send call.", "s");
            this.requestResponseDuration = this.meter.createDoubleHistogram("messaging.az.amqp.management.request.duration", "Duration of AMQP request-response operation.", "s");

            this.openConnections = this.meter.createLongCounter("connection.client.open_connections", "Number of currently open connections", "{connection}");
            this.connectDuration = this.meter.createDoubleHistogram("connection.client.connect_duration", "Duration of AMQP connection establishment", "s");
            this.connectionDuration = this.meter.createDoubleHistogram("connection.client.connection_duration", "Duration of AMQP connection", "s");

            this.sessionErrors = this.meter.createLongCounter("messaging.az.amqp.client.session.errors", "AMQP session errors", "errors");
            this.linkErrors = this.meter.createLongCounter("messaging.az.amqp.client.link.errors", "AMQP link errors", "errors");
            this.transportErrors = this.meter.createLongCounter("messaging.az.amqp.client.transport.errors", "AMQP session errors", "errors");
            this.addCredits = this.meter.createLongCounter("messaging.az.amqp.consumer.credits.requested", "Number of requested credits", "credits");
            this.prefetchedSequenceNumber = this.meter.createLongGauge("messaging.az.amqp.prefetch.sequence_number", "Last prefetched sequence number", "seqNo");
        } else {
            this.commonAttributesMap = null;
            this.sendDuration = null;
            this.requestResponseDuration = null;

            this.openConnections = null;
            this.connectDuration = null;
            this.connectionDuration = null;

            this.sessionErrors = null;
            this.linkErrors = null;
            this.transportErrors = null;
            this.prefetchedSequenceNumber = null;
            this.addCredits = null;
            this.sendAttributeCache = null;
            this.requestResponseAttributeCache = null;
            this.amqpErrorAttributeCache = null;
            this.commonAttributes = null;
        }
    }

    /**
     * Returns noop metrics provider.
     *
     * @return noop metrics provider.
     */
    public static AmqpMetricsProvider noop() {
        return NOOP;
    }

    /**
     * Checks if send delivery metric is enabled (for micro-optimizations).
     *
     * @return true if send delivery metric is enabled, false otherwise.
     */
    public boolean isSendDeliveryEnabled() {
        return isEnabled && sendDuration.isEnabled();
    }

    /**
     * Checks if request-response duration metric is enabled (for micro-optimizations).
     *
     * @return true if request-response duration metric is enabled, false otherwise.
     */
    public boolean isRequestResponseDurationEnabled() {
        return isEnabled && sendDuration.isEnabled();
    }


    /**
     * Checks if prefetched sequence number is enabled (for micro-optimizations).
     *
     * @return true if prefetched sequence number is enabled, false otherwise.
     */
    public boolean isPrefetchedSequenceNumberEnabled() {
        return isEnabled && prefetchedSequenceNumber.isEnabled();
    }


    /**
     * Checks if connection metrics are enabled (for micro-optimizations).
     *
     * @return true if one of the conneciton-level metrics is enabled, false otherwise.
     */
    public boolean areConnectionMetricEnabled() {
        return isEnabled && (connectionDuration.isEnabled() || connectDuration.isEnabled() || openConnections.isEnabled());
    }


    /**
     * Records duration of AMQP send call.
     *
     * @param startTime start time of the call.
     * @param deliveryState delivery state.
     */
    public void recordSend(Instant startTime, DeliveryState.DeliveryStateType deliveryState) {
        if (isEnabled && sendDuration.isEnabled()) {
            sendDuration.record(getDurationInSeconds(startTime), getDeliveryStateAttribute(deliveryState), Context.NONE);
        }
    }

    /**
     * Records duration of AMQP management call.
     *
     * @param startTime start time of the call.
     * @param operationName operation name.
     * @param responseCode response code.
     */
    public void recordRequestResponseDuration(Instant startTime, String operationName, AmqpResponseCode responseCode) {
        if (isEnabled && requestResponseDuration.isEnabled()) {
            requestResponseDuration.record(getDurationInSeconds(startTime),
                getResponseCodeAttributes(responseCode, operationName),
                Context.NONE);
        }
    }

    /**
     * Records connect attempt duration and increases the number of opened connections.
     *
     * @param initTime connection init time.
     */
    public void recordConnectionEstablished(Instant initTime) {
        if (isEnabled && connectDuration.isEnabled()) {
            openConnections.add(1, commonAttributes, Context.NONE);
            connectDuration.record(getDurationInSeconds(initTime), commonAttributes, Context.NONE);
        }
    }

    /**
     * Records connection close.
     *
     * @param condition error condition.
     * @param initTime connection init time.
     * @param establishedTime connection established time.
     */
    public void recordConnectionClosed(ErrorCondition condition, Instant initTime, Instant establishedTime) {
        if (isEnabled && (openConnections.isEnabled() || connectionDuration.isEnabled())) {
            TelemetryAttributes attributes = amqpErrorAttributeCache.getOrCreate(condition);
            openConnections.add(-1, attributes, Context.NONE);
            if (establishedTime == null) {
                // connection was never established, we should record error on connect duration
                // and not record connection duration
                connectDuration.record(getDurationInSeconds(initTime), attributes, Context.NONE);
            } else {
                connectionDuration.record(getDurationInSeconds(establishedTime), attributes, Context.NONE);
            }
        }
    }

    /**
     * Creates gauge subscription to report latest sequence number value.
     *
     * @param valueSupplier supplier of the sequence number value.
     * @return An instance of {@link AutoCloseable}.
     */
    public AutoCloseable trackPrefetchSequenceNumber(Supplier<Long> valueSupplier) {
        if (!isEnabled || !prefetchedSequenceNumber.isEnabled()) {
            return NOOP_CLOSEABLE;
        }

        return prefetchedSequenceNumber.registerCallback(valueSupplier, commonAttributes);
    }

    /**
     * Records that credits were added to link
     *
     * @param credits number of credits added.
     */
    public void recordAddCredits(int credits) {
        if (isEnabled && addCredits.isEnabled()) {
            addCredits.add(credits, commonAttributes, Context.NONE);
        }
    }

    /**
     * Records link error. Noop if condition is null (no error).
     *
     * @param source error source.
     * @param condition error condition.
     */
    public void recordHandlerError(ErrorSource source, ErrorCondition condition) {
        if (isEnabled && condition != null && condition.getCondition() != null) {
            TelemetryAttributes attributes = amqpErrorAttributeCache.getOrCreate(condition);
            switch (source) {
                case LINK:
                    if (linkErrors.isEnabled()) {
                        linkErrors.add(1, attributes, Context.NONE);
                    }
                    break;
                case SESSION:
                    if (sessionErrors.isEnabled()) {
                        sessionErrors.add(1, attributes, Context.NONE);
                    }
                    break;
                case TRANSPORT:
                    if (transportErrors.isEnabled()) {
                        transportErrors.add(1, attributes, Context.NONE);
                    }
                    break;
                default:
                    LOGGER.verbose("Unexpected error source: {}", source);
            }
        }
    }

    private TelemetryAttributes getDeliveryStateAttribute(DeliveryState.DeliveryStateType state) {
        // if there was no response, state is null and indicates a network (probably) error.
        // we don't have an enum for network issues and metric attributes cannot have arbitrary
        // high-cardinality data, so we'll just use vague "error" for it.
        int ind = state == null ? DELIVERY_STATES_COUNT - 1 : state.ordinal();
        TelemetryAttributes attrs = sendAttributeCache[ind];
        if (attrs != null) {
            return attrs;
        }

        return createDeliveryStateAttribute(state, ind);
    }

    private TelemetryAttributes getResponseCodeAttributes(AmqpResponseCode code, String operation) {
        // if there was no response, code is null and indicates a network (probably) error.
        // we don't have an enum for network issues and metric attributes cannot have arbitrary
        // high-cardinality data, so we'll just use vague "error" for it
        int ind = code == null ? RESPONSE_CODES_COUNT - 1 : code.ordinal();
        AttributeCache codeAttributes = requestResponseAttributeCache[ind];
        if (codeAttributes == null) {
            codeAttributes = createResponseCodeAttribute(code, ind);
        }

        return codeAttributes.getOrCreate(operation);
    }

    private synchronized AttributeCache createResponseCodeAttribute(AmqpResponseCode code, int ind) {
        Map<String, Object> attrs = new HashMap<>(commonAttributesMap);
        attrs.put(STATUS_CODE_KEY, responseCodeToLowerCaseString(code));
        requestResponseAttributeCache[ind] = new AttributeCache(MANAGEMENT_OPERATION_KEY, attrs);
        return requestResponseAttributeCache[ind];
    }

    private synchronized TelemetryAttributes createDeliveryStateAttribute(DeliveryState.DeliveryStateType state, int ind) {
        Map<String, Object> attrs = new HashMap<>(commonAttributesMap);
        attrs.put(ClientConstants.DELIVERY_STATE_KEY, deliveryStateToLowerCaseString(state));
        sendAttributeCache[ind] = this.meter.createAttributes(attrs);
        return sendAttributeCache[ind];
    }

    private static String deliveryStateToLowerCaseString(DeliveryState.DeliveryStateType state) {
        if (state == null) {
            return "_OTHER";
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
                return "_OTHER";
        }
    }

    private static String responseCodeToLowerCaseString(AmqpResponseCode response) {
        if (response == null) {
            return "error";
        }

        switch (response) {
            case OK:
                return "ok";
            case ACCEPTED:
                return "accepted";
            case BAD_REQUEST:
                return "bad_request";
            case NOT_FOUND:
                return "not_found";
            case FORBIDDEN:
                return "forbidden";
            case INTERNAL_SERVER_ERROR:
                return "internal_server_error";
            case UNAUTHORIZED:
                return "unauthorized";
            case CONTINUE:
                return "continue";
            case SWITCHING_PROTOCOLS:
                return "switching_protocols";
            case CREATED:
                return "created";
            case NON_AUTHORITATIVE_INFORMATION:
                return "not_authoritative_information";
            case NO_CONTENT:
                return "no_content";
            case RESET_CONTENT:
                return "reset_content";
            case PARTIAL_CONTENT:
                return "partial_content";
            case AMBIGUOUS:
                return "ambiguous";
            case MULTIPLE_CHOICES:
                return "multiple_choices";
            case MOVED:
                return "moved";
            case MOVED_PERMANENTLY:
                return "moved_permanently";
            case FOUND:
                return "found";
            case REDIRECT:
                return "redirect";
            case REDIRECT_METHOD:
                return "redirect_method";
            case SEE_OTHER:
                return "see_other";
            case NOT_MODIFIED:
                return "not_modified";
            case USE_PROXY:
                return "use_proxy";
            case UNUSED:
                return "unused";
            case REDIRECT_KEEP_VERB:
                return "redirect_keep_verb";
            case TEMPORARY_REDIRECT:
                return "temporary_redirect";
            case PAYMENT_REQUIRED:
                return "payment_required";
            case METHOD_NOT_ALLOWED:
                return "method_no_allowed";
            case NOT_ACCEPTABLE:
                return "not_acceptable";
            case PROXY_AUTHENTICATION_REQUIRED:
                return "proxy_authentication_required";
            case REQUEST_TIMEOUT:
                return "request_timeout";
            case CONFLICT:
                return "conflict";
            case GONE:
                return "gone";
            case LENGTH_REQUIRED:
                return "length_required";
            case PRECONDITION_FAILED:
                return "precondition_failed";
            case REQUEST_ENTITY_TOO_LARGE:
                return "request_entity_is_too_large";
            case REQUEST_URI_TOO_LONG:
                return "request_uri_too_long";
            case UNSUPPORTED_MEDIA_TYPE:
                return "unsupported_media_type";
            case REQUESTED_RANGE_NOT_SATISFIABLE:
                return "requested_range_not_satisfiable";
            case EXPECTATION_FAILED:
                return "expectation_failed";
            case UPGRADE_REQUIRED:
                return "upgrade_required";
            case NOT_IMPLEMENTED:
                return "no_implemented";
            case BAD_GATEWAY:
                return "bad_gateway";
            case SERVICE_UNAVAILABLE:
                return "service_unavailable";
            case GATEWAY_TIMEOUT:
                return "gateway_timeout";
            case HTTP_VERSION_NOT_SUPPORTED:
                return "http_version_not_supported";
            default:
                return "error";
        }
    }

    private class AttributeCache {
        private final Map<String, TelemetryAttributes> attr = new ConcurrentHashMap<>();
        private final Map<String, Object> common;
        private final String dimensionName;
        AttributeCache(String dimensionName, Map<String, Object> common) {
            this.dimensionName = dimensionName;
            this.common = common;
        }

        public TelemetryAttributes getOrCreate(ErrorCondition errorCondition) {
            String error = null;
            if (errorCondition != null) {
                Symbol conditionSymbol = errorCondition.getCondition();
                error = conditionSymbol != null ? conditionSymbol.toString() : null;
            }

            return error == null ? commonAttributes : attr.computeIfAbsent(error, this::create);
        }

        public TelemetryAttributes getOrCreate(String error) {
            return error == null ? commonAttributes : attr.computeIfAbsent(error, this::create);
        }

        private TelemetryAttributes create(String value) {
            Map<String, Object> attributes = new HashMap<>(common);
            attributes.put(dimensionName, value);
            return meter.createAttributes(attributes);
        }
    }
}
