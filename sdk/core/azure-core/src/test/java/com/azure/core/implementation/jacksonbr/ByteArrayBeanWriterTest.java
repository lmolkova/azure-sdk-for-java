package com.azure.core.implementation.jacksonbr;

import com.azure.core.util.Base64Url;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;
import wiremock.org.apache.commons.lang3.ArrayUtils;
import wiremock.org.eclipse.jetty.util.ArrayUtil;

import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ByteArrayBeanWriterTest {

    @Test
    public void testSerialize() throws IOException {
        JSON.registerSerializer(Byte[].class, new ByteArrayBeanWriter());

        assertEquals("{}", JSON.writeVal(new Model(null)));
        Byte[] array = new Byte[]{1,2,3,4,5};
        assertEquals(String.format("{\"array\":\"%s\"}", "AQIDBAU="), JSON.writeVal(new Model(array)));
    }

    class Model {
        public Model(Byte[] array) {
            this.array = array;
        }

        @JsonProperty
        Byte[] array;
    }
}
