// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.azure.core.implementation.UnixTime;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.DecimalUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.TimeZone;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Custom deserializer for deserializing epoch formats into {@link UnixTime} objects.
 */
public final class UnixTimeBeanReader extends ValueReader<UnixTime> {

    private final DateTimeBeanReader internalReader;

    public UnixTimeBeanReader() {
        this(new DateTimeBeanReader());
    }

    private UnixTimeBeanReader(DateTimeBeanReader internal) {
        super(UnixTime.class);
        internalReader = internal;
    }

    @Override
    public UnixTime read(JsonParser parser) throws IOException {
        OffsetDateTime dt = internalReader.read(parser);
        if (dt != null) {
            return new UnixTime(dt);
        }

        return null;
    }
}
