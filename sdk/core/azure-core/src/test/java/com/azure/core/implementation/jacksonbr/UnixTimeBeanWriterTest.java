package com.azure.core.implementation.jacksonbr;

import com.azure.core.implementation.Option;
import com.azure.core.implementation.UnixTime;
import com.azure.core.implementation.jackson.ObjectMapperShim;
import com.azure.core.util.DateTimeRfc1123;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnixTimeBeanWriterTest {

    @Test
    public void testSerialize() throws IOException {
        JSON.registerSerializer(UnixTime.class, new UnixTimeBeanWriter());
        JSON.registerDeserializer(UnixTime.class, new UnixTimeBeanReader());
        UnixTime nullTime  = null;

        assertEquals("{}", JSON.writeVal(new Model(null)));
        assertEquals("", JSON.writeVal(nullTime));

        UnixTime ut = new UnixTime(Instant.now().atOffset(ZoneOffset.UTC).toEpochSecond());

        assertEquals(String.format("{\"time\":%s}", ut.toString()), JSON.writeVal(new Model(ut)));
        assertEquals(String.format("%s", ut.toString()), JSON.writeVal(ut));
        assertEquals(ut, JSON.readVal(JSON.writeVal(ut), UnixTime.class));


        assertEquals(String.format("{\"time\":%s}", ut.toString()), JSON.writeVal(new Model(ut)));
        assertEquals(String.format(ut.toString()), JSON.writeVal(ut));
    }

    @Test
    public void testDeserialize() throws IOException {
        JSON.registerDeserializer(UnixTime.class, new UnixTimeBeanReader());
        JSON.registerSerializer(UnixTime.class, new UnixTimeBeanWriter());

        OffsetDateTime now = OffsetDateTime.now();
        UnixTime ut = new UnixTime(now);
        String utStr = now.toString();
        assertEquals(null, JSON.readVal("", UnixTime.class));

        assertEquals(null, JSON.readVal("{}", Model.class).time);
        assertEquals(null, JSON.readVal("{\"time\":\"\"}", Model.class).time);

        assertEquals(ut, JSON.readVal(String.format("\"%s\"", utStr), UnixTime.class));
        assertEquals(ut, JSON.readVal(String.format("{\"time\":\"%s\"}", utStr), Model.class).time);
    }

    static class Model {
        public Model() {time = null;}
        public Model(UnixTime time) {
            this.time = time;
        }

        @JsonProperty
        UnixTime time;
    }
}
