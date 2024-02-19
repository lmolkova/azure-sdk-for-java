// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.amqp.implementation.instrumentation;

import com.azure.core.amqp.exception.AmqpException;
import reactor.core.Exceptions;

import java.time.Duration;
import java.time.Instant;

/**
 * Utility class to help with OpenTelemetry instrumentation.
 */
public final class InstrumentationUtils {
    // Attribute names based on OpenTelemetry specification
    // https://github.com/open-telemetry/semantic-conventions/blob/main/docs/messaging/messaging-spans.md
    public static final String SERVER_ADDRESS = "server.address";
    public static final String ERROR_TYPE = "error.type";
    public static final String STATUS_CODE_KEY = "az.amqp.status_code";
    public static final String MANAGEMENT_OPERATION_KEY = "az.amqp.operation";
    public static final String NETWORK_PEER_PORT = "network.peer.port";
    public static final String MESSAGING_DESTINATION_NAME = "messaging.destination.name";

    // metrics
    // constant attribute values
    public static final String CANCELLED_ERROR_TYPE_VALUE = "cancelled";

    // _OTHER is a magic string defined in OpenTelemetry for 'unknown' errors
    /**
     * The value for an error type when the error is not known.
     */
    public static final String OTHER_ERROR_TYPE_VALUE =  "_OTHER";

    /**
     * Gets the error type from the given {@code error}.
     *
     * @param error The error to get the type from.
     * @return The error type.
     */
    public static String getErrorType(Throwable error) {
        if (error == null) {
            return null;
        }

        error = Exceptions.unwrap(error);

        if (error instanceof AmqpException && ((AmqpException) error).getErrorCondition() != null) {
            return ((AmqpException) error).getErrorCondition().getErrorCondition();
        }

        return error.getClass().getName();
    }

    /**
     * Gets the duration in seconds from the given {@code startTime} till now.
     *
     * @param error The error to get the type from.
     * @return The error type.
     */
    public static Throwable unwrap(Throwable error) {
        error = Exceptions.unwrap(error);

        if (error instanceof AmqpException && error.getCause() != null) {
            return error.getCause();
        }

        return error;
    }

    /**
     * Gets the duration in seconds from the given {@code startTime} till now.
     *
     * @param startTime The start time.
     * @return The duration in seconds.
     */
    public static double getDurationInSeconds(Instant startTime) {
        long durationNanos = Duration.between(startTime, Instant.now()).toNanos();
        if (durationNanos < 0) {
            // we use this method to get lag, so need to take care of time skew on different machines
            return 0d;
        }
        return durationNanos / 1_000_000_000d;
    }

    private InstrumentationUtils() {
    }
}
