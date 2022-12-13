package com.azure.spring.cloud.trace.sleuth;

import com.azure.core.util.TracingOptions;
import org.springframework.cloud.sleuth.Tracer;

public class SleuthTracingOptions extends TracingOptions {
    private Tracer sleuthTracer;

    public SleuthTracingOptions setTracer(Tracer sleuthTracer) {
        this.sleuthTracer = sleuthTracer;
        return this;
    }

    public Tracer getTracer() {
        return sleuthTracer;
    }
}
