// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import static com.fasterxml.jackson.core.JsonTokenId.ID_EMBEDDED_OBJECT;
import static com.fasterxml.jackson.core.JsonTokenId.ID_FALSE;
import static com.fasterxml.jackson.core.JsonTokenId.ID_NULL;
import static com.fasterxml.jackson.core.JsonTokenId.ID_NUMBER_FLOAT;
import static com.fasterxml.jackson.core.JsonTokenId.ID_NUMBER_INT;
import static com.fasterxml.jackson.core.JsonTokenId.ID_START_ARRAY;
import static com.fasterxml.jackson.core.JsonTokenId.ID_START_OBJECT;
import static com.fasterxml.jackson.core.JsonTokenId.ID_STRING;
import static com.fasterxml.jackson.core.JsonTokenId.ID_TRUE;

/**
 * {@link ValueReader} used for "untyped" values; ones that are bound
 * to whatever {@link Object} is the natural mapping to JSON
 * value that parser currently points to
 */
public class AnyReader extends ValueReader
{
    private static  CollectionBuilder _collectionBuilder = CollectionBuilder.defaultImpl();
    private static  MapBuilder _mapBuilder = MapBuilder.defaultImpl();
    public final static AnyReader std = new AnyReader();

    public AnyReader() { super(Object.class); }

    @Override
    public Object readNext(JsonParser p) throws IOException
    {
        p.nextToken();
        return read(p);
    }

    @Override
    public Object read(JsonParser p) throws IOException
    {
        switch (p.currentTokenId()) {
        case ID_NULL:
            return null;
        case ID_START_OBJECT:
            return readFromObject(p, _mapBuilder);
        case ID_START_ARRAY:
            /*if (r.arraysAsLists()) {
                return readCollectionFromArray(r, p, r._collectionBuilder);
            }*/
            return readArrayFromArray(p, _collectionBuilder);
        case ID_STRING:
            return fromString(p.getText());
        case ID_NUMBER_INT:
            {
                NumberType n = p.getNumberType();
                if (n == NumberType.INT) {
                    return Integer.valueOf(p.getIntValue());
                }
                if (n == NumberType.LONG) {
                    return Long.valueOf(p.getLongValue());
                }
                return p.getBigIntegerValue();
            }
        case ID_NUMBER_FLOAT:
            NumberType n = p.getNumberType();
            if (n == NumberType.FLOAT) {
                return Float.valueOf(p.getFloatValue());
            }
            if (n == NumberType.DOUBLE) {
                return Double.valueOf(p.getDoubleValue());
            }
            return p.getDecimalValue();
        case ID_TRUE:
            return fromBoolean(true);
        case ID_FALSE:
            return fromBoolean(false);
        case ID_EMBEDDED_OBJECT:
            return fromEmbedded(p.getEmbeddedObject());

            // Others are error cases...
            /*
        default:
        case END_ARRAY:
        case END_OBJECT:
        case FIELD_NAME:
        case NOT_AVAILABLE:
        */
        }
        throw new IOException("Unexpected value token: "+_tokenDesc(p));
    }

    public Map<String, Object> readFromObject(JsonParser p, MapBuilder b) throws IOException
    {
        // First, a minor optimization for empty Maps
        if (p.nextValue() == JsonToken.END_OBJECT) {
            return b.emptyMap();
        }
        // and another for singletons...
        String key = fromKey(p.getCurrentName());
        Object value = read(p);

        if (p.nextValue() == JsonToken.END_OBJECT) {
            return b.singletonMap(key, value);
        }

        // but then it's loop-de-loop
        try {
            b = b.start().put(key, value);
            do {
                b = b.put(fromKey(p.getCurrentName()), read(p));
            } while (p.nextValue() != JsonToken.END_OBJECT);
        } catch (IllegalArgumentException e) {
            throw  new IOException(e.getMessage());
        }
        return b.build();
    }

    public Object[] readArrayFromArray(JsonParser p, CollectionBuilder b) throws IOException
    {
        // First two special cases; empty, single-element
        if (p.nextToken() == JsonToken.END_ARRAY) {
            return b.emptyArray();
        }
        Object value = read(p);
        if (p.nextToken() == JsonToken.END_ARRAY) {
            return b.singletonArray(value);
        }
        try {
            b = b.start().add(value);
            do {
                b = b.add(read(p));
            } while (p.nextToken() != JsonToken.END_ARRAY);
            return b.buildArray();
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }
    }

    public Collection<Object> readCollectionFromArray(JsonParser p, CollectionBuilder b) throws IOException
    {
        if (p.nextToken() == JsonToken.END_ARRAY) {
            return b.emptyCollection();
        }
        Object value = read(p);
        if (p.nextToken() == JsonToken.END_ARRAY) {
            return b.singletonCollection(value);
        }
        try {
            b = b.start().add(value);
            do {
                b = b.add(read(p));
            } while (p.nextToken() != JsonToken.END_ARRAY);
            return b.buildCollection();
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }
    }

    /*
    /**********************************************************************
    /* Internal methods, simple scalar conversions
    /**********************************************************************
     */

    /**
     * Method called to let implementation change a null value that has been
     * read from input.
     * Default implementation returns null as is.
     */
    protected Object fromNull() throws IOException {
        return null;
    }

    /**
     * Method called to let implementation change a {@link Boolean} value that has been
     * read from input.
     * Default implementation returns Boolean value as is.
     */
    protected Object fromBoolean(boolean b) throws IOException {
        return b ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Method called to let implementation change a key of an Object field
     * after being parsed from input.
     * Default implementation returns key as is.
     */
    protected String fromKey(String key) throws IOException {
        return key;
    }

    /**
     * Method called to let implementation change a {@link String} value that has been
     * read from input.
     * Default implementation returns String value as is.
     */
    protected Object fromString(String str) throws IOException {
        // Nothing fancy, by default; return as is
        return str;
    }

    protected Object fromEmbedded(Object value) throws IOException {
        return value;
    }
}
