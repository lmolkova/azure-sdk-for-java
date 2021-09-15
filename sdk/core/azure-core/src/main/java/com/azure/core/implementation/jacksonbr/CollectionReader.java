// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Reader for typed {@link Collection} values.
 */
public class CollectionReader extends ValueReader
{
    private static  CollectionBuilder _collectionBuilder = CollectionBuilder.defaultImpl();
    protected final Class<?> _collectionType;
    protected final ValueReader _valueReader;

    public CollectionReader(Class<?> t, ValueReader vr) {
        super(t);
        // some cleanup will be needed....
        if (t == Collection.class || t == List.class) { // since we default to ArrayList
            _collectionType = null;
        } else if (t == Set.class) {
            _collectionType = HashSet.class;
        } else if (t == SortedSet.class) {
            _collectionType = TreeSet.class;
        } else {
            _collectionType = t;
        }
        _valueReader = vr;
    }

    @Override
    public Object readNext(JsonParser p) throws IOException {
        if (p.nextToken() != JsonToken.START_ARRAY) {
            if (p.hasToken(JsonToken.VALUE_NULL)) {
                return null;
            }
            throw new IOException("Unexpected token "+p.currentToken()+"; should get START_ARRAY");
        }
        CollectionBuilder b = _collectionBuilder.newBuilder(_collectionType);
        if (p.nextToken() == JsonToken.END_ARRAY) {
            return b.emptyCollection();
        }
        Object value = _valueReader.read(p);
        if (p.nextToken() == JsonToken.END_ARRAY) {
            return b.singletonCollection(value);
        }
        b = b.start().add(value);
        do {
            b = b.add(_valueReader.read(p));
        } while (p.nextToken() != JsonToken.END_ARRAY);
        return b.buildCollection();
    }

    @Override
    public Object read(JsonParser p) throws IOException {
        CollectionBuilder b = _collectionBuilder.newBuilder(_collectionType);
        if (p.nextToken() == JsonToken.END_ARRAY) {
            return b.emptyCollection();
        }
        Object value = _valueReader.read(p);
        if (p.nextToken() == JsonToken.END_ARRAY) {
            return b.singletonCollection(value);
        }
        b = b.start().add(value);
        do {
            b = b.add(_valueReader.read(p));
        } while (p.nextToken() != JsonToken.END_ARRAY);
        return b.buildCollection();
    }
}
