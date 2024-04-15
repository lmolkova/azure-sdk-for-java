// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.clientcore.core.util;

import io.clientcore.core.util.configuration.Configuration;
import io.clientcore.core.util.configuration.ConfigurationProperty;
import io.clientcore.core.util.configuration.ConfigurationPropertyBuilder;

import java.util.Objects;

public class InstrumentationOptions {
    private static final ConfigurationProperty<Boolean> IS_TRACING_DISABLED_PROPERTY
            = ConfigurationPropertyBuilder.ofBoolean("tracing.disabled")
            .environmentVariableName(Configuration.PROPERTY_TRACING_DISABLED)
            .shared(true)
            .defaultValue(false)
            .build();

    private static final ConfigurationProperty<Boolean> IS_METRICS_DISABLED_PROPERTY
            = ConfigurationPropertyBuilder.ofBoolean("metrics.disabled")
            .environmentVariableName(Configuration.PROPERTY_METRICS_DISABLED)
            .shared(true)
            .defaultValue(false)
            .build();

    private static final ConfigurationProperty<String> PROVIDER_NAME_PROPERTY
            = ConfigurationPropertyBuilder.ofString("instrumentation.provider.implementation")
            .environmentVariableName(Configuration.PROPERTY_INSTRUMENTATION_IMPLEMENTATION)
            .shared(true)
            .build();

    private static final Configuration GLOBAL_CONFIG = Configuration.getGlobalConfiguration();
    private final Class<? extends InstrumentationProvider> instrumentationProvider;
    private boolean isTracingEnabled;
    private boolean isMetricsEnabled;

    /**
     * Creates new instance of {@link InstrumentationOptions}
     */
    public InstrumentationOptions() {
        this(GLOBAL_CONFIG);
    }

    protected InstrumentationOptions(Class<? extends InstrumentationProvider> tracerProvider) {
        this.instrumentationProvider = tracerProvider;
        this.isTracingEnabled = !GLOBAL_CONFIG.get(IS_TRACING_DISABLED_PROPERTY);
        this.isMetricsEnabled = !GLOBAL_CONFIG.get(IS_METRICS_DISABLED_PROPERTY);
    }

    private InstrumentationOptions(Configuration configuration) {
        isTracingEnabled = !configuration.get(IS_TRACING_DISABLED_PROPERTY);
        isMetricsEnabled = !configuration.get(IS_METRICS_DISABLED_PROPERTY);
        String className = configuration.get(PROVIDER_NAME_PROPERTY);
        instrumentationProvider = className != null ? getClassByName(className) : null;
    }

    public static InstrumentationOptions fromConfiguration(Configuration configuration) {
        return new InstrumentationOptions(configuration);
    }

    public boolean isTracingEnabled() {
        return this.isTracingEnabled;
    }

    public InstrumentationOptions setTracingEnabled(boolean enabled) {
        this.isTracingEnabled = enabled;
        return this;
    }

    public boolean isMetricsEnabled() {
        return this.isMetricsEnabled;
    }

    public InstrumentationOptions setMetricsEnabled(boolean enabled) {
        this.isMetricsEnabled = enabled;
        return this;
    }

    public Class<? extends InstrumentationProvider> getInstrumentationProvider() {
        return instrumentationProvider;
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<? extends T> getClassByName(String className) {
        Objects.requireNonNull(className, "'className' cannot be null");
        try {
            return (Class<? extends T>) Class.forName(className, false, LoggingOptions.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class '" + className + "' is not found on the classpath.", e);
        }
    }
}
