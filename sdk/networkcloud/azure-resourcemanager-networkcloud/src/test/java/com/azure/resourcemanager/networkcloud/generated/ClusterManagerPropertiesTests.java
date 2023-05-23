// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

package com.azure.resourcemanager.networkcloud.generated;

import com.azure.core.util.BinaryData;
import com.azure.resourcemanager.networkcloud.fluent.models.ClusterManagerProperties;
import com.azure.resourcemanager.networkcloud.models.ManagedResourceGroupConfiguration;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;

public final class ClusterManagerPropertiesTests {
    @org.junit.jupiter.api.Test
    public void testDeserialize() throws Exception {
        ClusterManagerProperties model =
            BinaryData
                .fromString(
                    "{\"analyticsWorkspaceId\":\"ixzbinjeputtmryw\",\"availabilityZones\":[\"oqftiyqzrnkcq\",\"yx\",\"whzlsicohoq\",\"nwvlryavwhheunmm\"],\"clusterVersions\":[{\"supportExpiryDate\":\"xzko\",\"targetClusterVersion\":\"cukoklyaxuconu\"},{\"supportExpiryDate\":\"zf\",\"targetClusterVersion\":\"eyp\"}],\"detailedStatus\":\"Error\",\"detailedStatusMessage\":\"jmwvvj\",\"fabricControllerId\":\"kt\",\"managedResourceGroupConfiguration\":{\"location\":\"enhwlrs\",\"name\":\"rzpwvlqdqgbiq\"},\"managerExtendedLocation\":{\"name\":\"ihkaetcktvfc\",\"type\":\"vf\"},\"provisioningState\":\"Updating\",\"vmSize\":\"m\"}")
                .toObject(ClusterManagerProperties.class);
        Assertions.assertEquals("ixzbinjeputtmryw", model.analyticsWorkspaceId());
        Assertions.assertEquals("oqftiyqzrnkcq", model.availabilityZones().get(0));
        Assertions.assertEquals("kt", model.fabricControllerId());
        Assertions.assertEquals("enhwlrs", model.managedResourceGroupConfiguration().location());
        Assertions.assertEquals("rzpwvlqdqgbiq", model.managedResourceGroupConfiguration().name());
        Assertions.assertEquals("m", model.vmSize());
    }

    @org.junit.jupiter.api.Test
    public void testSerialize() throws Exception {
        ClusterManagerProperties model =
            new ClusterManagerProperties()
                .withAnalyticsWorkspaceId("ixzbinjeputtmryw")
                .withAvailabilityZones(Arrays.asList("oqftiyqzrnkcq", "yx", "whzlsicohoq", "nwvlryavwhheunmm"))
                .withFabricControllerId("kt")
                .withManagedResourceGroupConfiguration(
                    new ManagedResourceGroupConfiguration().withLocation("enhwlrs").withName("rzpwvlqdqgbiq"))
                .withVmSize("m");
        model = BinaryData.fromObject(model).toObject(ClusterManagerProperties.class);
        Assertions.assertEquals("ixzbinjeputtmryw", model.analyticsWorkspaceId());
        Assertions.assertEquals("oqftiyqzrnkcq", model.availabilityZones().get(0));
        Assertions.assertEquals("kt", model.fabricControllerId());
        Assertions.assertEquals("enhwlrs", model.managedResourceGroupConfiguration().location());
        Assertions.assertEquals("rzpwvlqdqgbiq", model.managedResourceGroupConfiguration().name());
        Assertions.assertEquals("m", model.vmSize());
    }
}