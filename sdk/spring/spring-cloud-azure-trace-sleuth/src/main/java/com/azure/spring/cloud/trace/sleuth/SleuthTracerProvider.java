package com.azure.spring.cloud.trace.sleuth;

import com.azure.core.util.TracingOptions;
import com.azure.core.util.tracing.Tracer;
import com.azure.core.util.tracing.TracerProvider;

public class SleuthTracerProvider implements TracerProvider {
    @Override
    public Tracer createTracer(String libraryName, String libraryVersion, String azNamespace, TracingOptions options) {
        return new SleuthTracer(azNamespace, options);
    }
}
