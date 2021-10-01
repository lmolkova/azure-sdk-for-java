// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_BOOLEAN;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_BOOLEAN_ARRAY;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_BYTE_ARRAY;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_CALENDAR;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_CHAR;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_CHARACTER_SEQUENCE;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_CHAR_ARRAY;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_CLASS;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_COLLECTION;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_DATE;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_ENUM;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_FILE;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_INT_ARRAY;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_ITERABLE;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_LIST;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_LONG_ARRAY;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_MAP;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_NUMBER_BIG_DECIMAL;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_NUMBER_BIG_INTEGER;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_NUMBER_BYTE;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_NUMBER_DOUBLE;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_NUMBER_FLOAT;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_NUMBER_INTEGER;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_NUMBER_LONG;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_NUMBER_SHORT;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_OBJECT_ARRAY;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_STRING;
//import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils. SER_UNIXTIME;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_UNKNOWN;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_URI;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_URL;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_UUID;

final public class JSONWriter {
    private final WriterCache _writerLocator;

    public JSONWriter(WriterCache loc) {
        _writerLocator = loc;
    }

    public void writeValue(Object value, JsonGenerator generator) throws IOException {
        if (value == null) {
            return;
        }
        _writeValue(value, generator);
    }


    private void writeField(String fieldName, Object value, JsonGenerator generator) throws IOException {
        generator.writeFieldName(fieldName);
        _writeValue(value, generator);
    }

    void _writeValue(Object value, JsonGenerator generator) throws IOException {
        if (value == null) {
            return;
        }

        int type = ValueLocatorUtils._findSimpleType(value.getClass(), true);

        switch (type) {

            // Structured types:
            case SER_MAP:
                writeMapValue((Map<?, ?>) value, generator);
                return;
            case SER_LIST:
                writeListValue((List<?>) value, generator);
                return;
            case SER_COLLECTION:
                writeCollectionValue((Collection<?>) value, generator);
                return;
            case SER_OBJECT_ARRAY:
                writeObjectArrayValue((Object[]) value, generator);
                return;
            case SER_INT_ARRAY:
                writeIntArrayValue((int[]) value, generator);
                return;
            case SER_LONG_ARRAY:
                writeLongArrayValue((long[]) value, generator);
                return;
            case SER_BOOLEAN_ARRAY:
                writeBooleanArrayValue((boolean[]) value, generator);
                return;

            // Textual types, related:
            case SER_STRING:
                generator.writeString((String) value);
                return;
            case SER_CHAR_ARRAY:
                generator.writeString(new String((char[]) value));
                return;
            case SER_CHARACTER_SEQUENCE:
                generator.writeString(((CharSequence) value).toString());
                return;
            case SER_BYTE_ARRAY:
                generator.writeBinary((byte[]) value);
                return;

            // Number types:

            case SER_NUMBER_FLOAT: // fall through
            case SER_NUMBER_DOUBLE:
                generator.writeNumber(((Number) value).doubleValue());
                return;
            case SER_NUMBER_BYTE: // fall through
            case SER_NUMBER_SHORT: // fall through
            case SER_NUMBER_INTEGER:
                generator.writeNumber(((Number) value).intValue());
                return;
            case SER_NUMBER_LONG:
                generator.writeNumber(((Number) value).longValue());
                return;
            case SER_NUMBER_BIG_DECIMAL:
                generator.writeNumber((BigDecimal) value);
                return;
            case SER_NUMBER_BIG_INTEGER:
                generator.writeNumber((BigInteger) value);
                return;

            // Other scalar types:

            case SER_BOOLEAN:
                generator.writeBoolean((((Boolean) value).booleanValue()));
                return;
            case SER_CHAR:
                generator.writeString((String.valueOf(value)));
                return;
            case SER_CALENDAR:
                generator.writeString(dateToString(((Calendar) value).getTime()));
                return;
            case SER_DATE:
                generator.writeString(dateToString((Date) value));
                return;

            case SER_ENUM:
                generator.writeString(value.toString());
                return;
            case SER_CLASS:
                generator.writeString(((Class<?>) value).getName());
                return;
            case SER_FILE:
                generator.writeString(((File) value).getAbsolutePath());
                return;
            // these type should be fine using toString()
            case SER_UUID:
            case SER_URL:
            case SER_URI:
                generator.writeString(value.toString());
                return;

            case SER_ITERABLE:
                writeIterableValue((Iterable<?>) value, generator);
                return;
            /*case SER_UNIXTIME:
                generator.writeNumber(value.toString());
            case SER_UNKNOWN:
                writeUnknownValue(value, generator);
                return;*/
        }

        BeanWriter writer = _writerLocator.getOrAdd(value.getClass());
        if (writer != null) { // sanity check
            writer.writeValue(value, generator, this);
            return;
        }

        _badType(type, value);
    }

    private void writeCollectionValue(Collection<?> v, JsonGenerator generator) throws IOException {
        generator.writeStartArray();
        for (Object ob : v) {
            writeValue(ob, generator);
        }
        generator.writeEndArray();
    }

    private void writeIterableValue(Iterable<?> v, JsonGenerator generator) throws IOException {
        generator.writeStartArray();
        for (Object ob : v) {
            writeValue(ob, generator);
        }
        generator.writeEndArray();
    }

    private void writeListValue(List<?> list, JsonGenerator generator) throws IOException {
        generator.writeStartArray();
        for (int i = 0, len = list.size(); i < len; ++i) {
            Object value = list.get(i);
            if (value == null) {
                generator.writeNull();
                continue;
            }
            _writeValue(value, generator);
        }
        generator.writeEndArray();
    }

    private void writeMapValue(Map<?, ?> v, JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        if (!v.isEmpty()) {
            for (Map.Entry<?, ?> entry : v.entrySet()) {
                String key = keyToString(entry.getKey());
                Object value = entry.getValue();

                if (value == null) {
                    continue;
                }

                writeField(key, value, generator);
            }
        }
        generator.writeEndObject();
    }

    private void writeObjectArrayValue(Object[] v, JsonGenerator generator) throws IOException {
        generator.writeStartArray();
        for (int i = 0, len = v.length; i < len; ++i) {
            writeValue(v[i], generator);
        }
        generator.writeEndArray();
    }

    private void writeIntArrayValue(int[] v, JsonGenerator generator) throws IOException {
        generator.writeStartArray();
        for (int i = 0, len = v.length; i < len; ++i) {
            generator.writeNumber(v[i]);
        }
        generator.writeEndArray();
    }

    private void writeLongArrayValue(long[] v, JsonGenerator generator) throws IOException {
        generator.writeStartArray();
        for (int i = 0, len = v.length; i < len; ++i) {
            generator.writeNumber(v[i]);
        }
        generator.writeEndArray();
    }

    private void writeBooleanArrayValue(boolean[] v, JsonGenerator generator) throws IOException {
        generator.writeStartArray();
        for (int i = 0, len = v.length; i < len; ++i) {
            generator.writeBoolean(v[i]);
        }
        generator.writeEndArray();
    }

    private void writeUnknownValue(Object data, JsonGenerator generator) throws IOException {
        _checkUnknown(data);
        generator.writeString(data.toString());
    }

    private void _checkUnknown(Object value) throws IOException {
        /*if (JSON.Feature.FAIL_ON_UNKNOWN_TYPE_WRITE.isEnabled(_features)) {
            throw new JSONObjectException("Unrecognized type ("+value.getClass().getName()
                    +"), don't know how to write (disable "+JSON.Feature.FAIL_ON_UNKNOWN_TYPE_WRITE
                    +" to avoid exception)");
        }*/
    }

    private String keyToString(Object rawKey) {
        if (rawKey instanceof String) {
            return (String) rawKey;
        }
        return String.valueOf(rawKey);
    }

    private String dateToString(Date v) {
        if (v == null) {
            return "";
        }
        // !!! 01-Dec-2015, tatu: Should really use proper DateFormat or something
        //   since this relies on system-wide defaults, and hard/impossible to
        //   change easily
        return v.toString();
    }

    /*
    /**********************************************************************
    /* Other internal methods
    /**********************************************************************
     */

    private void _badType(int type, Object value) {
        if (type < 0) {
            throw new IllegalStateException(String.format(
                "Internal error: missing BeanDefinition for id %d (class %s)",
                type, value.getClass().getName()));
        }
        throw new IllegalStateException(String.format(
            "Unsupported type: %s (%s)", type, value.getClass().getName()));
    }
}
