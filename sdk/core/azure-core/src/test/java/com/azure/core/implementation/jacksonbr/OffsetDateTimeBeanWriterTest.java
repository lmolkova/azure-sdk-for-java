package com.azure.core.implementation.jacksonbr;

import com.azure.core.implementation.UnixTime;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OffsetDateTimeBeanWriterTest {

    @Test
    public void testSerialize() throws IOException {
        JSON.registerSerializer(OffsetDateTime.class, new DateTimeBeanWriter());
        JSON.registerDeserializer(OffsetDateTime.class, new DateTimeBeanReader());

        OffsetDateTime nullTime  = null;

        assertEquals("{}", JSON.writeVal(new Model(null)));
        assertEquals("", JSON.writeVal(nullTime));

        OffsetDateTime utcNow = Instant.now().atOffset(ZoneOffset.UTC);
        assertEquals(String.format("{\"time\":\"%s\"}", utcNow), JSON.writeVal(new Model(utcNow)));
        assertEquals(String.format("\"%s\"", utcNow.toString()), JSON.writeVal(utcNow));
        assertEquals(utcNow, JSON.readVal(JSON.writeVal(utcNow), OffsetDateTime.class));
    }

    @Test
    public void testDeserialize() throws IOException {
        JSON.registerSerializer(OffsetDateTime.class, new DateTimeBeanWriter());
        JSON.registerDeserializer(OffsetDateTime.class, new DateTimeBeanReader());

        OffsetDateTime utcNow = Instant.now().atOffset(ZoneOffset.UTC);
        String nowStr = utcNow.toString();
        assertEquals(null, JSON.readVal("", UnixTime.class));

        assertEquals(null, JSON.readVal("{}", Model.class).time);
        assertEquals(null, JSON.readVal("{\"time\":\"\"}", Model.class).time);

        assertEquals(utcNow, JSON.readVal(String.format("\"%s\"", nowStr), OffsetDateTime.class));
        assertEquals(utcNow, JSON.readVal(String.format("{\"time\":\"%s\"}", nowStr), Model.class).time);
    }

    static class Model {
        public Model() {time = null;}
        public Model(OffsetDateTime time) {
            this.time = time;
        }

        @JsonProperty
        OffsetDateTime time;
    }
}
