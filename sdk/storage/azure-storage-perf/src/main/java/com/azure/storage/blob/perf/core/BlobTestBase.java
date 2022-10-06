// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.storage.blob.perf.core;

import com.azure.security.keyvault.keys.cryptography.models.KeyWrapAlgorithm;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.perf.BlobPerfStressOptions;
import com.azure.storage.blob.specialized.BlockBlobAsyncClient;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.azure.storage.blob.specialized.cryptography.EncryptedBlobClientBuilder;
import com.azure.storage.blob.specialized.cryptography.EncryptionVersion;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;

import java.util.Random;

public abstract class BlobTestBase<TOptions extends BlobPerfStressOptions> extends ContainerTest<TOptions> {

    public static final int DEFAULT_BUFFER_SIZE = 8192;
    protected static  final String BLOB_NAME_PREFIX = "randomblobtest-";
    protected final BlobClient blobClient;
    protected final BlockBlobClient blockBlobClient;
    protected final BlobAsyncClient blobAsyncClient;
    protected final BlockBlobAsyncClient blockBlobAsyncClient;
    private static final FakeKey fakeKeyEncryptionKey;

    static {
        //SdkTracerProvider provider = SdkTracerProvider.builder()
            //.setSampler(Sampler.traceIdRatioBased(0.01))
        //    .addSpanProcessor(new NoopProcessor())
        //    .build();

        //OpenTelemetrySdk.builder().setTracerProvider(provider).buildAndRegisterGlobal();


        Random rand = new Random(System.currentTimeMillis());
        byte[] data = new byte[256];
        rand.nextBytes(data);
        fakeKeyEncryptionKey = new FakeKey("keyId", data);
    }

    public BlobTestBase(TOptions options, String blobName) {
        super(options);


        if (options.getClientEncryption() != null) {
            EncryptionVersion version;
            if (options.getClientEncryption().equals("1.0")) {
                version = EncryptionVersion.V1;
            } else if (options.getClientEncryption().equals("2.0")) {
                version = EncryptionVersion.V2;
            } else {
                throw new IllegalArgumentException("Encryption version not recognized");
            }


            EncryptedBlobClientBuilder builder = new EncryptedBlobClientBuilder(version)
                .blobClient(blobContainerClient.getBlobClient(blobName))
                .key(fakeKeyEncryptionKey, KeyWrapAlgorithm.A256KW.toString());

            blobClient = builder.buildEncryptedBlobClient();
            blobAsyncClient = builder.buildEncryptedBlobAsyncClient();
        } else {
            blobClient = blobContainerClient.getBlobClient(blobName);
            blobAsyncClient = blobContainerAsyncClient.getBlobAsyncClient(blobName);
        }

        blockBlobClient = blobContainerClient.getBlobClient(blobName).getBlockBlobClient();
        blockBlobAsyncClient = blobContainerAsyncClient.getBlobAsyncClient(blobName).getBlockBlobAsyncClient();
    }

    static class NoopProcessor implements SpanProcessor
    {

        @Override
        public void onStart(Context context, ReadWriteSpan readWriteSpan) {
        }

        @Override
        public boolean isStartRequired() {
            return false;
        }

        @Override
        public void onEnd(ReadableSpan readableSpan) {
            //System.out.println(readableSpan.getName());
        }

        @Override
        public boolean isEndRequired() {
            return false;
        }
    }
}
