// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.amqp.implementation;

import com.azure.core.util.Context;
import com.azure.core.util.tracing.SpanKind;
import com.azure.core.util.tracing.StartSpanOptions;
import com.azure.core.util.tracing.Tracer;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.codec.ReadableBuffer;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.reactor.Selectable;
import reactor.core.publisher.MonoSink;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static com.azure.core.util.tracing.Tracer.PARENT_TRACE_CONTEXT_KEY;

/**
 * Represents a work item that can be scheduled multiple times.
 */
class RetriableWorkItem {
    private final AtomicInteger retryAttempts = new AtomicInteger();
    private final MonoSink<DeliveryState> monoSink;
    private final TimeoutTracker timeoutTracker;
    private final ReadableBuffer encodedBuffer;
    private final byte[] encodedBytes;
    private final int messageFormat;
    private final int encodedMessageSize;
    private final Tracer tracer;
    private final DeliveryState deliveryState;
    private boolean waitingForAck;
    private Exception lastKnownException;
    private final AmqpMetricsProvider metricsProvider;
    private long tryStartTime = 0;
    private Context span = Context.NONE;
    private static final MethodHandle GET_SELECTABLE;

    static {
        MethodHandle getSelectable = null;
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            // ((InetSocketAddress)sender.getSession().getConnection().getTransport().getSelectable().getChannel().getRemoteAddress()).getAddress().getHostAddress()
            Class<?> transportImpl = Class.forName("org.apache.qpid.proton.engine.impl.TransportImpl", false, RetriableWorkItem.class.getClassLoader());
            Method pm = transportImpl.getDeclaredMethod("getSelectable");
            pm.setAccessible(true);
            getSelectable = lookup.unreflect(pm);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
        }
        GET_SELECTABLE = getSelectable;
    }

    RetriableWorkItem(ReadableBuffer buffer, int messageFormat, MonoSink<DeliveryState> monoSink, Duration timeout,
        DeliveryState deliveryState, AmqpMetricsProvider metricsProvider) {
        this.encodedBuffer = buffer;
        this.encodedBytes = null;
        this.encodedMessageSize = buffer.remaining();
        this.messageFormat = messageFormat;
        this.monoSink = monoSink;
        this.timeoutTracker = new TimeoutTracker(timeout, false);
        this.deliveryState = deliveryState;
        this.metricsProvider = metricsProvider;
        this.tracer = metricsProvider.getTracer();
    }

    RetriableWorkItem(byte[] bytes, int encodedMessageSize, int messageFormat, MonoSink<DeliveryState> monoSink, Duration timeout,
        DeliveryState deliveryState, AmqpMetricsProvider metricsProvider) {
        this.encodedBytes = bytes;
        this.encodedBuffer = null;
        this.encodedMessageSize = encodedMessageSize;
        this.messageFormat = messageFormat;
        this.monoSink = monoSink;
        this.timeoutTracker = new TimeoutTracker(timeout, false);
        this.deliveryState = deliveryState;
        this.metricsProvider = metricsProvider;
        this.tracer = metricsProvider.getTracer();
    }

    DeliveryState getDeliveryState() {
        return deliveryState;
    }

    boolean isDeliveryStateProvided() {
        return deliveryState != null;
    }

    TimeoutTracker getTimeoutTracker() {
        return timeoutTracker;
    }

    void success(DeliveryState deliveryState) {
        reportTelemetry(deliveryState, null);
        monoSink.success(deliveryState);
    }

    void error(Throwable error, DeliveryState deliveryState) {
        reportTelemetry(deliveryState, error);
        monoSink.error(error);
    }

    int incrementRetryAttempts() {
        return retryAttempts.incrementAndGet();
    }

    void beforeTry() {

        if (metricsProvider.isSendDeliveryEnabled()) {
            this.tryStartTime = Instant.now().toEpochMilli();
        }
    }

    boolean hasBeenRetried() {
        return retryAttempts.get() == 0;
    }

    int getEncodedMessageSize() {
        return encodedMessageSize;
    }

    int getMessageFormat() {
        return messageFormat;
    }

    Exception getLastKnownException() {
        return this.lastKnownException;
    }

    void setLastKnownException(Exception exception) {
        this.lastKnownException = exception;
    }

    void setWaitingForAck() {
        this.waitingForAck = true;
    }

    boolean isWaitingForAck() {
        return this.waitingForAck;
    }

    void send(Sender sender) {
        Context parent = monoSink.contextView().getOrDefault(PARENT_TRACE_CONTEXT_KEY, Context.NONE);
        StartSpanOptions startOptions = new StartSpanOptions(SpanKind.CLIENT);
        setNetworkAttributes(sender, startOptions);
        span = tracer.start("AMQP send", startOptions, parent);

        final int sentMsgSize;
        if (encodedBytes != null) {
            sentMsgSize = sender.send(encodedBytes, 0, encodedMessageSize);
        } else {
            encodedBuffer.rewind();
            sentMsgSize = sender.send(encodedBuffer);
        }
        assert sentMsgSize == encodedMessageSize : "Contract of the ProtonJ library for Sender. Send API changed";
    }

    private void reportTelemetry(DeliveryState deliveryState, Throwable error) {
        metricsProvider.recordSend(tryStartTime, deliveryState == null ? null : deliveryState.getType(), span);
        tracer.end(null, error, span);
    }

    private static void setNetworkAttributes(Sender sender, StartSpanOptions startOptions) {
        startOptions.setAttribute("network.protocol.name", "AMQP");
        startOptions.setAttribute("network.protocol.version", "1.0");

        Transport transport = sender.getSession().getConnection().getTransport();

        // TransportImpl is in impl package :(
        Selectable selectable = null;
        try {
            selectable = (Selectable) GET_SELECTABLE.invoke(transport);
        } catch (Throwable e) {
            return;
        }

        Channel channel = selectable.getChannel();

        setAttributes(startOptions, getRemoteAddress(channel), true);
        setAttributes(startOptions, getLocalAddress(channel), false);
    }

    private static SocketAddress getRemoteAddress(Channel channel) {
        try {
            if (SocketChannel.class.isAssignableFrom(channel.getClass())) {
                return ((SocketChannel) channel).getRemoteAddress();
            } else if (DatagramChannel.class.isAssignableFrom(channel.getClass())) {
                // not really possible, but just in case
                return ((DatagramChannel) channel).getRemoteAddress();
            }
        } catch (IOException e) {
        }
        return null;
    }

    private static SocketAddress getLocalAddress(Channel channel) {
        try {
            if (NetworkChannel.class.isAssignableFrom(channel.getClass())) {
                return ((NetworkChannel) channel).getLocalAddress();
            }
        } catch (IOException e) {
        }
        return null;
    }

    private static void setAttributes(StartSpanOptions startOptions, SocketAddress address, boolean remote) {
        if (address == null) {
            return;
        }

        if (InetSocketAddress.class.isAssignableFrom(address.getClass())) {
            InetSocketAddress inetAddress = (InetSocketAddress) address;
            if (remote) {
                startOptions.setAttribute("network.peer.name", inetAddress.getHostName());
                // set it once:
                startOptions.setAttribute("network.type", inetAddress.getAddress() instanceof Inet6Address ? "ipv6" : "ipv4");
            }

            startOptions.setAttribute(remote ? "network.peer.address" : "network.local.address", inetAddress.getAddress().getHostAddress());
            startOptions.setAttribute(remote ? "network.peer.port" : "network.local.port", inetAddress.getPort());
        } else if (address.getClass().getName().equals("java.net.UnixDomainSocketAddress")) { // for java 8, UnixDomainSocketAddress is added in 16
            // not really possible for AMQP, but just in case
            startOptions.setAttribute(remote ? "network.peer.address" : "network.local.address", address.toString());
            if (remote) { // set it once
                startOptions.setAttribute("network.transport", "unix");
            }
        }
    }
}
