/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 */

package com.microsoft.azure.management.appservice.v2020_09_01;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Azure Files or Blob Storage access information value for dictionary storage.
 */
public class AzureStorageInfoValue {
    /**
     * Type of storage. Possible values include: 'AzureFiles', 'AzureBlob'.
     */
    @JsonProperty(value = "type")
    private AzureStorageType type;

    /**
     * Name of the storage account.
     */
    @JsonProperty(value = "accountName")
    private String accountName;

    /**
     * Name of the file share (container name, for Blob storage).
     */
    @JsonProperty(value = "shareName")
    private String shareName;

    /**
     * Access key for the storage account.
     */
    @JsonProperty(value = "accessKey")
    private String accessKey;

    /**
     * Path to mount the storage within the site's runtime environment.
     */
    @JsonProperty(value = "mountPath")
    private String mountPath;

    /**
     * State of the storage account. Possible values include: 'Ok',
     * 'InvalidCredentials', 'InvalidShare'.
     */
    @JsonProperty(value = "state", access = JsonProperty.Access.WRITE_ONLY)
    private AzureStorageState state;

    /**
     * Get type of storage. Possible values include: 'AzureFiles', 'AzureBlob'.
     *
     * @return the type value
     */
    public AzureStorageType type() {
        return this.type;
    }

    /**
     * Set type of storage. Possible values include: 'AzureFiles', 'AzureBlob'.
     *
     * @param type the type value to set
     * @return the AzureStorageInfoValue object itself.
     */
    public AzureStorageInfoValue withType(AzureStorageType type) {
        this.type = type;
        return this;
    }

    /**
     * Get name of the storage account.
     *
     * @return the accountName value
     */
    public String accountName() {
        return this.accountName;
    }

    /**
     * Set name of the storage account.
     *
     * @param accountName the accountName value to set
     * @return the AzureStorageInfoValue object itself.
     */
    public AzureStorageInfoValue withAccountName(String accountName) {
        this.accountName = accountName;
        return this;
    }

    /**
     * Get name of the file share (container name, for Blob storage).
     *
     * @return the shareName value
     */
    public String shareName() {
        return this.shareName;
    }

    /**
     * Set name of the file share (container name, for Blob storage).
     *
     * @param shareName the shareName value to set
     * @return the AzureStorageInfoValue object itself.
     */
    public AzureStorageInfoValue withShareName(String shareName) {
        this.shareName = shareName;
        return this;
    }

    /**
     * Get access key for the storage account.
     *
     * @return the accessKey value
     */
    public String accessKey() {
        return this.accessKey;
    }

    /**
     * Set access key for the storage account.
     *
     * @param accessKey the accessKey value to set
     * @return the AzureStorageInfoValue object itself.
     */
    public AzureStorageInfoValue withAccessKey(String accessKey) {
        this.accessKey = accessKey;
        return this;
    }

    /**
     * Get path to mount the storage within the site's runtime environment.
     *
     * @return the mountPath value
     */
    public String mountPath() {
        return this.mountPath;
    }

    /**
     * Set path to mount the storage within the site's runtime environment.
     *
     * @param mountPath the mountPath value to set
     * @return the AzureStorageInfoValue object itself.
     */
    public AzureStorageInfoValue withMountPath(String mountPath) {
        this.mountPath = mountPath;
        return this;
    }

    /**
     * Get state of the storage account. Possible values include: 'Ok', 'InvalidCredentials', 'InvalidShare'.
     *
     * @return the state value
     */
    public AzureStorageState state() {
        return this.state;
    }

}
