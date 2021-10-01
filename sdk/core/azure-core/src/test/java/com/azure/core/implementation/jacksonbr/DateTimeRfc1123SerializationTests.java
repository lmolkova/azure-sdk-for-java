// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.azure.core.implementation.UnixTime;
import com.azure.core.util.DateTimeRfc1123;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link DateTimeRfc1123}.
 */
public class DateTimeRfc1123SerializationTests {
    private static final DateTimeFormatter RFC1123_DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'").withZone(ZoneId.of("UTC")).withLocale(Locale.US);

    @Test
    public void serialization() throws IOException {
        JSON.registerSerializer(DateTimeRfc1123.class, new DateTimeRfc1123BeanWriter());
        assertEquals("{}", JSON.writeVal(new Model(null)));
        OffsetDateTime now = OffsetDateTime.now();
        DateTimeRfc1123 dt = new DateTimeRfc1123(now);
        assertEquals(String.format("{\"time\":\"%s\"}", dt.toString()), JSON.writeVal(new Model(dt)));
    }

    class Model {
        public Model(DateTimeRfc1123 time) {
            this.time = time;
        }

        @JsonProperty
        DateTimeRfc1123 time;
    }
}
