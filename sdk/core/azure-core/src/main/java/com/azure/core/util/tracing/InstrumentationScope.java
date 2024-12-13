package com.azure.core.util.tracing;

import com.azure.core.util.Context;
import com.azure.core.util.metrics.Meter;

public class InstrumentationScope implements AutoCloseable {

    private final Tracer tracer;
    private final Meter meter;

    public InstrumentationScope(Tracer tracer, Meter meter) {
        this.tracer = tracer;
        this.meter = meter;
    }

    public InstrumentationScope start(String operationName, Context context) {
        System.out.println("start scope: " + operationName);
        return this;
    }

    public InstrumentationScope setError(Exception ex) {
        System.out.println("error: " + ex.getMessage());
        return this;
    }


    @Override
    public void close() {
        System.out.println("end scope");
    }
}
