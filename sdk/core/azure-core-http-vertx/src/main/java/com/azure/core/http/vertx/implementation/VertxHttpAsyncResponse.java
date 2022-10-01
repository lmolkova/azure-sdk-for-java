// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.http.vertx.implementation;

import com.azure.core.http.HttpRequest;
import com.azure.core.util.FluxUtil;
import io.vertx.core.http.HttpClientResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.nio.ByteBuffer;

/**
 * Default HTTP response for Vert.x.
 */
public class VertxHttpAsyncResponse extends VertxHttpResponseBase {

    private final Sinks.Many<ByteBuffer> body = Sinks.many().multicast().onBackpressureBuffer(100500);
    public VertxHttpAsyncResponse(HttpRequest azureHttpRequest, HttpClientResponse vertxHttpResponse) {
        super(azureHttpRequest, vertxHttpResponse);
        vertxHttpResponse.pause();
        vertxHttpResponse
            .handler(buffer -> {
                body.emitNext(buffer.getByteBuf().nioBuffer(), Sinks.EmitFailureHandler.FAIL_FAST);
            })
            .endHandler(e -> body.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST))
            .exceptionHandler(e -> body.emitError(e, Sinks.EmitFailureHandler.FAIL_FAST));
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
        getVertxHttpResponse().resume();
        return body.asFlux();
    }
}
