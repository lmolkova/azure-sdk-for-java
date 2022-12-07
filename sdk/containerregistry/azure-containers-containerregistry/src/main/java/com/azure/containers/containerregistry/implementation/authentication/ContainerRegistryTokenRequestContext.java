// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.containers.containerregistry.implementation.authentication;

import com.azure.core.credential.TokenRequestContext;
import com.azure.core.util.Context;

/**
 * A token request context associated with a given container registry token.
 */
public class ContainerRegistryTokenRequestContext extends TokenRequestContext {
    private final String scope;
    private final String serviceName;
    private final Context context;

    /**
     * Creates an instance of TokenRequestContext.
     * @param serviceName the service name of the registry.
     * @param scope token scope.
     */
    public ContainerRegistryTokenRequestContext(String serviceName, String scope, Context context) {
        this.serviceName = serviceName;
        this.scope = scope;
        this.context = context;
    }

    /**
     * Get the service name.
     * @return service name.
     */
    public String getServiceName() {
        return this.serviceName;
    }

    /**
     * Get's the token scope.
     * @return scope for the context.
     */
    public String getScope() {
        return this.scope;
    }

    public Context getContext() {
        return this.context;
    }
}
