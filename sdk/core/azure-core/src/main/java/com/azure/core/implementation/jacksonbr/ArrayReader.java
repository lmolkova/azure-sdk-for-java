// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

/**
 * Reader for typed Array values.
 */
public class ArrayReader extends ValueReader
{
    private static  CollectionBuilder _collectionBuilder = CollectionBuilder.defaultImpl();
    protected final Class<?> _elementType;
    protected final ValueReader _valueReader;

    public ArrayReader(Class<?> arrayType, Class<?> elementType, ValueReader vr) {
        super(arrayType);
        _elementType = elementType;
        _valueReader = vr;
    }

    @Override
    public Object readNext(JsonParser p) throws IOException {
        if (p.nextToken() != JsonToken.START_ARRAY) {
            if (p.hasToken(JsonToken.VALUE_NULL)) {
                return null;
            }
            throw new IOException("Unexpected token %s; should get START_ARRAY" + p.currentToken());
        }

        CollectionBuilder b = _collectionBuilder.newBuilder();
        if (p.nextToken() == JsonToken.END_ARRAY) {
            return b.emptyArray(_elementType);
        }
        Object value = _valueReader.read(p);
        if (p.nextToken() == JsonToken.END_ARRAY) {
            return b.singletonArray(_elementType, value);
        }
        b = b.start().add(value);
        do {
            b = b.add(_valueReader.read(p));
        } while (p.nextToken() != JsonToken.END_ARRAY);
        return b.buildArray(_elementType);
    }

    @Override
    public Object read(JsonParser p) throws IOException {
        CollectionBuilder b = _collectionBuilder.newBuilder();
        if (p.nextToken() == JsonToken.END_ARRAY) {
            return b.emptyArray(_elementType);
        }
        Object value = _valueReader.read(p);
        if (p.nextToken() == JsonToken.END_ARRAY) {
            return b.singletonArray(_elementType, value);
        }
        b = b.start().add(value);
        do {
            b = b.add(_valueReader.read(p));
        } while (p.nextToken() != JsonToken.END_ARRAY);
        return b.buildArray(_elementType);
    }
}
