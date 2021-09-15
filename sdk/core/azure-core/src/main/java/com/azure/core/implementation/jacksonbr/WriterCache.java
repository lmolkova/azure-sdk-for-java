// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_STRING;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_UNKNOWN;

final public class WriterCache {
    private final ConcurrentHashMap<ClassKey, Integer> _knownSerTypes;
    private final CopyOnWriteArrayList<BeanWriter> _knownWriters;

    private Class<?> _prevClass;

    private int _prevType;

    public WriterCache() {
        _knownSerTypes = new ConcurrentHashMap<ClassKey, Integer>(20, 0.75f, 2);
        _knownWriters = new CopyOnWriteArrayList<BeanWriter>();
    }

    public BeanWriter getWriter(int index) {
        // for simplicity, let's allow caller to pass negative id as is
        if (index < 0) {
            index = -(index + 1);
        }
        return _knownWriters.get(index);
    }

    public final int findSerializationType(Class<?> raw) {
        if (raw == _prevClass) {
            return _prevType;
        }
        if (raw == String.class) {
            return SER_STRING;
        }
        ClassKey k = new ClassKey(raw, 0);
        int type;

        Integer I = _knownSerTypes.get(k);

        if (I == null) {
            type = ValueLocatorUtils._findSimpleType(raw, true);
            if (type == SER_UNKNOWN) {
                POJODefinition beanDef = BeanPropertyIntrospector.instance().pojoDefinitionForSerialization(raw);
                type = _registerWriter(raw, new BeanWriter(beanDef));
            }
            _knownSerTypes.put(new ClassKey(raw, 0), Integer.valueOf(type));
        } else {
            type = I.intValue();
        }
        _prevType = type;
        _prevClass = raw;
        return type;
    }

    private int _registerWriter(Class<?> rawType, BeanWriter valueWriter) {
        // Due to concurrent access, possible that someone might have added it
        synchronized (_knownWriters) {
            // Important: do NOT try to reuse shared instance; caller needs it
            ClassKey k = new ClassKey(rawType, 0);
            Integer I = _knownSerTypes.get(k);
            // if it was already concurrently added, we'll just discard this copy, return earlier
            if (I != null) {
                return I.intValue();
            }
            // otherwise add at the end, use -(index+1) as id
            _knownWriters.add(valueWriter);
            int typeId = -_knownWriters.size();
            _knownSerTypes.put(k, Integer.valueOf(typeId));
            return typeId;
        }
    }
}
