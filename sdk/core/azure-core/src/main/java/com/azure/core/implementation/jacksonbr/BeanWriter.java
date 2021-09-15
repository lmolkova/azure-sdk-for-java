// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SerializedString;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class BeanWriter {
    private final ClassKey.BeanProperty[] _properties;
    private static final ClassKey.BeanProperty[] NO_PROPS_FOR_WRITE = new ClassKey.BeanProperty[0];

    public BeanWriter(POJODefinition beanDef) {
        _properties = _resolveBeanForSer(beanDef);
    }

    public void writeValue(JSONWriter writer, JsonGenerator g, Object value)
        throws IOException {
        writeBeanValue(_properties, value, writer, g);
    }

    void writeBeanValue(ClassKey.BeanProperty[] props, Object bean, JSONWriter writer, JsonGenerator g) throws IOException {
        if (props.length == 1 && props[0].name.getValue().isEmpty()) {
            Object value = props[0].getValueFor(bean);
            if (value != null) {
                writer._writeValue(value, props[0].typeId, g);
            }

            return;
        }

        g.writeStartObject();
        for (int i = 0, end = props.length; i < end; ++i) {
            ClassKey.BeanProperty property = props[i];
            SerializedString name = property.name;
            Object value = property.getValueFor(bean);
            if (value == null) {
                continue;
            }

            g.writeFieldName(name);
            writer._writeValue(value, property.typeId, g);
        }
        g.writeEndObject();
    }

    private static ClassKey.BeanProperty[] _resolveBeanForSer(POJODefinition beanDef) {
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
            int typeId = ValueLocatorUtils._findSimpleType(type, true);
            props.add(new ClassKey.BeanProperty(typeId, rawProp.name, f, m, value));
        }
        int plen = props.size();
        ClassKey.BeanProperty[] propArray = (plen == 0) ? NO_PROPS_FOR_WRITE
            : props.toArray(NO_PROPS_FOR_WRITE);
        return propArray;
    }
}
