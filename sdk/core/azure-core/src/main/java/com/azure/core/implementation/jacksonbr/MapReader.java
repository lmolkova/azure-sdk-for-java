// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.Map;

/**
 * Reader for typed {@link Map} values.
 */
public class MapReader extends ValueReader
{
    protected final Class<?> _mapType;
    protected final ValueReader _valueReader;
    private static  MapBuilder _mapBuilder = MapBuilder.defaultImpl();

    public MapReader(Class<?> mapType, ValueReader vr) {
        super(mapType);
        // Some caveats: drop type if it's generic enough (aka "don't care")
        _mapType = (mapType == Map.class) ? null : mapType;
        _valueReader = vr;
    }

    @Override
    public Object readNext(JsonParser p) throws IOException {
        if (p.nextToken() != JsonToken.START_OBJECT) {
            if (p.hasToken(JsonToken.VALUE_NULL)) {
                return null;
            }
            throw new IOException("Unexpected token "+p.currentToken()+"; should get START_OBJECT");
        }

        MapBuilder b = _mapBuilder.newBuilder(_mapType);
        String propName0 = p.nextFieldName();
        if (propName0 == null) {
            if (p.hasToken(JsonToken.END_OBJECT)) {
                return b.emptyMap();
            }
            throw _reportProblem(p);
        }
        Object value = _valueReader.readNext(p);
        String propName = p.nextFieldName();
        if (propName == null) {
            if (p.hasToken(JsonToken.END_OBJECT)) {
                return b.singletonMap(propName0, value);
            }
            throw _reportProblem(p);
        }
        try {
            b = b.start().put(propName0, value);
            while (true) {
                b = b.put(propName, _valueReader.readNext(p));
                propName = p.nextFieldName();
                if (propName == null) {
                    if (p.hasToken(JsonToken.END_OBJECT)) {
                        return b.build();
                    }
                    throw _reportProblem(p);
                }
            }
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public Object read(JsonParser p) throws IOException {
        MapBuilder b = _mapBuilder.newBuilder(_mapType);
        String propName0 = p.nextFieldName();
        if (propName0 == null) {
            if (p.hasToken(JsonToken.END_OBJECT)) {
                return b.emptyMap();
            }
            throw _reportProblem(p);
        }
        Object value = _valueReader.readNext(p);
        String propName = p.nextFieldName();
        if (propName == null) {
            if (p.hasToken(JsonToken.END_OBJECT)) {
                return b.singletonMap(propName0, value);
            }
            throw _reportProblem(p);
        }
        try {
            b = b.start().put(propName0, value);
            while (true) {
                b = b.put(propName, _valueReader.readNext(p));
                propName = p.nextFieldName();
                if (propName == null) {
                    if (p.hasToken(JsonToken.END_OBJECT)) {
                        return b.build();
                    }
                    throw _reportProblem(p);
                }
            }
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }
    }

    protected IOException _reportProblem(JsonParser p) {
        return new IOException("Unexpected token "+p.currentToken()+"; should get FIELD_NAME or END_OBJECT");
    }
}
