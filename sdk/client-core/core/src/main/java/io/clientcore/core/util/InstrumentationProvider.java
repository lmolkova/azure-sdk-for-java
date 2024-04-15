package io.clientcore.core.util;

import io.clientcore.core.http.pipeline.HttpPipelinePolicy;
import io.clientcore.core.implementation.util.InstrumentationProviderImpl;

public interface InstrumentationProvider {
    HttpPipelinePolicy createPhysicalPolicy(InstrumentationOptions options);
    HttpPipelinePolicy createLogicalPolicy(InstrumentationOptions options);

    static InstrumentationProvider getProvider() {
        return InstrumentationProviderImpl.getInstance();
    }
}
