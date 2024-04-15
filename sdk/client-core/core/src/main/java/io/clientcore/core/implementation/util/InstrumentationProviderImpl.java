// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.clientcore.core.implementation.util;

import io.clientcore.core.http.models.HttpRequest;
import io.clientcore.core.http.models.Response;
import io.clientcore.core.http.pipeline.HttpPipelineNextPolicy;
import io.clientcore.core.http.pipeline.HttpPipelinePolicy;
import io.clientcore.core.util.ClientLogger;
import io.clientcore.core.util.InstrumentationOptions;
import io.clientcore.core.util.InstrumentationProvider;

public final class InstrumentationProviderImpl implements InstrumentationProvider {
    private static final ClientLogger LOGGER = new ClientLogger(InstrumentationProviderImpl.class);
    private static final InstrumentationProviderImpl INSTANCE = new InstrumentationProviderImpl();
    private static final HttpPipelinePolicy NOOP_POLICY = new NoopPolicy();
    private static final Providers<InstrumentationProvider> INSTRUMENTATION_PROVIDERS
            = new Providers<>(InstrumentationProvider.class, null, "TODO", LOGGER);

    private InstrumentationProviderImpl() {
    }

    public static InstrumentationProvider getInstance() {
        return INSTANCE;
    }

    public HttpPipelinePolicy createPhysicalPolicy(InstrumentationOptions options) {
        return INSTRUMENTATION_PROVIDERS.create(
                (provider) -> provider.createPhysicalPolicy(options),
                () -> NOOP_POLICY, null);
    }

    public HttpPipelinePolicy createLogicalPolicy(InstrumentationOptions options) {
        return INSTRUMENTATION_PROVIDERS.create(
                (provider) -> provider.createLogicalPolicy(options),
                () -> NOOP_POLICY, null);
    }

    private static class NoopPolicy implements HttpPipelinePolicy {

        @Override
        public Response<?> process(HttpRequest httpRequest, HttpPipelineNextPolicy next) {
            return next.process();
        }
    }
}
