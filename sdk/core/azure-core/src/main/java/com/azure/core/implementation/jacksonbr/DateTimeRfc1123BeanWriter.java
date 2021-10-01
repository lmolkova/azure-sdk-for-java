// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.azure.core.util.DateTimeRfc1123;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;

/**
 * Custom serializer for serializing {@link DateTimeRfc1123} object into RFC1123 formats.
 */
public final class DateTimeRfc1123BeanWriter implements BeanWriter<DateTimeRfc1123> {
    @Override
    public void writeValue(DateTimeRfc1123 value, JsonGenerator g, JSONWriter context) throws IOException {
        g.writeString(value.toString()); //Use the default toString as it is RFC1123.
    }
}
