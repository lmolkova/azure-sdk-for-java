package com.azure.storage.blob.stress.scenarios;

import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;
import com.azure.core.util.logging.ClientLogger;
import com.azure.storage.blob.options.BlobDownloadToFileOptions;
import com.azure.storage.blob.stress.builders.BlobScenarioBuilder;
import com.azure.storage.blob.stress.builders.DownloadToFileScenarioBuilder;
import com.azure.storage.blob.stress.builders.HucTls13StressScenarioBuilder;
import com.azure.storage.blob.stress.scenarios.infra.BlobStressScenario;
import com.azure.storage.stress.RandomInputStream;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;

public class HucTls13StressScenario extends BlobStressScenario<HucTls13StressScenarioBuilder> {

    private static final ClientLogger LOGGER = new ClientLogger(HucTls13StressScenario.class);
    private static final String BLOB_URL_WITH_SAS_TOKEN = ""; // Fill this out.
    private static final Tracer TRACER = GlobalOpenTelemetry.getTracer("HucTls13");

    private final URL blobDownloadUrl;

    public HucTls13StressScenario(HucTls13StressScenarioBuilder builder) {
        super(builder, /*singletonBlob*/true, /*initializeBlob*/true);
        try {
            blobDownloadUrl = new URL(BLOB_URL_WITH_SAS_TOKEN);
        } catch (MalformedURLException e) {
            throw LOGGER.logExceptionAsError(new RuntimeException(e));
        }
    }

    @Override
    public void run(Duration timeout) {
        long endTimeNano = System.nanoTime() + timeout.toNanos();
        long iterations = 0;
        while (endTimeNano - System.nanoTime() > 0) {
            LOGGER.atInfo().addKeyValue("iteration", iterations).log("starting...");
            Span span = TRACER.spanBuilder("download").startSpan();
            Scope scope = span.makeCurrent();
            try {
                HttpURLConnection connection = (HttpURLConnection) blobDownloadUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(60 * 1000); // 60 second connection timeout.
                connection.setReadTimeout(5 * 60 * 1000); // 5 minute read timeout.
                connection.setRequestProperty("x-ms-client-request-id", span.getSpanContext().getTraceId());
                connection.connect();

                // Read the response.
                int responseCode = connection.getResponseCode();
                Map<String, List<String>> headers = connection.getHeaderFields();
                span.setAttribute(AttributeKey.stringKey("x-ms-request-id"), headers.get("x-ms-request-id").get(0));
                int len = Integer.parseInt(headers.get("Content-Length").get(0));
                byte[] data = connection.getInputStream().readNBytes(len);
            } catch (Throwable ex) {
                LOGGER.atError().addKeyValue("iteration", iterations).log("error", ex);
                span.setStatus(StatusCode.ERROR);
            } finally {
                scope.close();
                span.end();
                LOGGER.atInfo().addKeyValue("iteration", iterations).log("done");
                iterations ++;
            }
        }

        LOGGER.atInfo().addKeyValue("iteration", iterations).log("done.");
    }

    @Override
    public Mono<Void> runAsync() {
        return Mono.error(new UnsupportedOperationException("Not implemented yet"));
    }
}
