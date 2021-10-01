package com.azure.core.implementation.jacksonbr;

import com.azure.core.implementation.UnixTime;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

public class UnixTimeBeanWriter implements BeanWriter<UnixTime> {
    @Override
    public void writeValue(UnixTime value, JsonGenerator g, JSONWriter context) throws IOException {
        g.writeNumber(value.toString());
    }
}
