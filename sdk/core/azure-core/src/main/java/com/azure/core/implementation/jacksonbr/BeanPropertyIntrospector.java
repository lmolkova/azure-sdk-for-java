// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;

public class BeanPropertyIntrospector
{
    protected final static POJODefinition.Prop[] NO_PROPS = new POJODefinition.Prop[0];

    private final static BeanPropertyIntrospector INSTANCE = new BeanPropertyIntrospector();

    public BeanPropertyIntrospector() { }

    public static BeanPropertyIntrospector instance() { return INSTANCE; }

    public POJODefinition pojoDefinitionForDeserialization(Class<?> pojoType) {
        try {
            return _construct(pojoType);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Failed to introspect ClassDefinition for type '%s': %s",
                    pojoType.getName(), e.getMessage()), e);
        }

    }

    public POJODefinition pojoDefinitionForSerialization(Class<?> pojoType) {
        try {
            return _construct(pojoType);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format
                    ("Failed to introspect ClassDefinition for type '%s': %s",
                            pojoType.getName(), e.getMessage()), e);
        }
    }

    private POJODefinition _construct(Class<?> beanType)
    {
        Map<String, POJODefinition.PropBuilder> propsByName = new TreeMap<String, POJODefinition.PropBuilder>();
        _introspect(beanType, propsByName);

        Constructor<?> defaultCtor = null;
        Constructor<?> stringCtor = null;

        for (Constructor<?> ctor : beanType.getDeclaredConstructors()) {
            Class<?>[] argTypes = ctor.getParameterTypes();
            if (argTypes.length == 0) {
                defaultCtor = ctor;
            } else if (argTypes.length == 1) {
                Class<?> argType = argTypes[0];
                if (argType == String.class) {
                    stringCtor = ctor;
                } else {
                    continue;
                }
            }
        }

        Constructor<?> creatorCtor = null;
        for (Constructor<?> ctor : beanType.getDeclaredConstructors()) {
            JsonCreator creatorAnnotation = ctor.getAnnotation(JsonCreator.class);
            if (creatorAnnotation != null) {
                creatorCtor = ctor;
                break;
            }
        }

        final int len = propsByName.size();
        POJODefinition.Prop[] props;
        if (len == 0) {
            props = NO_PROPS;
        } else {
            props = new POJODefinition.Prop[len];
            int i = 0;
            for (POJODefinition.PropBuilder builder : propsByName.values()) {
                props[i++] = builder.build();
            }
        }
        return new POJODefinition(beanType, props, defaultCtor, stringCtor, null);
    }

    private static void _introspect(Class<?> currType, Map<String, POJODefinition.PropBuilder> props)
    {
        if (currType == null || currType == Object.class) {
            return;
        }
        // First, check base type
        _introspect(currType.getSuperclass(), props);

        JsonTypeInfo typeInfoAnnotation = currType.getAnnotation(JsonTypeInfo.class);
        if (typeInfoAnnotation != null) {

            if (typeInfoAnnotation.use() != JsonTypeInfo.Id.NAME) {
                throw new UnsupportedOperationException("unsupported JsonTypeInfo.use " + typeInfoAnnotation.use().name());
            }

            if (typeInfoAnnotation.include() != JsonTypeInfo.As.PROPERTY) {
                throw new UnsupportedOperationException("unsupported JsonTypeInfo.include " + typeInfoAnnotation.include().name());
            }

            JsonTypeName nameAnnotation =  currType.getAnnotation(JsonTypeName.class);
            _propFrom(props, typeInfoAnnotation.property()).withValue(nameAnnotation.value());
        }

        for (Field f : currType.getDeclaredFields()) {
            JsonProperty annotation = f.getAnnotation(JsonProperty.class);
            if (annotation != null) {
                String name = annotation.value();
                if (name == null || name.isEmpty()) {
                    name = f.getName();
                }

                _propFrom(props, name).withField(f);
            }
        }

        if (props.isEmpty()) {
            // look for jsonValue

            for (Field f : currType.getDeclaredFields()) {
                if (f.isAnnotationPresent(JsonValue.class)) {
                    _propFrom(props, "").withField(f);
                    return;
                }
            }

            for (Method m : currType.getDeclaredMethods()) {
                if (m.isAnnotationPresent(JsonValue.class) && m.getParameterCount() == 0) {
                    _propFrom(props, "").withGetter(m);
                    return;
                }
            }
        }
    }

    private static POJODefinition.PropBuilder _propFrom(Map<String, POJODefinition.PropBuilder> props, String name) {
        POJODefinition.PropBuilder prop = props.get(name);
        if (prop == null) {
            prop = POJODefinition.Prop.builder(name);
            props.put(name, prop);
        }
        return prop;
    }
}
