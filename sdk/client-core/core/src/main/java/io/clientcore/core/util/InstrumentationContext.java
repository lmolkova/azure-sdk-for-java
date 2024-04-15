package io.clientcore.core.util;

public class InstrumentationContext {
    private Object traceContext;
    private String operationName;
    private int retryCount;

    public InstrumentationContext() {
    }

    public InstrumentationContext setTraceContext(Object traceContext) {
        this.traceContext = traceContext;
        return this;
    }

    public Object getTraceContext() {
        return this.traceContext;
    }

    public String getOperationName() {
        return this.operationName;
    }

    public InstrumentationContext setRetryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public int getRetryCount() {
        return this.retryCount;
    }

    public InstrumentationContext setOperationName(String operationName) {
        this.operationName = operationName;
        return this;
    }
}
