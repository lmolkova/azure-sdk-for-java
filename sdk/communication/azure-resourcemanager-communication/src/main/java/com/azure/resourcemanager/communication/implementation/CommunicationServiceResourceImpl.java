// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

package com.azure.resourcemanager.communication.implementation;

import com.azure.core.management.SystemData;
import com.azure.resourcemanager.communication.fluent.models.CommunicationServiceResourceInner;
import com.azure.resourcemanager.communication.models.CommunicationServiceResource;
import com.azure.resourcemanager.communication.models.ProvisioningState;
import java.util.Collections;
import java.util.Map;

public final class CommunicationServiceResourceImpl implements CommunicationServiceResource {
    private CommunicationServiceResourceInner innerObject;

    private final com.azure.resourcemanager.communication.CommunicationManager serviceManager;

    CommunicationServiceResourceImpl(
        CommunicationServiceResourceInner innerObject,
        com.azure.resourcemanager.communication.CommunicationManager serviceManager) {
        this.innerObject = innerObject;
        this.serviceManager = serviceManager;
    }

    public String location() {
        return this.innerModel().location();
    }

    public SystemData systemData() {
        return this.innerModel().systemData();
    }

    public ProvisioningState provisioningState() {
        return this.innerModel().provisioningState();
    }

    public String hostname() {
        return this.innerModel().hostname();
    }

    public String dataLocation() {
        return this.innerModel().dataLocation();
    }

    public String notificationHubId() {
        return this.innerModel().notificationHubId();
    }

    public String version() {
        return this.innerModel().version();
    }

    public String immutableResourceId() {
        return this.innerModel().immutableResourceId();
    }

    public Map<String, String> tags() {
        Map<String, String> inner = this.innerModel().tags();
        if (inner != null) {
            return Collections.unmodifiableMap(inner);
        } else {
            return Collections.emptyMap();
        }
    }

    public CommunicationServiceResourceInner innerModel() {
        return this.innerObject;
    }

    private com.azure.resourcemanager.communication.CommunicationManager manager() {
        return this.serviceManager;
    }
}
