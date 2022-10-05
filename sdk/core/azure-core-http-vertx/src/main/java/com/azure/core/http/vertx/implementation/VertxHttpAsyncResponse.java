// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.http.vertx.implementation;

import com.azure.core.http.HttpRequest;
import com.azure.core.util.FluxUtil;
import io.vertx.core.http.HttpClientResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Default HTTP response for Vert.x.
 */
public class VertxHttpAsyncResponse extends VertxHttpResponseBase {

    private final List<String> logs = new ArrayList<>();
    public VertxHttpAsyncResponse(HttpRequest azureHttpRequest, HttpClientResponse vertxHttpResponse) {
        super(azureHttpRequest, vertxHttpResponse);
        logs.add("--------------- ctor! 1" + Instant.now().toEpochMilli());
        vertxHttpResponse.pause();
        logs.add("--------------- ctor! 2 " + Instant.now().toEpochMilli());
    }

    @Override
    public Flux<ByteBuffer> getBody() {
        return streamResponseBody();
    }

    @Override
    public Mono<byte[]> getBodyAsByteArray() {
        return FluxUtil.collectBytesFromNetworkResponse(streamResponseBody(), getHeaders())
            .flatMap(bytes -> (bytes == null || bytes.length == 0)
                ? Mono.empty()
                : Mono.just(bytes));
    }

    private Flux<ByteBuffer> streamResponseBody() {
        System.out.println("--------------- streamResponseBody " + Instant.now().toEpochMilli());
        HttpClientResponse vertxHttpResponse = getVertxHttpResponse();
        return Flux.create(sink -> {
            vertxHttpResponse.handler(buffer -> {
                logs.add("--------------- next " + Instant.now().toEpochMilli() + ", " + buffer.length());
                sink.next(buffer.getByteBuf().nioBuffer());
            }).endHandler(event -> {
                logs.add("--------------- end " + Instant.now().toEpochMilli());
                sink.complete();

                logs.stream().forEach(System.out::println);

            }).exceptionHandler(e ->  {
                logs.add("--------------- ex " + Instant.now().toEpochMilli() + ", " + e.toString());
                sink.error(e);
                logs.stream().forEach(System.out::println);
            });

            vertxHttpResponse.resume();
            logs.add("--------------- resume " + Instant.now().toEpochMilli());
        });
    }
}
