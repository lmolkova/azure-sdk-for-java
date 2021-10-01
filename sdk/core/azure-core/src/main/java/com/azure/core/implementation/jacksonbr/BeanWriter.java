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
import java.util.List;

public interface BeanWriter<T> {
    void writeValue(T value, JsonGenerator g, JSONWriter context) throws IOException;

    default boolean shouldWriteField(SerializedString propertyName, T value) throws IOException {
        return value != null;
    }
}

class BeanWriterDefault implements BeanWriter<Object> {
    private final WriterCache _writerCache;
    private final ClassKey.BeanProperty[] _properties;
    private static final ClassKey.BeanProperty[] NO_PROPS_FOR_WRITE = new ClassKey.BeanProperty[0];

    public BeanWriterDefault(POJODefinition beanDef, WriterCache cache) {
        _writerCache = cache;
        _properties = _resolveBeanForSer(beanDef);
    }

    @Override
    public void writeValue(Object value, JsonGenerator g, JSONWriter context) throws IOException {
        writeBeanValue(value, context, g);
    }

    private void writeBeanValue(Object bean, JSONWriter writer, JsonGenerator g) throws IOException {
        if (_properties.length == 1 && _properties[0].name.getValue().isEmpty()) {
            Object value = _properties[0].getValueFor(bean);
            if (value != null) {
                writer._writeValue(value,  g);
            }

            return;
        }

        g.writeStartObject();
        for (int i = 0, end = _properties.length; i < end; ++i) {
            ClassKey.BeanProperty property = _properties[i];
            SerializedString name = property.name;
            Object value = property.getValueFor(bean);
            if (value == null) {
                continue;
            }

            BeanWriter propWriter = _writerCache.getOrAdd(property.typeId);
            if (propWriter.shouldWriteField(name, value)) {
                g.writeFieldName(name);
                writer.writeValue(value, g);
            }
        }
        g.writeEndObject();
    }

    private ClassKey.BeanProperty[] _resolveBeanForSer(POJODefinition beanDef) {
        final POJODefinition.Prop[] rawProps = beanDef.properties();
        final int len = rawProps.length;
        List<ClassKey.BeanProperty> props = new ArrayList<ClassKey.BeanProperty>(len);

        for (int i = 0; i < len; ++i) {
            POJODefinition.Prop rawProp = rawProps[i];
            Method m = rawProp.getter;
            Field f = rawProp.field;
            Object value = rawProp.value;

            Class<?> type;
            if (m != null) {
                type = m.getReturnType();
                m.setAccessible(true);
            } else if (rawProp.field != null) {
                type = f.getType();
                f.setAccessible(true);
            } else {
                type = rawProp.value.getClass();
            }

            props.add(new ClassKey.BeanProperty(type, rawProp.name, f, m, value));
        }
        int plen = props.size();
        ClassKey.BeanProperty[] propArray = (plen == 0) ? NO_PROPS_FOR_WRITE
            : props.toArray(NO_PROPS_FOR_WRITE);
        return propArray;
    }
}
