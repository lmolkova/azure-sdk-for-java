// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.spring.cloud.trace.sleuth;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.http.HttpPipelineCallContext;
import com.azure.core.http.HttpPipelineNextPolicy;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.test.http.MockHttpResponse;
import com.azure.core.util.ClientOptions;
import com.azure.core.util.Configuration;
import com.azure.core.util.HttpClientOptions;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.spring.cloud.core.provider.ClientOptionsProvider;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.CustomerProvidedKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SleuthHttpPolicyTests {

    @Mock
    private Tracer tracer;

    @Mock
    private HttpPipelineCallContext httpPipelineCallContext;

    @Mock
    private HttpPipelineNextPolicy httpPipelineNextPolicy;

    private MockHttpResponse successResponse = spy(new MockHttpResponse(mock(HttpRequest.class), 200));

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void cleanup() throws Exception {
        MockitoAnnotations.openMocks(this).close();
    }

    @Test
    public void addPolicyForBlobServiceClientBuilder() {
        Configuration.getGlobalConfiguration().put("TRACER_PROVIDER_CLASSNAME", SleuthTracerProvider.class.getName());
        ClientOptions options = new HttpClientOptions().setTracingOptions(
            new SleuthTracingOptions().setTracer(tracer));
        // key is test-key
        CustomerProvidedKey providedKey = new CustomerProvidedKey("dGVzdC1rZXk=");
        TokenCredential tokenCredential = new ClientSecretCredentialBuilder()
            .clientSecret("dummy-secret")
            .clientId("dummy-client-id")
            .tenantId("dummy-tenant-id")
            .build();
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
            .customerProvidedKey(providedKey)
            .credential(tokenCredential)
            .clientOptions(options)
            .endpoint("https://test.blob.core.windows.net/")
            .buildClient();

        HttpPipeline pipeline = blobServiceClient.getHttpPipeline();
        assertTrue(pipeline.getTracer() instanceof SleuthTracer);

        boolean foundInstrumentationPolicy = false;
        for (int i = 0; i < pipeline.getPolicyCount(); i ++) {
            foundInstrumentationPolicy |= pipeline.getPolicy(i).getClass().getName().contains("InstrumentationPolicy");
        }
        assertTrue(foundInstrumentationPolicy);
    }

    // implement tests with  micrometer-tracing-test
}
