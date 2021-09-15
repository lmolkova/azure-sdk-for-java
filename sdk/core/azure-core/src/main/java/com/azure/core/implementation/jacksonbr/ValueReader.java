// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

public abstract class ValueReader
{
    protected final Class<?> _valueType;

    protected ValueReader(Class<?> valueType) {
        _valueType = valueType;
    }

    public abstract Object read(JsonParser p) throws IOException;

    public Object readNext(JsonParser p) throws IOException {
        p.nextToken();
        return read(p);
    }

    public static String _tokenDesc(JsonParser p) throws IOException {
        return _tokenDesc(p, p.currentToken());
    }

    public static String _tokenDesc(JsonParser p, JsonToken t) throws IOException {
        if (t == null) {
            return "NULL";
        }
        switch (t) {
        case FIELD_NAME:
            return "JSON Field name '"+p.getCurrentName()+"'";
        case START_ARRAY:
            return "JSON Array";
        case START_OBJECT:
            return "JSON Object";
        case VALUE_FALSE:
            return "`false`";
        case VALUE_NULL:
            return "'null'";
        case VALUE_NUMBER_FLOAT:
        case VALUE_NUMBER_INT:
            return "JSON Number";
        case VALUE_STRING:
            return "JSON String";
        case VALUE_TRUE:
            return "`true`";
        case VALUE_EMBEDDED_OBJECT:
            {
                final Object value = p.getEmbeddedObject();
                if (value == null) {
                    return "EMBEDDED_OBJECT `null`";
                }
                return "EMBEDDED_OBJECT of type "+p.getClass().getName();
            }
        default:
            return t.toString();
        }
    }
}
