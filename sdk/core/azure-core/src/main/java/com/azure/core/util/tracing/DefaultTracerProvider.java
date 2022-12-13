// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.util.tracing;

import com.azure.core.util.Configuration;
import com.azure.core.util.CoreUtils;
import com.azure.core.util.TracingOptions;
import com.azure.core.util.logging.ClientLogger;

import java.util.Iterator;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

final class DefaultTracerProvider implements TracerProvider {
    private static final TracerProvider INSTANCE = new DefaultTracerProvider();
    private static RuntimeException error;
    private static final ClientLogger LOGGER = new ClientLogger(DefaultTracerProvider.class);
    private static TracerProvider tracerProvider = initialize();
    private static Tracer fallbackTracer;


    private DefaultTracerProvider() {
    }

    @SuppressWarnings("unchecked")
    private static TracerProvider initialize() {
        // TODO: make consistent with HTTP clients
        String providerClassName = Configuration.getGlobalConfiguration().get("TRACER_PROVIDER_CLASSNAME");
        if (!CoreUtils.isNullOrEmpty(providerClassName)) {
            RuntimeException error;
            try {
                Class<?> providerClass = Class.forName(providerClassName);
                if (TracerProvider.class.isAssignableFrom(providerClass)) {
                    return ((Class<TracerProvider>) providerClass).getDeclaredConstructor().newInstance();
                } else {
                    error = new IllegalStateException("foo");
                }
            } catch (ReflectiveOperationException e) {
                error = new IllegalStateException(e);
            }

            LOGGER.error("Can't load provided class", error);
            return null;
        }

        // Use as classloader to load provider-configuration files and provider classes the classloader
        // that loaded this class. In most cases this will be the System classloader.
        // But this choice here provides additional flexibility in managed environments that control
        // classloading differently (OSGi, Spring and others) and don't/ depend on the
        // System classloader to load Meter classes.
        ServiceLoader<TracerProvider> serviceLoader = ServiceLoader.load(TracerProvider.class, TracerProvider.class.getClassLoader());
        Iterator<TracerProvider> iterator = serviceLoader.iterator();
        if (iterator.hasNext()) {
            TracerProvider provider = iterator.next();

            if (iterator.hasNext()) {
                String allProviders = StreamSupport.stream(serviceLoader.spliterator(), false)
                    .map(p -> p.getClass().getName())
                    .collect(Collectors.joining(", "));

                // TODO (lmolkova) add configuration to allow picking specific provider
                String message = String.format("Expected only one TracerProvider on the classpath, but found multiple providers: %s. "
                    + "Please pick one TracerProvider implementation and remove or exclude packages that bring other implementations", allProviders);

                error = new IllegalStateException(message);
                LOGGER.error(message);
            } else {
                error = null;
                LOGGER.info("Found TracerProvider implementation on the classpath: {}", tracerProvider.getClass().getName());
            }

            return provider;
        } else {
            error = null;
            fallbackTracer = createFallbackTracer();
            return null;
        }
    }

    private static Tracer createFallbackTracer() {
        // backward compatibility with preview OTel plugin - it didn't have TracerProvider
        ServiceLoader<Tracer> serviceLoader = ServiceLoader.load(Tracer.class, Tracer.class.getClassLoader());
        Iterator<Tracer> iterator = serviceLoader.iterator();
        if (iterator.hasNext()) {
            Tracer tracer = iterator.next();
            LOGGER.info("Found Tracer implementation on the classpath: {}", tracer.getClass().getName());
            return tracer;
        }

        return null;
    }

    static TracerProvider getInstance() {
        if (error != null) {
            throw LOGGER.logThrowableAsError(error);
        }

        return INSTANCE;
    }

    public Tracer createTracer(String libraryName, String libraryVersion, String azNamespace, TracingOptions options) {
        Objects.requireNonNull(libraryName, "'libraryName' cannot be null.");

        if (options != null && !options.isEnabled()) {
            return NoopTracer.INSTANCE;
        }

        if (tracerProvider != null) {
            return tracerProvider.createTracer(libraryName, libraryVersion, azNamespace, options);
        } else if (fallbackTracer != null) {
            return fallbackTracer;
        }

        return NoopTracer.INSTANCE;
    }
}
