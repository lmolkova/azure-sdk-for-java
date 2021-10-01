// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.azure.core.util.Base64Url;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;

public final class Base64UrlBeanWriter implements BeanWriter<Base64Url> {

    @Override
    public void writeValue(Base64Url value, JsonGenerator g, JSONWriter context) throws IOException {
        g.writeString(value.toString());
    }
}
