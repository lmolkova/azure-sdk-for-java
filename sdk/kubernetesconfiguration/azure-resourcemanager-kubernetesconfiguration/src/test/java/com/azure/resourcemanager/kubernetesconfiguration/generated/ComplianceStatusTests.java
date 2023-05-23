// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

package com.azure.resourcemanager.kubernetesconfiguration.generated;

import com.azure.core.util.BinaryData;
import com.azure.resourcemanager.kubernetesconfiguration.models.ComplianceStatus;
import com.azure.resourcemanager.kubernetesconfiguration.models.MessageLevelType;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Assertions;

public final class ComplianceStatusTests {
    @org.junit.jupiter.api.Test
    public void testDeserialize() throws Exception {
        ComplianceStatus model =
            BinaryData
                .fromString(
                    "{\"complianceState\":\"Pending\",\"lastConfigApplied\":\"2021-03-15T08:23:31Z\",\"message\":\"gigr\",\"messageLevel\":\"Error\"}")
                .toObject(ComplianceStatus.class);
        Assertions.assertEquals(OffsetDateTime.parse("2021-03-15T08:23:31Z"), model.lastConfigApplied());
        Assertions.assertEquals("gigr", model.message());
        Assertions.assertEquals(MessageLevelType.ERROR, model.messageLevel());
    }

    @org.junit.jupiter.api.Test
    public void testSerialize() throws Exception {
        ComplianceStatus model =
            new ComplianceStatus()
                .withLastConfigApplied(OffsetDateTime.parse("2021-03-15T08:23:31Z"))
                .withMessage("gigr")
                .withMessageLevel(MessageLevelType.ERROR);
        model = BinaryData.fromObject(model).toObject(ComplianceStatus.class);
        Assertions.assertEquals(OffsetDateTime.parse("2021-03-15T08:23:31Z"), model.lastConfigApplied());
        Assertions.assertEquals("gigr", model.message());
        Assertions.assertEquals(MessageLevelType.ERROR, model.messageLevel());
    }
}