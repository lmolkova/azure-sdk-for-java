// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

package com.azure.resourcemanager.authorization.fluent.models;

import com.azure.core.util.ExpandableStringEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Collection;

/** Defines values for ContractsContractSelect. */
public final class ContractsContractSelect extends ExpandableStringEnum<ContractsContractSelect> {
    /** Static value id for ContractsContractSelect. */
    public static final ContractsContractSelect ID = fromString("id");

    /** Static value deletedDateTime for ContractsContractSelect. */
    public static final ContractsContractSelect DELETED_DATE_TIME = fromString("deletedDateTime");

    /** Static value contractType for ContractsContractSelect. */
    public static final ContractsContractSelect CONTRACT_TYPE = fromString("contractType");

    /** Static value customerId for ContractsContractSelect. */
    public static final ContractsContractSelect CUSTOMER_ID = fromString("customerId");

    /** Static value defaultDomainName for ContractsContractSelect. */
    public static final ContractsContractSelect DEFAULT_DOMAIN_NAME = fromString("defaultDomainName");

    /** Static value displayName for ContractsContractSelect. */
    public static final ContractsContractSelect DISPLAY_NAME = fromString("displayName");

    /**
     * Creates or finds a ContractsContractSelect from its string representation.
     *
     * @param name a name to look for.
     * @return the corresponding ContractsContractSelect.
     */
    @JsonCreator
    public static ContractsContractSelect fromString(String name) {
        return fromString(name, ContractsContractSelect.class);
    }

    /** @return known ContractsContractSelect values. */
    public static Collection<ContractsContractSelect> values() {
        return values(ContractsContractSelect.class);
    }
}
