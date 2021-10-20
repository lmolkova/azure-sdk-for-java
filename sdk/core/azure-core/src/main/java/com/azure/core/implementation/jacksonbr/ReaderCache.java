// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.azure.core.implementation.TypeUtil;
import com.azure.core.implementation.jacksonbr.type.ResolvedType;
import com.azure.core.implementation.jacksonbr.type.TypeBindings;
import com.azure.core.implementation.jacksonbr.type.TypeResolver;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public final class ReaderCache
{
    private final static int MAX_CACHED_READERS = 500;
    private final TypeResolver _typeResolver;

    private final ConcurrentHashMap<ResolvedType, ValueReader> _knownReaders;
    private Map<ClassKey, ValueReader> _incompleteReaders;
    private final Object _readerLock;

    public ReaderCache() {
        _typeResolver = new TypeResolver();
        _knownReaders = new ConcurrentHashMap<ResolvedType, ValueReader>(10, 0.75f, 2);
        _readerLock = new Object();
    }

    public void registerReader(ResolvedType raw, ValueReader reader)
    {
        ValueReader vr = _knownReaders.get(raw);
        if (vr != null) {
            return;
        }
        // 15-Jun-2016, tatu: Let's limit maximum number of readers to prevent
        //   unbounded memory retention (at least wrt readers)
        if (_knownReaders.size() >= MAX_CACHED_READERS) {
            _knownReaders.clear();
        }
        _knownReaders.putIfAbsent(raw, reader);
        return;
    }

    public ValueReader findReader(Class<?> raw) {
        return findReader(_typeResolver.resolve(raw));
    }

    public ValueReader findReader(ResolvedType raw)
    {
        ValueReader vr = _knownReaders.get(raw);
        if (vr != null) {
            return vr;
        }
        vr = _createReader(raw, raw);
        // 15-Jun-2016, tatu: Let's limit maximum number of readers to prevent
        //   unbounded memory retention (at least wrt readers)
        if (_knownReaders.size() >= MAX_CACHED_READERS) {
            _knownReaders.clear();
        }
        _knownReaders.putIfAbsent(raw, vr);
        return vr;
    }

    private ValueReader _createReader(ResolvedType type, ResolvedType valueType)
    {
        if (type.erasedType() == Object.class) {
            return AnyReader.std;
        }
        if (type.isArray()) {
            return arrayReader(type, valueType);
        }
        if (type.erasedType().isEnum()) {
           return enumReader(type.erasedType());
        }
        if (Collection.class.isAssignableFrom(type.erasedType())) {
            return collectionReader(type.erasedType(), valueType);
        }

        if (Map.class.isAssignableFrom(type.erasedType())) {
            return mapReader(type.erasedType(), valueType);
        }

        int typeId = ValueLocatorUtils._findSimpleType(type.erasedType(), false);
        if (typeId > 0) {
            return new SimpleValueReader(type.erasedType(), typeId);
        }
        return beanReader(type);
    }

    private ValueReader arrayReader(ResolvedType type, ResolvedType valueType) {
        Class<?> elemType = valueType.elementType().erasedType();
        if (!elemType.isPrimitive()) {
            return new ArrayReader(type.erasedType(), elemType,
                    _createReader(valueType.elementType(), valueType.elementType()));
        }
        int typeId = ValueLocatorUtils._findSimpleType(type.erasedType(), false);
        if (typeId > 0) {
            return new SimpleValueReader(type.erasedType(), typeId);
        }
        throw new IllegalArgumentException("Deserialization of "+ type.getTypeName()+" not (yet) supported");
    }

    private ValueReader enumReader(Class<?> enumType) {
        Object[] enums = enumType.getEnumConstants();
        Map<String,Object> byName = new HashMap<String,Object>();
        for (Object e : enums) {
            byName.put(e.toString(), e);
        }
        return new EnumReader(enumType, enums, byName);
    }

    private ValueReader collectionReader(Class<?> contextType, Type collectionType)
    {
        ResolvedType t = _typeResolver.resolve(_bindings(contextType), collectionType);
        List<ResolvedType> params = t.typeParametersFor(Collection.class);
        return collectionReader(t.erasedType(), params.get(0));
    }

    private ValueReader collectionReader(Class<?> collectionType, ResolvedType valueType)
    {
        final Class<?> rawValueType = valueType.erasedType();
        if (Collection.class.isAssignableFrom(rawValueType)) {
            List<ResolvedType> params = valueType.typeParametersFor(Collection.class);
            return collectionReader(rawValueType, params.get(0));
        } else if (Map.class.isAssignableFrom(rawValueType)) {
            List<ResolvedType> params = valueType.typeParametersFor(Map.class);
            return mapReader(rawValueType, params.get(1));
        }

        final ValueReader valueReader = findReader(valueType);
        return new CollectionReader(collectionType, valueReader);
    }

    private ValueReader mapReader(Class<?> collectionType, Type mapType)
    {
        ResolvedType t = _typeResolver.resolve(_bindings(collectionType), mapType);
        List<ResolvedType> params = t.typeParametersFor(Map.class);
        return mapReader(t.erasedType(), params.get(1));
    }

    private ValueReader mapReader(Class<?> mapType, ResolvedType valueType)
    {
        final Class<?> rawValueType = valueType.erasedType();
        if (Collection.class.isAssignableFrom(rawValueType)) {
            List<ResolvedType> params = valueType.typeParametersFor(Collection.class);
            return collectionReader(rawValueType, params.get(0));
        } else if (Map.class.isAssignableFrom(rawValueType)) {
            List<ResolvedType> params = valueType.typeParametersFor(Map.class);
            return mapReader(rawValueType, params.get(1));
        }

        final ValueReader valueReader = findReader(rawValueType);
        return new MapReader(mapType, valueReader);
    }

    private ValueReader beanReader(ResolvedType type)
    {
        // NOTE: caller (must) handle custom reader lookup earlier, not done here

        final ClassKey key = new ClassKey(type.erasedType(), 0);
        synchronized (_readerLock) {
            if (_incompleteReaders == null) {
                _incompleteReaders = new HashMap<ClassKey, ValueReader>();
            } else { // perhaps it has already been resolved?
                ValueReader vr = _incompleteReaders.get(key);
                if (vr != null) {
                    return vr;
                }
            }
            final BeanReader def = _resolveBeanForDeser(type.erasedType(), BeanPropertyIntrospector.instance().pojoDefinitionForDeserialization(type.erasedType()));
            try {
                _incompleteReaders.put(key, def);
                for (Map.Entry<String, ValueReader> entry : def.readersByName().entrySet()) {
                    POJODefinition.Prop prop = def._propsByName.get(entry.getKey());
                    ParameterizedType pt = TypeUtil.createParameterizedType(prop.typeId, prop.getGenericType());
                    ValueReader vr = _knownReaders.get(_typeResolver.resolve(prop.getGenericType()));
                    if (vr != null) {
                        entry.setValue(vr);
                    } else {
                        entry.setValue(_createReader(_typeResolver.resolve(prop.typeId), _typeResolver.resolve(prop.getGenericType())));
                    }


                }
            } finally {
                _incompleteReaders.remove(key);
            }
            return def;
        }
    }

    private BeanReader _resolveBeanForDeser(Class<?> raw, POJODefinition beanDef)
    {
        Constructor<?> defaultCtor = beanDef.defaultCtor;
        Constructor<?> stringCtor = beanDef.stringCtor;
        Method fromString = beanDef.fromString;

        final boolean forceAccess = true;
        if (forceAccess) {
            if (defaultCtor != null) {
                defaultCtor.setAccessible(true);
            }
            if (stringCtor != null) {
                stringCtor.setAccessible(true);
            }
        }
        return new BeanReader(raw, beanDef, this);
    }

    private TypeBindings _bindings(Class<?> ctxt) {
        if (ctxt == null) {
            return TypeBindings.emptyBindings();
        }
        return TypeBindings.create(ctxt, (ResolvedType[]) null);
    }
}
