package com.azure.core.util.tracing;

import com.azure.core.util.Context;
import com.azure.core.util.metrics.Meter;

public class Instrumentation {

    private final Tracer tracer;
    private final Meter meter;

    public Instrumentation(Tracer tracer, Meter meter) {
        this.tracer = tracer;
        this.meter = meter;
    }

    public Tracer getTracer() {
        return tracer;
    }

    public InstrumentationScope startScope(String operationName, Context context) {
        return new InstrumentationScope(tracer, meter).start(operationName, context);
    }
}
