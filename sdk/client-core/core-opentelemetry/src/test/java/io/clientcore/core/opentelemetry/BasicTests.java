// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.clientcore.core.opentelemetry;

import io.clientcore.core.annotation.ServiceInterface;
import io.clientcore.core.http.RestProxy;
import io.clientcore.core.http.annotation.HostParam;
import io.clientcore.core.http.annotation.HttpRequestInformation;
import io.clientcore.core.http.models.HttpMethod;
import io.clientcore.core.http.pipeline.HttpPipeline;
import io.clientcore.core.http.pipeline.HttpPipelineBuilder;
import io.clientcore.core.shared.LocalTestServer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import javax.servlet.ServletException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.SAME_THREAD)
public class BasicTests {
    private static final String SERVICE_ENDPOINT = "/default";

    private InMemorySpanExporter exporter;
    private SdkTracerProvider tracerProvider;
    private OpenTelemetry openTelemetry;

    private static LocalTestServer server;
    @BeforeAll
    public static void startTestServer() {
        server = new LocalTestServer((req, resp, requestBody) -> {
            if ("GET".equalsIgnoreCase(req.getMethod()) && SERVICE_ENDPOINT.equals(req.getServletPath())) {
                resp.setStatus(200);
            } else {
                throw new ServletException("Unexpected request: " + req.getMethod() + " " + req.getServletPath());
            }
        });
        server.start();
    }

    @AfterAll
    public static void stopTestServer() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void test() {
        exporter = InMemorySpanExporter.create();
        tracerProvider = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(exporter)).build();

        openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();

        createService(Service1.class, new OpenTelemetryInstrumentationOptions()
                .setOpenTelemetry(openTelemetry))
                .getByteArray("https://httpbin.org");

        assertEquals(1, exporter.getFinishedSpanItems().size());
    }

    protected <T> T createService(Class<T> serviceClass, OpenTelemetryInstrumentationOptions options) {
        HttpPipeline pipeline = new HttpPipelineBuilder()
                .instrumentationOptions(options)
                .build();

        return RestProxy.create(serviceClass, pipeline, null);
    }

    @ServiceInterface(name = "Service1", host = "{url}")
    private interface Service1 {
        @HttpRequestInformation(method = HttpMethod.GET, path = "bytes/100", expectedStatusCodes = {200})
        byte[] getByteArray(@HostParam("url") String url);
    }
}
