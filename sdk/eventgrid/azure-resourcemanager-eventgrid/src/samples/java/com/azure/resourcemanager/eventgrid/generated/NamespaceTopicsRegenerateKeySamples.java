// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

package com.azure.resourcemanager.eventgrid.generated;

import com.azure.resourcemanager.eventgrid.models.TopicRegenerateKeyRequest;

/** Samples for NamespaceTopics RegenerateKey. */
public final class NamespaceTopicsRegenerateKeySamples {
    /*
     * x-ms-original-file: specification/eventgrid/resource-manager/Microsoft.EventGrid/preview/2023-06-01-preview/examples/NamespaceTopics_RegenerateKey.json
     */
    /**
     * Sample code: NamespaceTopics_RegenerateKey.
     *
     * @param manager Entry point to EventGridManager.
     */
    public static void namespaceTopicsRegenerateKey(com.azure.resourcemanager.eventgrid.EventGridManager manager) {
        manager
            .namespaceTopics()
            .regenerateKey(
                "examplerg",
                "examplenamespace2",
                "examplenamespacetopic2",
                new TopicRegenerateKeyRequest().withKeyName("fakeTokenPlaceholder"),
                com.azure.core.util.Context.NONE);
    }
}