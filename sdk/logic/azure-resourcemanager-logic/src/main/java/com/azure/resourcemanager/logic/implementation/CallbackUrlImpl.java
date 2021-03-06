// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

package com.azure.resourcemanager.logic.implementation;

import com.azure.resourcemanager.logic.fluent.models.CallbackUrlInner;
import com.azure.resourcemanager.logic.models.CallbackUrl;

public final class CallbackUrlImpl implements CallbackUrl {
    private CallbackUrlInner innerObject;

    private final com.azure.resourcemanager.logic.LogicManager serviceManager;

    CallbackUrlImpl(CallbackUrlInner innerObject, com.azure.resourcemanager.logic.LogicManager serviceManager) {
        this.innerObject = innerObject;
        this.serviceManager = serviceManager;
    }

    public String value() {
        return this.innerModel().value();
    }

    public CallbackUrlInner innerModel() {
        return this.innerObject;
    }

    private com.azure.resourcemanager.logic.LogicManager manager() {
        return this.serviceManager;
    }
}
