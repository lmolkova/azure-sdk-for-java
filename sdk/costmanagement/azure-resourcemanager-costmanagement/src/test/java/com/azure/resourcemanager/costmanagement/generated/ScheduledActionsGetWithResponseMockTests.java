// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

package com.azure.resourcemanager.costmanagement.generated;

import com.azure.core.credential.AccessToken;
import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.costmanagement.CostManagementManager;
import com.azure.resourcemanager.costmanagement.models.ScheduleFrequency;
import com.azure.resourcemanager.costmanagement.models.ScheduledAction;
import com.azure.resourcemanager.costmanagement.models.ScheduledActionKind;
import com.azure.resourcemanager.costmanagement.models.ScheduledActionStatus;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class ScheduledActionsGetWithResponseMockTests {
    @Test
    public void testGetWithResponse() throws Exception {
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        ArgumentCaptor<HttpRequest> httpRequest = ArgumentCaptor.forClass(HttpRequest.class);

        String responseStr =
            "{\"properties\":{\"displayName\":\"mfxapjwogqqno\",\"fileDestination\":{\"fileFormats\":[]},\"notification\":{\"to\":[],\"language\":\"cdabtqwpwya\",\"message\":\"zasqbucljgkyexao\",\"regionalFormat\":\"yaipidsda\",\"subject\":\"ltxijjumfqwazln\"},\"notificationEmail\":\"mcjn\",\"schedule\":{\"frequency\":\"Weekly\",\"hourOfDay\":706743032,\"daysOfWeek\":[],\"weeksOfMonth\":[],\"dayOfMonth\":260855518,\"startDate\":\"2021-06-21T10:55:23Z\",\"endDate\":\"2021-08-28T15:42:49Z\"},\"scope\":\"nyfusfzsvtuikzh\",\"status\":\"Expired\",\"viewId\":\"qglcfhmlrqryxynq\"},\"eTag\":\"rd\",\"kind\":\"Email\",\"id\":\"wxznptgoei\",\"name\":\"bbabp\",\"type\":\"hv\"}";

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

        CostManagementManager manager =
            CostManagementManager
                .configure()
                .withHttpClient(httpClient)
                .authenticate(
                    tokenRequestContext -> Mono.just(new AccessToken("this_is_a_token", OffsetDateTime.MAX)),
                    new AzureProfile("", "", AzureEnvironment.AZURE));

        ScheduledAction response =
            manager.scheduledActions().getWithResponse("ex", com.azure.core.util.Context.NONE).getValue();

        Assertions.assertEquals(ScheduledActionKind.EMAIL, response.kind());
        Assertions.assertEquals("mfxapjwogqqno", response.displayName());
        Assertions.assertEquals("cdabtqwpwya", response.notification().language());
        Assertions.assertEquals("zasqbucljgkyexao", response.notification().message());
        Assertions.assertEquals("yaipidsda", response.notification().regionalFormat());
        Assertions.assertEquals("ltxijjumfqwazln", response.notification().subject());
        Assertions.assertEquals("mcjn", response.notificationEmail());
        Assertions.assertEquals(ScheduleFrequency.WEEKLY, response.schedule().frequency());
        Assertions.assertEquals(706743032, response.schedule().hourOfDay());
        Assertions.assertEquals(260855518, response.schedule().dayOfMonth());
        Assertions.assertEquals(OffsetDateTime.parse("2021-06-21T10:55:23Z"), response.schedule().startDate());
        Assertions.assertEquals(OffsetDateTime.parse("2021-08-28T15:42:49Z"), response.schedule().endDate());
        Assertions.assertEquals("nyfusfzsvtuikzh", response.scope());
        Assertions.assertEquals(ScheduledActionStatus.EXPIRED, response.status());
        Assertions.assertEquals("qglcfhmlrqryxynq", response.viewId());
    }
}