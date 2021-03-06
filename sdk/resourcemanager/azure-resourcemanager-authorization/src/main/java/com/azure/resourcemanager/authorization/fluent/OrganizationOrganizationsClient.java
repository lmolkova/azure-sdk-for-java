// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

package com.azure.resourcemanager.authorization.fluent;

import com.azure.core.annotation.ReturnType;
import com.azure.core.annotation.ServiceMethod;
import com.azure.core.http.rest.PagedFlux;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import com.azure.resourcemanager.authorization.fluent.models.MicrosoftGraphOrganizationInner;
import com.azure.resourcemanager.authorization.fluent.models.OrganizationOrganizationExpand;
import com.azure.resourcemanager.authorization.fluent.models.OrganizationOrganizationOrderby;
import com.azure.resourcemanager.authorization.fluent.models.OrganizationOrganizationSelect;
import java.util.List;
import reactor.core.publisher.Mono;

/** An instance of this class provides access to all the operations defined in OrganizationOrganizationsClient. */
public interface OrganizationOrganizationsClient {
    /**
     * Get entities from organization.
     *
     * @param top Show only the first n items.
     * @param skip Skip the first n items.
     * @param search Search items by search phrases.
     * @param filter Filter items by property values.
     * @param count Include count of items.
     * @param orderby Order items by property values.
     * @param select Select properties to be returned.
     * @param expand Expand related entities.
     * @throws IllegalArgumentException thrown if parameters fail the validation.
     * @throws com.azure.resourcemanager.authorization.fluent.models.OdataErrorMainException thrown if the request is
     *     rejected by server.
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
     * @return entities from organization.
     */
    @ServiceMethod(returns = ReturnType.COLLECTION)
    PagedFlux<MicrosoftGraphOrganizationInner> listOrganizationAsync(
        Integer top,
        Integer skip,
        String search,
        String filter,
        Boolean count,
        List<OrganizationOrganizationOrderby> orderby,
        List<OrganizationOrganizationSelect> select,
        List<OrganizationOrganizationExpand> expand);

    /**
     * Get entities from organization.
     *
     * @throws com.azure.resourcemanager.authorization.fluent.models.OdataErrorMainException thrown if the request is
     *     rejected by server.
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
     * @return entities from organization.
     */
    @ServiceMethod(returns = ReturnType.COLLECTION)
    PagedFlux<MicrosoftGraphOrganizationInner> listOrganizationAsync();

    /**
     * Get entities from organization.
     *
     * @throws com.azure.resourcemanager.authorization.fluent.models.OdataErrorMainException thrown if the request is
     *     rejected by server.
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
     * @return entities from organization.
     */
    @ServiceMethod(returns = ReturnType.COLLECTION)
    PagedIterable<MicrosoftGraphOrganizationInner> listOrganization();

    /**
     * Get entities from organization.
     *
     * @param top Show only the first n items.
     * @param skip Skip the first n items.
     * @param search Search items by search phrases.
     * @param filter Filter items by property values.
     * @param count Include count of items.
     * @param orderby Order items by property values.
     * @param select Select properties to be returned.
     * @param expand Expand related entities.
     * @param context The context to associate with this operation.
     * @throws IllegalArgumentException thrown if parameters fail the validation.
     * @throws com.azure.resourcemanager.authorization.fluent.models.OdataErrorMainException thrown if the request is
     *     rejected by server.
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
     * @return entities from organization.
     */
    @ServiceMethod(returns = ReturnType.COLLECTION)
    PagedIterable<MicrosoftGraphOrganizationInner> listOrganization(
        Integer top,
        Integer skip,
        String search,
        String filter,
        Boolean count,
        List<OrganizationOrganizationOrderby> orderby,
        List<OrganizationOrganizationSelect> select,
        List<OrganizationOrganizationExpand> expand,
        Context context);

    /**
     * Add new entity to organization.
     *
     * @param body New entity.
     * @throws IllegalArgumentException thrown if parameters fail the validation.
     * @throws com.azure.resourcemanager.authorization.fluent.models.OdataErrorMainException thrown if the request is
     *     rejected by server.
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
     * @return the organization resource represents an instance of global settings and resources which operate and are
     *     provisioned at the tenant-level.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    Mono<Response<MicrosoftGraphOrganizationInner>> createOrganizationWithResponseAsync(
        MicrosoftGraphOrganizationInner body);

    /**
     * Add new entity to organization.
     *
     * @param body New entity.
     * @throws IllegalArgumentException thrown if parameters fail the validation.
     * @throws com.azure.resourcemanager.authorization.fluent.models.OdataErrorMainException thrown if the request is
     *     rejected by server.
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
     * @return the organization resource represents an instance of global settings and resources which operate and are
     *     provisioned at the tenant-level.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    Mono<MicrosoftGraphOrganizationInner> createOrganizationAsync(MicrosoftGraphOrganizationInner body);

    /**
     * Add new entity to organization.
     *
     * @param body New entity.
     * @throws IllegalArgumentException thrown if parameters fail the validation.
     * @throws com.azure.resourcemanager.authorization.fluent.models.OdataErrorMainException thrown if the request is
     *     rejected by server.
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
     * @return the organization resource represents an instance of global settings and resources which operate and are
     *     provisioned at the tenant-level.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    MicrosoftGraphOrganizationInner createOrganization(MicrosoftGraphOrganizationInner body);

    /**
     * Add new entity to organization.
     *
     * @param body New entity.
     * @param context The context to associate with this operation.
     * @throws IllegalArgumentException thrown if parameters fail the validation.
     * @throws com.azure.resourcemanager.authorization.fluent.models.OdataErrorMainException thrown if the request is
     *     rejected by server.
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
     * @return the organization resource represents an instance of global settings and resources which operate and are
     *     provisioned at the tenant-level.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    Response<MicrosoftGraphOrganizationInner> createOrganizationWithResponse(
        MicrosoftGraphOrganizationInner body, Context context);

    /**
     * Get entity from organization by key.
     *
     * @param organizationId key: id of organization.
     * @param select Select properties to be returned.
     * @param expand Expand related entities.
     * @throws IllegalArgumentException thrown if parameters fail the validation.
     * @throws com.azure.resourcemanager.authorization.fluent.models.OdataErrorMainException thrown if the request is
     *     rejected by server.
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
     * @return entity from organization by key.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    Mono<Response<MicrosoftGraphOrganizationInner>> getOrganizationWithResponseAsync(
        String organizationId,
        List<OrganizationOrganizationSelect> select,
        List<OrganizationOrganizationExpand> expand);

    /**
     * Get entity from organization by key.
     *
     * @param organizationId key: id of organization.
     * @param select Select properties to be returned.
     * @param expand Expand related entities.
     * @throws IllegalArgumentException thrown if parameters fail the validation.
     * @throws com.azure.resourcemanager.authorization.fluent.models.OdataErrorMainException thrown if the request is
     *     rejected by server.
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
     * @return entity from organization by key.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    Mono<MicrosoftGraphOrganizationInner> getOrganizationAsync(
        String organizationId,
        List<OrganizationOrganizationSelect> select,
        List<OrganizationOrganizationExpand> expand);

    /**
     * Get entity from organization by key.
     *
     * @param organizationId key: id of organization.
     * @throws IllegalArgumentException thrown if parameters fail the validation.
     * @throws com.azure.resourcemanager.authorization.fluent.models.OdataErrorMainException thrown if the request is
     *     rejected by server.
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
     * @return entity from organization by key.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    Mono<MicrosoftGraphOrganizationInner> getOrganizationAsync(String organizationId);

    /**
     * Get entity from organization by key.
     *
     * @param organizationId key: id of organization.
     * @throws IllegalArgumentException thrown if parameters fail the validation.
     * @throws com.azure.resourcemanager.authorization.fluent.models.OdataErrorMainException thrown if the request is
     *     rejected by server.
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
     * @return entity from organization by key.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    MicrosoftGraphOrganizationInner getOrganization(String organizationId);

    /**
     * Get entity from organization by key.
     *
     * @param organizationId key: id of organization.
     * @param select Select properties to be returned.
     * @param expand Expand related entities.
     * @param context The context to associate with this operation.
     * @throws IllegalArgumentException thrown if parameters fail the validation.
     * @throws com.azure.resourcemanager.authorization.fluent.models.OdataErrorMainException thrown if the request is
     *     rejected by server.
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
     * @return entity from organization by key.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    Response<MicrosoftGraphOrganizationInner> getOrganizationWithResponse(
        String organizationId,
        List<OrganizationOrganizationSelect> select,
        List<OrganizationOrganizationExpand> expand,
        Context context);

    /**
     * Update entity in organization.
     *
     * @param organizationId key: id of organization.
     * @param body New property values.
     * @throws IllegalArgumentException thrown if parameters fail the validation.
     * @throws com.azure.resourcemanager.authorization.fluent.models.OdataErrorMainException thrown if the request is
     *     rejected by server.
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
     * @return the completion.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    Mono<Response<Void>> updateOrganizationWithResponseAsync(
        String organizationId, MicrosoftGraphOrganizationInner body);

    /**
     * Update entity in organization.
     *
     * @param organizationId key: id of organization.
     * @param body New property values.
     * @throws IllegalArgumentException thrown if parameters fail the validation.
     * @throws com.azure.resourcemanager.authorization.fluent.models.OdataErrorMainException thrown if the request is
     *     rejected by server.
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
     * @return the completion.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    Mono<Void> updateOrganizationAsync(String organizationId, MicrosoftGraphOrganizationInner body);

    /**
     * Update entity in organization.
     *
     * @param organizationId key: id of organization.
     * @param body New property values.
     * @throws IllegalArgumentException thrown if parameters fail the validation.
     * @throws com.azure.resourcemanager.authorization.fluent.models.OdataErrorMainException thrown if the request is
     *     rejected by server.
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    void updateOrganization(String organizationId, MicrosoftGraphOrganizationInner body);

    /**
     * Update entity in organization.
     *
     * @param organizationId key: id of organization.
     * @param body New property values.
     * @param context The context to associate with this operation.
     * @throws IllegalArgumentException thrown if parameters fail the validation.
     * @throws com.azure.resourcemanager.authorization.fluent.models.OdataErrorMainException thrown if the request is
     *     rejected by server.
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
     * @return the response.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    Response<Void> updateOrganizationWithResponse(
        String organizationId, MicrosoftGraphOrganizationInner body, Context context);

    /**
     * Delete entity from organization.
     *
     * @param organizationId key: id of organization.
     * @param ifMatch ETag.
     * @throws IllegalArgumentException thrown if parameters fail the validation.
     * @throws com.azure.resourcemanager.authorization.fluent.models.OdataErrorMainException thrown if the request is
     *     rejected by server.
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
     * @return the completion.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    Mono<Response<Void>> deleteOrganizationWithResponseAsync(String organizationId, String ifMatch);

    /**
     * Delete entity from organization.
     *
     * @param organizationId key: id of organization.
     * @param ifMatch ETag.
     * @throws IllegalArgumentException thrown if parameters fail the validation.
     * @throws com.azure.resourcemanager.authorization.fluent.models.OdataErrorMainException thrown if the request is
     *     rejected by server.
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
     * @return the completion.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    Mono<Void> deleteOrganizationAsync(String organizationId, String ifMatch);

    /**
     * Delete entity from organization.
     *
     * @param organizationId key: id of organization.
     * @throws IllegalArgumentException thrown if parameters fail the validation.
     * @throws com.azure.resourcemanager.authorization.fluent.models.OdataErrorMainException thrown if the request is
     *     rejected by server.
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
     * @return the completion.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    Mono<Void> deleteOrganizationAsync(String organizationId);

    /**
     * Delete entity from organization.
     *
     * @param organizationId key: id of organization.
     * @throws IllegalArgumentException thrown if parameters fail the validation.
     * @throws com.azure.resourcemanager.authorization.fluent.models.OdataErrorMainException thrown if the request is
     *     rejected by server.
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    void deleteOrganization(String organizationId);

    /**
     * Delete entity from organization.
     *
     * @param organizationId key: id of organization.
     * @param ifMatch ETag.
     * @param context The context to associate with this operation.
     * @throws IllegalArgumentException thrown if parameters fail the validation.
     * @throws com.azure.resourcemanager.authorization.fluent.models.OdataErrorMainException thrown if the request is
     *     rejected by server.
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
     * @return the response.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    Response<Void> deleteOrganizationWithResponse(String organizationId, String ifMatch, Context context);
}
