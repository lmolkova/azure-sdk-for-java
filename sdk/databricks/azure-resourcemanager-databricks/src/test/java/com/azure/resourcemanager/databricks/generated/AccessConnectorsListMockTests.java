// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

package com.azure.resourcemanager.databricks.generated;

import com.azure.core.credential.AccessToken;
import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.databricks.AzureDatabricksManager;
import com.azure.resourcemanager.databricks.models.AccessConnector;
import com.azure.resourcemanager.databricks.models.ManagedServiceIdentityType;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class AccessConnectorsListMockTests {
    @Test
    public void testList() throws Exception {
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        ArgumentCaptor<HttpRequest> httpRequest = ArgumentCaptor.forClass(HttpRequest.class);

        String responseStr =
            "{\"value\":[{\"identity\":{\"principalId\":\"0b6e54aa-aa52-4836-ac81-01efe6d11758\",\"tenantId\":\"d4b0e95f-10eb-45df-9d18-d4503b2b5f2e\",\"type\":\"None\",\"userAssignedIdentities\":{}},\"properties\":{\"provisioningState\":\"Failed\"},\"location\":\"xllrxcyjm\",\"tags\":{\"arm\":\"su\",\"rw\":\"wdmjsjqbjhhyx\",\"duhpk\":\"yc\",\"hky\":\"kgymareqnajxqug\"},\"id\":\"ubeddg\",\"name\":\"sofwqmzqalkrmnji\",\"type\":\"pxacqqudfn\"}]}";

        Mockito.when(httpResponse.getStatusCode()).thenReturn(200);
        Mockito.when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
        Mockito
            .when(httpResponse.getBody())
            .thenReturn(Flux.just(ByteBuffer.wrap(responseStr.getBytes(StandardCharsets.UTF_8))));
        Mockito
            .when(httpResponse.getBodyAsByteArray())
            .thenReturn(Mono.just(responseStr.getBytes(StandardCharsets.UTF_8)));
        Mockito
            .when(httpClient.send(httpRequest.capture(), Mockito.any()))
            .thenReturn(
                Mono
                    .defer(
                        () -> {
                            Mockito.when(httpResponse.getRequest()).thenReturn(httpRequest.getValue());
                            return Mono.just(httpResponse);
                        }));

        AzureDatabricksManager manager =
            AzureDatabricksManager
                .configure()
                .withHttpClient(httpClient)
                .authenticate(
                    tokenRequestContext -> Mono.just(new AccessToken("this_is_a_token", OffsetDateTime.MAX)),
                    new AzureProfile("", "", AzureEnvironment.AZURE));

        PagedIterable<AccessConnector> response = manager.accessConnectors().list(com.azure.core.util.Context.NONE);

        Assertions.assertEquals("xllrxcyjm", response.iterator().next().location());
        Assertions.assertEquals("su", response.iterator().next().tags().get("arm"));
        Assertions.assertEquals(ManagedServiceIdentityType.NONE, response.iterator().next().identity().type());
    }
}