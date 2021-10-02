// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SerializedString;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public interface BeanWriter<T> {
    void writeValue(T value, JsonGenerator g, JSONWriter context) throws IOException;

    default boolean shouldWriteField(SerializedString propertyName, T value) throws IOException {
        return value != null;
    }
}

class BeanWriterDefault implements BeanWriter<Object> {
    private final WriterCache _writerCache;
    private final POJODefinition  beanDef;
    private final POJODefinition.Prop[] _properties;
    private static final  POJODefinition.Prop[] NO_PROPS_FOR_WRITE = new  POJODefinition.Prop[0];

    public BeanWriterDefault(POJODefinition beanDef, WriterCache cache) {
        _writerCache = cache;
        _properties = (beanDef._properties.length == 0) ? NO_PROPS_FOR_WRITE : beanDef._properties;
        this.beanDef = beanDef;
    }

    @Override
    public void writeValue(Object value, JsonGenerator g, JSONWriter context) throws IOException {
        writeBeanValue(value, context, g);
    }

    private void writeBeanValue(Object bean, JSONWriter writer, JsonGenerator g) throws IOException {
        if (_properties.length == 1 && _properties[0].serializedName.getValue().isEmpty()) {
            Object value = _properties[0].getValue(bean);
            if (value != null) {
                writer._writeValue(value,  g);
            }

            return;
        }

        g.writeStartObject();
        for (int i = 0, end = _properties.length; i < end; ++i) {
            POJODefinition.Prop property = _properties[i];

            SerializedString name = property.serializedName;
            Object value = property.getValue(bean);
            if (value == null) {
                continue;
            }

            BeanWriter propWriter = _writerCache.getOrAdd(property.typeId);
            if (propWriter.shouldWriteField(name, value)) {
                if (property.unwrappedProp) {
                    if (value instanceof Map) {
                        Map<String, Object> additionalProperties = (Map<String, Object>) value;
                        if (!additionalProperties.isEmpty()) {
                            for (Map.Entry<String, Object> entry : additionalProperties.entrySet()) {
                                Object ev = entry.getValue();

                                if (ev == null) {
                                    continue;
                                }

                                g.writeFieldName(entry.getKey());
                                writer.writeValue(ev, g);
                            }
                        }
                    }
                } else {
                    g.writeFieldName(name);
                    writer.writeValue(value, g);
                }
            }
        }
        g.writeEndObject();
    }
}

