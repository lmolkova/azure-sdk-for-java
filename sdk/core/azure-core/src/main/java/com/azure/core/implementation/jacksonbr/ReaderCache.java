// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.azure.core.implementation.jacksonbr.type.ResolvedType;
import com.azure.core.implementation.jacksonbr.type.TypeBindings;
import com.azure.core.implementation.jacksonbr.type.TypeResolver;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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

    private final ConcurrentHashMap<ClassKey, ValueReader> _knownReaders;
    private Map<ClassKey, ValueReader> _incompleteReaders;
    private final Object _readerLock;

    public ReaderCache() {
        _typeResolver = new TypeResolver();
        _knownReaders = new ConcurrentHashMap<ClassKey, ValueReader>(10, 0.75f, 2);
        _readerLock = new Object();
    }

    public void registerReader(Class<?> raw, ValueReader reader)
    {
        ClassKey k = new ClassKey(raw, 0);
        ValueReader vr = _knownReaders.get(k);
        if (vr != null) {
            return;
        }
        // 15-Jun-2016, tatu: Let's limit maximum number of readers to prevent
        //   unbounded memory retention (at least wrt readers)
        if (_knownReaders.size() >= MAX_CACHED_READERS) {
            _knownReaders.clear();
        }
        _knownReaders.putIfAbsent(new ClassKey(raw, 0), reader);
        return;
    }

    public ValueReader findReader(Class<?> raw)
    {
        ClassKey k = new ClassKey(raw, 0);
        ValueReader vr = _knownReaders.get(k);
        if (vr != null) {
            return vr;
        }
        vr = _createReader(null, raw, raw);
        // 15-Jun-2016, tatu: Let's limit maximum number of readers to prevent
        //   unbounded memory retention (at least wrt readers)
        if (_knownReaders.size() >= MAX_CACHED_READERS) {
            _knownReaders.clear();
        }
        _knownReaders.putIfAbsent(new ClassKey(raw, 0), vr);
        return vr;
    }

    private ValueReader _createReader(Class<?> contextType, Class<?> type, Type genericType)
    {
        if (type == Object.class) {
            return AnyReader.std;
        }
        if (type.isArray()) {
            return arrayReader(contextType, type);
        }
        if (type.isEnum()) {
           return enumReader(type);
        }
        if (Collection.class.isAssignableFrom(type)) {
            return collectionReader(contextType, genericType);
        }

        if (Map.class.isAssignableFrom(type)) {
            return mapReader(contextType, genericType);
        }

        int typeId = ValueLocatorUtils._findSimpleType(type, false);
        if (typeId > 0) {
            return new SimpleValueReader(type, typeId);
        }
        return beanReader(type);
    }

    private ValueReader arrayReader(Class<?> contextType, Class<?> arrayType) {
        // TODO: maybe allow custom array readers?
        Class<?> elemType = arrayType.getComponentType();
        if (!elemType.isPrimitive()) {
            return new ArrayReader(arrayType, elemType,
                    _createReader(contextType, elemType, elemType));
        }
        int typeId = ValueLocatorUtils._findSimpleType(arrayType, false);
        if (typeId > 0) {
            return new SimpleValueReader(arrayType, typeId);
        }
        throw new IllegalArgumentException("Deserialization of "+arrayType.getName()+" not (yet) supported");
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
        final ValueReader valueReader;
        if (Collection.class.isAssignableFrom(rawValueType)) {
            List<ResolvedType> params = valueType.typeParametersFor(Collection.class);
            valueReader = collectionReader(rawValueType, params.get(0));
        } else if (Map.class.isAssignableFrom(rawValueType)) {
            List<ResolvedType> params = valueType.typeParametersFor(Map.class);
            valueReader = mapReader(rawValueType, params.get(1));
        } else {
            valueReader = findReader(rawValueType);
        }

        return new CollectionReader(collectionType, valueReader);
    }

    private ValueReader mapReader(Class<?> contextType, Type mapType)
    {
        ResolvedType t = _typeResolver.resolve(_bindings(contextType), mapType);
        List<ResolvedType> params = t.typeParametersFor(Map.class);
        return mapReader(t.erasedType(), params.get(1));
    }

    private ValueReader mapReader(Class<?> mapType, ResolvedType valueType)
    {
        final Class<?> rawValueType = valueType.erasedType();
        final ValueReader valueReader;
        if (Collection.class.isAssignableFrom(rawValueType)) {
            List<ResolvedType> params = valueType.typeParametersFor(Collection.class);
            valueReader = collectionReader(rawValueType, params.get(0));
        } else if (Map.class.isAssignableFrom(rawValueType)) {
            List<ResolvedType> params = valueType.typeParametersFor(Map.class);
            valueReader = mapReader(rawValueType, params.get(1));
        } else {
            valueReader = findReader(rawValueType);
        }
        return new MapReader(mapType, valueReader);
    }

    private ValueReader beanReader(Class<?> type)
    {
        // NOTE: caller (must) handle custom reader lookup earlier, not done here

        final ClassKey key = new ClassKey(type, 0);
        synchronized (_readerLock) {
            if (_incompleteReaders == null) {
                _incompleteReaders = new HashMap<ClassKey, ValueReader>();
            } else { // perhaps it has already been resolved?
                ValueReader vr = _incompleteReaders.get(key);
                if (vr != null) {
                    return vr;
                }
            }
            final BeanReader def = _resolveBeanForDeser(type, BeanPropertyIntrospector.instance().pojoDefinitionForDeserialization(type));
            try {
                _incompleteReaders.put(key, def);
                for (Map.Entry<String, BeanPropertyReader> entry : def.propertiesByName().entrySet()) {
                    BeanPropertyReader prop = entry.getValue();
                    ValueReader vr = _knownReaders.get(new ClassKey(prop.rawSetterType(), 0));
                    if (vr != null) {
                        entry.setValue(prop.withReader(vr));
                    } else {
                        entry.setValue(prop.withReader(_createReader(type, prop.rawSetterType(), prop.genericSetterType())));
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

        final POJODefinition.Prop[] rawProps = beanDef.properties();
        final int len = rawProps.length;
        final Map<String, BeanPropertyReader> propMap;

        if (len == 0) {
            propMap = Collections.emptyMap();
        } else {
            propMap = new HashMap<String, BeanPropertyReader>();
            final boolean useFields = true;
            for (int i = 0; i < len; ++i) {
                POJODefinition.Prop rawProp = rawProps[i];
                Method m = rawProp.setter;
                Field f = useFields ? rawProp.field : null;

                if (m != null) {
                    if (forceAccess) {
                        m.setAccessible(true);
                    } else if (!Modifier.isPublic(m.getModifiers())) {
                        // access to non-public setters must be forced to be usable:
                        m = null;
                    }
                }
                // if no setter, field would do as well
                if (m == null) {
                    if (f == null) {
                        continue;
                    }
                    // fields should always be public, but let's just double-check
                    if (forceAccess) {
                        f.setAccessible(true);
                    } else if (!Modifier.isPublic(f.getModifiers())) {
                        continue;
                    }
                }

                propMap.put(rawProp.name, new BeanPropertyReader(rawProp.name, f, m));
            }
        }
        return new BeanReader(raw, propMap, defaultCtor, stringCtor, fromString);
    }

    private TypeBindings _bindings(Class<?> ctxt) {
        if (ctxt == null) {
            return TypeBindings.emptyBindings();
        }
        return TypeBindings.create(ctxt, (ResolvedType[]) null);
    }
}
