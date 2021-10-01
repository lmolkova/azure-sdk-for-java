// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.azure.core.implementation.Option;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OptionsSerializationTest {
    @Test
    public void serialization() throws IOException {
        JSON.registerSerializer(Option.class, new OptionsBeanWriter());
        assertEquals("{}", JSON.writeVal(new Model()));
        assertEquals("{\"option\":null}", JSON.writeVal(new Model(Option.of(null))));
        assertEquals("{\"option\":\"foo\"}", JSON.writeVal(new Model(Option.of("foo"))));
    }

    @Test
    public void serializationComplex() throws IOException {
        JSON.registerSerializer(Option.class, new OptionsBeanWriter());
        assertEquals("{}", JSON.writeVal(new ModelComplex()));
        assertEquals("{\"option\":null}", JSON.writeVal(new ModelComplex(Option.of(null))));
        assertEquals("{\"option\":{\"foo\":\"1\"}}", JSON.writeVal(new ModelComplex(Option.of(Map.of("foo", "1")))));
    }

    static class Model {
        public Model() {
            option = Option.uninitialized();
        }

        public Model(String val) {
            option = Option.of(val);
        }

        public Model(Option<String> opt) {
            option = opt;
        }

        @JsonProperty
        Option<String> option;
    }

    static class ModelComplex {
        public ModelComplex() {
            option = Option.uninitialized();
        }

        public ModelComplex(Map<String,String> val) {
            option = Option.of(val);
        }

        public ModelComplex(Option<Map<String,String>> opt) {
            option = opt;
        }

        @JsonProperty
        Option<Map<String,String>> option;
    }
}
