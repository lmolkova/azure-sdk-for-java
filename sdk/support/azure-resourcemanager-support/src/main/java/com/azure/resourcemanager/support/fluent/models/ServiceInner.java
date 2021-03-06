// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

package com.azure.resourcemanager.support.fluent.models;

import com.azure.core.annotation.Fluent;
import com.azure.core.annotation.JsonFlatten;
import com.azure.core.util.logging.ClientLogger;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Object that represents a Service resource. */
@JsonFlatten
@Fluent
public class ServiceInner {
    @JsonIgnore private final ClientLogger logger = new ClientLogger(ServiceInner.class);

    /*
     * Id of the resource.
     */
    @JsonProperty(value = "id", access = JsonProperty.Access.WRITE_ONLY)
    private String id;

    /*
     * Name of the resource.
     */
    @JsonProperty(value = "name", access = JsonProperty.Access.WRITE_ONLY)
    private String name;

    /*
     * Type of the resource 'Microsoft.Support/services'.
     */
    @JsonProperty(value = "type", access = JsonProperty.Access.WRITE_ONLY)
    private String type;

    /*
     * Localized name of the Azure service.
     */
    @JsonProperty(value = "properties.displayName")
    private String displayName;

    /*
     * ARM Resource types.
     */
    @JsonProperty(value = "properties.resourceTypes")
    private List<String> resourceTypes;

    /**
     * Get the id property: Id of the resource.
     *
     * @return the id value.
     */
    public String id() {
        return this.id;
    }

    /**
     * Get the name property: Name of the resource.
     *
     * @return the name value.
     */
    public String name() {
        return this.name;
    }

    /**
     * Get the type property: Type of the resource 'Microsoft.Support/services'.
     *
     * @return the type value.
     */
    public String type() {
        return this.type;
    }

    /**
     * Get the displayName property: Localized name of the Azure service.
     *
     * @return the displayName value.
     */
    public String displayName() {
        return this.displayName;
    }

    /**
     * Set the displayName property: Localized name of the Azure service.
     *
     * @param displayName the displayName value to set.
     * @return the ServiceInner object itself.
     */
    public ServiceInner withDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    /**
     * Get the resourceTypes property: ARM Resource types.
     *
     * @return the resourceTypes value.
     */
    public List<String> resourceTypes() {
        return this.resourceTypes;
    }

    /**
     * Set the resourceTypes property: ARM Resource types.
     *
     * @param resourceTypes the resourceTypes value to set.
     * @return the ServiceInner object itself.
     */
    public ServiceInner withResourceTypes(List<String> resourceTypes) {
        this.resourceTypes = resourceTypes;
        return this;
    }

    /**
     * Validates the instance.
     *
     * @throws IllegalArgumentException thrown if the instance is not valid.
     */
    public void validate() {
    }
}
