// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

package com.azure.resourcemanager.costmanagement.generated;

import com.azure.core.util.BinaryData;
import com.azure.resourcemanager.costmanagement.models.ExportDatasetConfiguration;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;

public final class ExportDatasetConfigurationTests {
    @org.junit.jupiter.api.Test
    public void testDeserialize() throws Exception {
        ExportDatasetConfiguration model =
            BinaryData.fromString("{\"columns\":[\"vudutncor\"]}").toObject(ExportDatasetConfiguration.class);
        Assertions.assertEquals("vudutncor", model.columns().get(0));
    }

    @org.junit.jupiter.api.Test
    public void testSerialize() throws Exception {
        ExportDatasetConfiguration model = new ExportDatasetConfiguration().withColumns(Arrays.asList("vudutncor"));
        model = BinaryData.fromObject(model).toObject(ExportDatasetConfiguration.class);
        Assertions.assertEquals("vudutncor", model.columns().get(0));
    }
}