package com.azure.core.implementation.jacksonbr;

import com.azure.core.annotation.JsonFlatten;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BasicFlatteningPropertiesTest {
    @Test
    public void basicFlatModelSerialization() throws IOException {
        ModelFlat m = new ModelFlat("1", "2", "3");

        String str = JSON.writeVal(m);
        assertEquals("{\"prop\":{\"a\":\"1\",\"b\":\"2\",\"c\":\"3\"}}", str);
    }

    @Test
    public void basicFlatPropSerialization() throws IOException {
        Model m = new Model("1", "2", "3");

        String str = JSON.writeVal(m);
        assertEquals("{\"prop.not.flat\":\"3\",\"prop\":{\"multi\":{\"layer\":{\"a\":\"1\",\"b\":\"2\"}}}}", str);
    }


    @JsonFlatten
    public static class ModelFlat {

        @JsonProperty("prop.a")
        public final String a;

        @JsonProperty("prop.b")
        public final String b;

        @JsonProperty("prop.c")
        public final String c;

        public ModelFlat(String a, String b, String c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    public static class Model {

        @JsonFlatten
        @JsonProperty("prop.multi.layer.a")
        public final String a;

        @JsonFlatten
        @JsonProperty("prop.multi.layer.b")
        public final String b;

        @JsonProperty("prop.not.flat")
        public final String c;

        public Model(String a, String b, String c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }
}
