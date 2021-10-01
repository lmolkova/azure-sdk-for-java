// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_STRING;
import static com.azure.core.implementation.jacksonbr.ValueLocatorUtils.SER_UNKNOWN;

final public class WriterCache {
    private final ConcurrentHashMap<Class<?>, BeanWriter> _typeToWriter;

    public WriterCache() {
        _typeToWriter = new ConcurrentHashMap<Class<?>, BeanWriter>(20, 0.75f, 2);
    }

    public <T> void registerWriter(Class<T> rawType, BeanWriter<T> writer) {
        _typeToWriter.computeIfAbsent(rawType, r -> writer);
    }

    public <T> BeanWriter<T> getOrAdd(Class<T> rawType) {
        return _typeToWriter.computeIfAbsent(rawType, r -> {
           POJODefinition beanDef = BeanPropertyIntrospector.instance().pojoDefinitionForSerialization(r);
           return new BeanWriterDefault(beanDef, this);
        });
    }
}
