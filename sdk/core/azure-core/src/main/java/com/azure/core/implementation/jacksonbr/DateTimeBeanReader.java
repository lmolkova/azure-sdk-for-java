// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;

/**
 * Custom deserializer that handles converting ISO8601 dates into {@link OffsetDateTime} objects.
 */
public class DateTimeBeanReader extends ValueReader<OffsetDateTime> {

    public DateTimeBeanReader() {
        super(OffsetDateTime.class);
    }

    @Override
    public OffsetDateTime read(JsonParser parser) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_NUMBER_INT) {
            return OffsetDateTime.ofInstant(Instant.ofEpochSecond(parser.getValueAsLong()), ZoneOffset.UTC);
        } else {
            String strValue = parser.getValueAsString();
            if (strValue.isEmpty()) {
                return null;
            }

            TemporalAccessor temporal = DateTimeFormatter.ISO_DATE_TIME
                .parseBest(strValue, OffsetDateTime::from, LocalDateTime::from);

            if (temporal.query(TemporalQueries.offset()) == null) {
                return LocalDateTime.from(temporal).atOffset(ZoneOffset.UTC);
            } else {
                return OffsetDateTime.from(temporal);
            }
        }
    }
}
