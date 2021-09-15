// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_UNKNOWN;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_URI;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_URL;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_UUID;

/**
 * Default {@link ValueReader} used for simple scalar types and related,
 * not including POJO-, {@link Map} and {@link Collection}
 * types.
 */
public class SimpleValueReader extends ValueReader
{
    protected final CollectionBuilder _collectionBuilder = CollectionBuilder.defaultImpl();
    private static final Class<?> INT_ARRAY_CLASS = (new Integer[0]).getClass();
    protected final int _typeId;

    public SimpleValueReader(Class<?> raw, int typeId) {
        super(raw);
        _typeId = typeId;
    }

    @Override
    public Object readNext(JsonParser p) throws IOException
    {
        // NOTE: only cases where we can optimize
        switch (_typeId) {
        // Textual types, related:
        case SER_STRING:
        case SER_CHARACTER_SEQUENCE:
            {
                String str = p.nextTextValue();
                if (str != null) {
                    return str;
                }
                return read(p);
            }

        case SER_CHAR_ARRAY:
            {
                String str = p.nextTextValue();
                if (str != null) {
                    return str.toCharArray();
                }
                return read(p);
            }

        // Number types:

        // Let's only optimize common ones, int/Integer, long/Long;
        // and only when `nextXxx()` correctly returns value. In all other
        // cases default to "standard" handling which does range checks etc

        case SER_NUMBER_INTEGER:
            {
                int i = p.nextIntValue(-2);
                if (i != -2) {
                    return i;
                }
                return read(p);
            }

        case SER_NUMBER_LONG:
            {
                long l = p.nextLongValue(-2L);
                if (l != -2L) {
                    return l;
                }
                return read(p);
            }

        // Other scalar types:

        case SER_BOOLEAN:
            {
                Boolean b = p.nextBooleanValue();
                if (b != null) {
                    return b;
                }
                return read(p);
            }
        }

        p.nextToken();
        return read(p);
    }

    @Override
    public Object read(JsonParser p) throws IOException
    {
        switch (_typeId) {

        case SER_INT_ARRAY:
            return _readIntArray(p);

        /*case SER_TREE_NODE:
            return reader.readTree();*/

        // Textual types, related:
        case SER_STRING:
        case SER_CHARACTER_SEQUENCE:
            return p.getValueAsString();
        case SER_CHAR_ARRAY:
            return p.getValueAsString().toCharArray();
        case SER_BYTE_ARRAY:
            return _readBinary(p);

        // Number types:

        case SER_NUMBER_FLOAT: // fall through
            return Float.valueOf((float) p.getValueAsDouble());
        case SER_NUMBER_DOUBLE:
            return p.getValueAsDouble();

        case SER_NUMBER_BYTE: // fall through
            return (byte) p.getValueAsInt();

        case SER_NUMBER_SHORT: // fall through
            return (short) p.getValueAsInt();
        case SER_NUMBER_INTEGER:
            return p.getValueAsInt();
        case SER_NUMBER_LONG:
            return p.getValueAsLong();

        case SER_NUMBER_BIG_DECIMAL:
            return p.getDecimalValue();

        case SER_NUMBER_BIG_INTEGER:
            return p.getBigIntegerValue();

        // Other scalar types:

        case SER_BOOLEAN:
            switch (p.currentTokenId()) {
            case JsonTokenId.ID_TRUE:
                return Boolean.TRUE;
            case JsonTokenId.ID_FALSE:
                return Boolean.FALSE;
            case JsonTokenId.ID_NULL:
                // 07-Jul-2020, tatu: since `boolean` and `java.lang.Boolean` both handled
                //   here, can not (alas!) separate yet
                return Boolean.FALSE;

            case JsonTokenId.ID_STRING:
                // 07-Jul-2020, tatu: Allow coercion for backwards compatibility (with 2.11)
                return p.getValueAsBoolean();
            }
            // 07-Jul-2020, tatu: leave out more esoteric coercions with 2.12
            break;

        case SER_CHAR:
            {
                String str = p.getValueAsString();
                return (str == null || str.isEmpty()) ? ' ' : str.charAt(0);
            }

        case SER_CALENDAR:
            // [jackson-jr#73]: should allow null
            if (p.hasToken(JsonToken.VALUE_NULL)) {
                return null;
            }
            {
                long l = _fetchLong(p);
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(l);
                return cal;
            }

        case SER_DATE:
            // [jackson-jr#73]: should allow null
            if (p.hasToken(JsonToken.VALUE_NULL)) {
                return null;
            }
            return new Date(_fetchLong(p));

        case SER_CLASS:
        {
            // [jackson-jr#73]: should allow null
            if (p.hasToken(JsonToken.VALUE_NULL)) {
                return null;
            }
            String v = p.getValueAsString();
            try {
                return Class.forName(v);
            } catch (Exception e) {
                throw new IOException("Failed to bind java.lang.Class from value '"+v+"'");
            }
        }
        case SER_FILE:
            // [jackson-jr#73]: should allow null
            if (p.hasToken(JsonToken.VALUE_NULL)) {
                return null;
            }
            return new File(p.getValueAsString());
        case SER_UUID:
            // [jackson-jr#73]: should allow null
            if (p.hasToken(JsonToken.VALUE_NULL)) {
                return null;
            }
            return UUID.fromString(p.getValueAsString());
        case SER_URL:
            // [jackson-jr#73]: should allow null
            if (p.hasToken(JsonToken.VALUE_NULL)) {
                return null;
            }
            return new URL(p.getValueAsString());
        case SER_URI:
            // [jackson-jr#73]: should allow null
            if (p.hasToken(JsonToken.VALUE_NULL)) {
                return null;
            }
            return URI.create(p.getValueAsString());

//        case SER_MAP:
//        case SER_LIST:
//        case SER_COLLECTION:
//        case SER_OBJECT_ARRAY:
            // should never get here: we have dedicated readers
        default: // types that shouldn't get here
        //case SER_ENUM:
        }

        throw new IOException("Can not create a `"+_valueType.getName()+"` instance out of "+_tokenDesc(p));
    }

    /*
    /**********************************************************************
    /* Read methods for scalars
    /**********************************************************************
     */

    protected byte[] _readBinary(JsonParser p) throws IOException {
        return p.getBinaryValue();
    }

    protected int[] _readIntArray(JsonParser p) throws IOException
    {
        JsonToken t = p.currentToken();
        if (t == JsonToken.VALUE_NULL) {
            return null;
        }
        if (t != JsonToken.START_ARRAY) {
            new IOException("Can not read an array: expect to see START_ARRAY ('['), instead got: "+ValueReader._tokenDesc(p));
        }

        CollectionBuilder b = _collectionBuilder.newBuilder(int.class);
        if (p.nextToken() == JsonToken.END_ARRAY) {
            return new int[0];
        }

        int value = p.getValueAsInt();
        if (p.nextToken() == JsonToken.END_ARRAY) {
            return new int[] {value};
        }

        List<Integer> list = new ArrayList<>();
        list.add(value);
        do {
            list.add(p.getValueAsInt());
        } while (p.nextToken() != JsonToken.END_ARRAY);

        int [] a = new int[list.size()];
        for (int i = 0; i < list.size(); i ++) {
            a[i] = list.get(i);
        }

        return a;
    }

    protected long _fetchLong(JsonParser p) throws IOException
    {
        JsonToken t = p.currentToken();
        if (t == JsonToken.VALUE_NUMBER_INT) {
            return p.getLongValue();
        }
        throw new IOException("Can not get long numeric value from JSON (to construct "
                +_valueType.getName()+") from "+_tokenDesc(p, t));
    }
}
