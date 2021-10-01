package com.azure.core.implementation.jacksonbr;

import com.azure.core.implementation.Option;
import com.azure.core.implementation.UnixTime;
import com.azure.core.util.Base64Url;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Base64UrlBeanWriterTest {

    @Test
    public void testSerialize() throws IOException {
        JSON.registerSerializer(Base64Url.class, new Base64UrlBeanWriter());
        assertEquals("{}", JSON.writeVal(new Model(null)));
        Base64Url url = new Base64Url("AAECAwQFBgcICQ");
        assertEquals(String.format("{\"url\":\"%s\"}", url.toString()), JSON.writeVal(new Model(url)));
    }

    class Model {
        public Model(Base64Url url) {
            this.url = url;
        }

        @JsonProperty
        Base64Url url;
    }
}
