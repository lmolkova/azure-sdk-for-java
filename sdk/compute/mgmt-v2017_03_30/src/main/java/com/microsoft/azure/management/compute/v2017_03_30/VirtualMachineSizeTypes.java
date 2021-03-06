/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 */

package com.microsoft.azure.management.compute.v2017_03_30;

import java.util.Collection;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.microsoft.rest.ExpandableStringEnum;

/**
 * Defines values for VirtualMachineSizeTypes.
 */
public final class VirtualMachineSizeTypes extends ExpandableStringEnum<VirtualMachineSizeTypes> {
    /** Static value Basic_A0 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes BASIC_A0 = fromString("Basic_A0");

    /** Static value Basic_A1 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes BASIC_A1 = fromString("Basic_A1");

    /** Static value Basic_A2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes BASIC_A2 = fromString("Basic_A2");

    /** Static value Basic_A3 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes BASIC_A3 = fromString("Basic_A3");

    /** Static value Basic_A4 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes BASIC_A4 = fromString("Basic_A4");

    /** Static value Standard_A0 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_A0 = fromString("Standard_A0");

    /** Static value Standard_A1 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_A1 = fromString("Standard_A1");

    /** Static value Standard_A2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_A2 = fromString("Standard_A2");

    /** Static value Standard_A3 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_A3 = fromString("Standard_A3");

    /** Static value Standard_A4 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_A4 = fromString("Standard_A4");

    /** Static value Standard_A5 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_A5 = fromString("Standard_A5");

    /** Static value Standard_A6 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_A6 = fromString("Standard_A6");

    /** Static value Standard_A7 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_A7 = fromString("Standard_A7");

    /** Static value Standard_A8 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_A8 = fromString("Standard_A8");

    /** Static value Standard_A9 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_A9 = fromString("Standard_A9");

    /** Static value Standard_A10 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_A10 = fromString("Standard_A10");

    /** Static value Standard_A11 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_A11 = fromString("Standard_A11");

    /** Static value Standard_A1_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_A1_V2 = fromString("Standard_A1_v2");

    /** Static value Standard_A2_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_A2_V2 = fromString("Standard_A2_v2");

    /** Static value Standard_A4_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_A4_V2 = fromString("Standard_A4_v2");

    /** Static value Standard_A8_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_A8_V2 = fromString("Standard_A8_v2");

    /** Static value Standard_A2m_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_A2M_V2 = fromString("Standard_A2m_v2");

    /** Static value Standard_A4m_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_A4M_V2 = fromString("Standard_A4m_v2");

    /** Static value Standard_A8m_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_A8M_V2 = fromString("Standard_A8m_v2");

    /** Static value Standard_D1 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_D1 = fromString("Standard_D1");

    /** Static value Standard_D2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_D2 = fromString("Standard_D2");

    /** Static value Standard_D3 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_D3 = fromString("Standard_D3");

    /** Static value Standard_D4 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_D4 = fromString("Standard_D4");

    /** Static value Standard_D11 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_D11 = fromString("Standard_D11");

    /** Static value Standard_D12 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_D12 = fromString("Standard_D12");

    /** Static value Standard_D13 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_D13 = fromString("Standard_D13");

    /** Static value Standard_D14 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_D14 = fromString("Standard_D14");

    /** Static value Standard_D1_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_D1_V2 = fromString("Standard_D1_v2");

    /** Static value Standard_D2_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_D2_V2 = fromString("Standard_D2_v2");

    /** Static value Standard_D3_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_D3_V2 = fromString("Standard_D3_v2");

    /** Static value Standard_D4_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_D4_V2 = fromString("Standard_D4_v2");

    /** Static value Standard_D5_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_D5_V2 = fromString("Standard_D5_v2");

    /** Static value Standard_D11_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_D11_V2 = fromString("Standard_D11_v2");

    /** Static value Standard_D12_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_D12_V2 = fromString("Standard_D12_v2");

    /** Static value Standard_D13_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_D13_V2 = fromString("Standard_D13_v2");

    /** Static value Standard_D14_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_D14_V2 = fromString("Standard_D14_v2");

    /** Static value Standard_D15_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_D15_V2 = fromString("Standard_D15_v2");

    /** Static value Standard_DS1 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_DS1 = fromString("Standard_DS1");

    /** Static value Standard_DS2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_DS2 = fromString("Standard_DS2");

    /** Static value Standard_DS3 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_DS3 = fromString("Standard_DS3");

    /** Static value Standard_DS4 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_DS4 = fromString("Standard_DS4");

    /** Static value Standard_DS11 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_DS11 = fromString("Standard_DS11");

    /** Static value Standard_DS12 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_DS12 = fromString("Standard_DS12");

    /** Static value Standard_DS13 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_DS13 = fromString("Standard_DS13");

    /** Static value Standard_DS14 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_DS14 = fromString("Standard_DS14");

    /** Static value Standard_DS1_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_DS1_V2 = fromString("Standard_DS1_v2");

    /** Static value Standard_DS2_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_DS2_V2 = fromString("Standard_DS2_v2");

    /** Static value Standard_DS3_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_DS3_V2 = fromString("Standard_DS3_v2");

    /** Static value Standard_DS4_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_DS4_V2 = fromString("Standard_DS4_v2");

    /** Static value Standard_DS5_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_DS5_V2 = fromString("Standard_DS5_v2");

    /** Static value Standard_DS11_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_DS11_V2 = fromString("Standard_DS11_v2");

    /** Static value Standard_DS12_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_DS12_V2 = fromString("Standard_DS12_v2");

    /** Static value Standard_DS13_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_DS13_V2 = fromString("Standard_DS13_v2");

    /** Static value Standard_DS14_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_DS14_V2 = fromString("Standard_DS14_v2");

    /** Static value Standard_DS15_v2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_DS15_V2 = fromString("Standard_DS15_v2");

    /** Static value Standard_F1 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_F1 = fromString("Standard_F1");

    /** Static value Standard_F2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_F2 = fromString("Standard_F2");

    /** Static value Standard_F4 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_F4 = fromString("Standard_F4");

    /** Static value Standard_F8 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_F8 = fromString("Standard_F8");

    /** Static value Standard_F16 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_F16 = fromString("Standard_F16");

    /** Static value Standard_F1s for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_F1S = fromString("Standard_F1s");

    /** Static value Standard_F2s for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_F2S = fromString("Standard_F2s");

    /** Static value Standard_F4s for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_F4S = fromString("Standard_F4s");

    /** Static value Standard_F8s for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_F8S = fromString("Standard_F8s");

    /** Static value Standard_F16s for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_F16S = fromString("Standard_F16s");

    /** Static value Standard_G1 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_G1 = fromString("Standard_G1");

    /** Static value Standard_G2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_G2 = fromString("Standard_G2");

    /** Static value Standard_G3 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_G3 = fromString("Standard_G3");

    /** Static value Standard_G4 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_G4 = fromString("Standard_G4");

    /** Static value Standard_G5 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_G5 = fromString("Standard_G5");

    /** Static value Standard_GS1 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_GS1 = fromString("Standard_GS1");

    /** Static value Standard_GS2 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_GS2 = fromString("Standard_GS2");

    /** Static value Standard_GS3 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_GS3 = fromString("Standard_GS3");

    /** Static value Standard_GS4 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_GS4 = fromString("Standard_GS4");

    /** Static value Standard_GS5 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_GS5 = fromString("Standard_GS5");

    /** Static value Standard_H8 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_H8 = fromString("Standard_H8");

    /** Static value Standard_H16 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_H16 = fromString("Standard_H16");

    /** Static value Standard_H8m for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_H8M = fromString("Standard_H8m");

    /** Static value Standard_H16m for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_H16M = fromString("Standard_H16m");

    /** Static value Standard_H16r for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_H16R = fromString("Standard_H16r");

    /** Static value Standard_H16mr for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_H16MR = fromString("Standard_H16mr");

    /** Static value Standard_L4s for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_L4S = fromString("Standard_L4s");

    /** Static value Standard_L8s for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_L8S = fromString("Standard_L8s");

    /** Static value Standard_L16s for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_L16S = fromString("Standard_L16s");

    /** Static value Standard_L32s for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_L32S = fromString("Standard_L32s");

    /** Static value Standard_NC6 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_NC6 = fromString("Standard_NC6");

    /** Static value Standard_NC12 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_NC12 = fromString("Standard_NC12");

    /** Static value Standard_NC24 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_NC24 = fromString("Standard_NC24");

    /** Static value Standard_NC24r for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_NC24R = fromString("Standard_NC24r");

    /** Static value Standard_NV6 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_NV6 = fromString("Standard_NV6");

    /** Static value Standard_NV12 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_NV12 = fromString("Standard_NV12");

    /** Static value Standard_NV24 for VirtualMachineSizeTypes. */
    public static final VirtualMachineSizeTypes STANDARD_NV24 = fromString("Standard_NV24");

    /**
     * Creates or finds a VirtualMachineSizeTypes from its string representation.
     * @param name a name to look for
     * @return the corresponding VirtualMachineSizeTypes
     */
    @JsonCreator
    public static VirtualMachineSizeTypes fromString(String name) {
        return fromString(name, VirtualMachineSizeTypes.class);
    }

    /**
     * @return known VirtualMachineSizeTypes values
     */
    public static Collection<VirtualMachineSizeTypes> values() {
        return values(VirtualMachineSizeTypes.class);
    }
}
