// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;

public final class ByteArrayBeanWriter implements BeanWriter<Byte[]> {

    @Override
    public void writeValue(Byte[] value, JsonGenerator g, JSONWriter context) throws IOException {
        byte[] bytes = new byte[value.length];
        for (int i = 0; i < value.length; i++) {
            bytes[i] = value[i];
        }
        g.writeBinary(bytes);
    }
}
