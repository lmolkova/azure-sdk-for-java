// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateTimeSerializerTests {

    @Test
    public void serialization() throws IOException {
        JSON.registerSerializer(OffsetDateTime.class, new DateTimeBeanWriter());
        assertEquals("{}", JSON.writeVal(new Model(null)));
        assertEquals("\"0001-01-01T14:00:00Z\"", JSON.writeVal(OffsetDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-14))));
        assertEquals("\"10000-01-01T13:59:59.999Z\"", JSON.writeVal(OffsetDateTime.of(LocalDate.of(10000, 1, 1), LocalTime.parse("13:59:59.999"), ZoneOffset.UTC)));
        assertEquals("\"2010-01-01T12:34:56Z\"", JSON.writeVal(OffsetDateTime.of(2010, 1, 1, 12, 34, 56, 0, ZoneOffset.UTC)));
        assertEquals("\"0001-01-01T00:00:00Z\"", JSON.writeVal(OffsetDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)));
    }

    class Model {
        public Model(OffsetDateTime time) {
            this.time = time;
        }

        @JsonProperty
        OffsetDateTime time;
    }
}
