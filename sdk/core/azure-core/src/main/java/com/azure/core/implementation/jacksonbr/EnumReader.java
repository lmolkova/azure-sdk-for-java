// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.Map;

/**
 * Reader for Enum values: needed because we need a simple {@link Map}
 * for efficient conversion from id (gotten with {@link Enum#toString()}
 * to value.
 *<p>
 * In future we could consider alternatively allowing use of
 * {@link Enum#name()} for id.
 */
public class EnumReader extends ValueReader
{
    protected final Object[] _byIndex;
    protected final Map<String,Object> _byName;

    public EnumReader(Class<?> enumType,
            Object[] byIndex, Map<String,Object> byName) {
        super(enumType);
        _byIndex = byIndex;
        _byName = byName;
    }

    private String desc() {
        return _byIndex[0].getClass().getName();
    }

    @Override
    public Object readNext(JsonParser p) throws IOException {
        String name = p.nextTextValue();
        if (name != null) {
            return _enum(name);
        }
        return read(p);
    }

    @Override
    public Object read(JsonParser p) throws IOException {
        if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            int ix = p.getIntValue();
            if (ix < 0 || ix >= _byIndex.length) {
                throw new IOException("Failed to bind Enum "+desc()+" with index "+ix
                        +" (has "+_byIndex.length+" values)");
            }
            return _byIndex[ix];
        }
        if (p.hasToken(JsonToken.VALUE_NULL)) {
            return null;
        }
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            return _enum(p.getValueAsString().trim());
        }
        throw new IOException("Can not read Enum `"+_valueType.getName()+"` from "
                +_tokenDesc(p, p.currentToken()));
    }

    private Object _enum(String id) throws IOException
    {
        Object e = _byName.get(id);
        if (e == null) {
            throw new IOException("Failed to find Enum of type "+desc()+" for value '"+id+"'");
        }
        return e;
    }
}
