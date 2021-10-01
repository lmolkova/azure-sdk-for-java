// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Custom serializer for serializing {@link OffsetDateTime} object into ISO8601 formats.
 */
public final class DateTimeBeanWriter implements BeanWriter<OffsetDateTime> {

    @Override
    public void writeValue(OffsetDateTime value, JsonGenerator g, JSONWriter context) throws IOException {
        g.writeString(toString(value));
    }

    /**
     * Convert the provided OffsetDateTime to its String representation.
     *
     * @param offsetDateTime The OffsetDateTime to convert.
     * @return The String representation of the provided offsetDateTime.
     */
    public static String toString(OffsetDateTime offsetDateTime) {
        String result = null;
        if (offsetDateTime != null) {
            offsetDateTime = offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC);
            result = DateTimeFormatter.ISO_INSTANT.format(offsetDateTime);
            if (result.startsWith("+")) {
                result = result.substring(1);
            }
        }
        return result;
    }
}
