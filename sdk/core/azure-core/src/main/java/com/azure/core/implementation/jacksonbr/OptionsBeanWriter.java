package com.azure.core.implementation.jacksonbr;

import com.azure.core.implementation.Option;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SerializedString;

import java.io.IOException;

public class OptionsBeanWriter implements BeanWriter<Option>{
    @Override
    public void writeValue(Option value, JsonGenerator g, JSONWriter context) throws IOException {
        if (value.isInitialized()) {
            if (value.getValue() == null) {
                g.writeNull();
            } else {
                context.writeValue(value.getValue(), g);
            }
        }
    }

    @Override
    public boolean shouldWriteField(SerializedString propertyName, Option value) throws IOException {
        return  value != null && value.isInitialized();
    }
}
