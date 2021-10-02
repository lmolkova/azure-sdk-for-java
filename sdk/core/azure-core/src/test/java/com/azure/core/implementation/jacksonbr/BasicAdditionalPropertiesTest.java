package com.azure.core.implementation.jacksonbr;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BasicAdditionalPropertiesTest {
    @Test
    public void basicSerialization() throws IOException {
        Model m = new Model();
        m.additionalProperties.put("foo", 1);
        m.additionalProperties.put("bar", "2");

        String str = JSON.writeVal(m);
        assertTrue(str.equals("{\"foo\":1,\"bar\":\"2\"}") ||
            str.equals("{\"bar\":\"2\",\"foo\":1}"));

        Model m2 = JSON.readVal(str, Model.class);
        areEqual(m.additionalProperties, m2.additionalProperties);
    }

    @Test
    public void basicSerializationComplexObject() throws IOException {
        JSON.registerSerializer(Duration.class, new DurationBeanWriter());
        Model m = new Model();
        m.additionalProperties.put("foo", Duration.ofMinutes(1));
        m.additionalProperties.put("bar", new int[]{1,2});

        String str = JSON.writeVal(m);
        assertTrue(str.equals("{\"foo\":\"PT1M\",\"bar\":[1,2]}") ||
            str.equals("{\"bar\":[1,2],\"foo\":\"PT1M\"}"));

        Model m2 = JSON.readVal(str, Model.class);
        areEqual(m.additionalProperties, m2.additionalProperties);

    }

    private boolean areEqual(Map<String, Object> first, Map<String, Object> second) {
        if (first.size() != second.size()) {
            return false;
        }

        return first.entrySet().stream()
            .allMatch(e -> e.getValue().equals(second.get(e.getKey())));
    }

    public static class Model {
        public Map<String, Object> additionalProperties = new HashMap<>();
    }
}
