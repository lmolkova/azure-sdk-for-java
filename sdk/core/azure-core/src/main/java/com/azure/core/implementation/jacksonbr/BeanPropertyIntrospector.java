// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.azure.core.annotation.JsonFlatten;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.IOException;
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
                props[i] = builder.build();
                props[i].makeAccessible();
                i++;
            }
        }

        JsonSubTypes subTypes = beanType.getAnnotation(JsonSubTypes.class);
        JsonTypeInfo typeInfoAnnotation = beanType.getAnnotation(JsonTypeInfo.class);
        JsonTypeName nameAnnotation =  beanType.getAnnotation(JsonTypeName.class);
        if (typeInfoAnnotation != null) {

            if (typeInfoAnnotation.use() != JsonTypeInfo.Id.NAME) {
                throw new UnsupportedOperationException("unsupported JsonTypeInfo.use " + typeInfoAnnotation.use().name());
            }

            if (typeInfoAnnotation.include() != JsonTypeInfo.As.PROPERTY) {
                throw new UnsupportedOperationException("unsupported JsonTypeInfo.include " + typeInfoAnnotation.include().name());
            }
        }

        return new POJODefinition(beanType,
            subTypes == null ? null : subTypes.value(),
            nameAnnotation == null ? null : nameAnnotation.value(),
            typeInfoAnnotation == null ? null : typeInfoAnnotation.property(),
            props,
            defaultCtor,
            stringCtor,
            null);
    }

    private static void _introspect(Class<?> currType, Map<String, POJODefinition.PropBuilder> props)
    {
        if (currType == null || currType == Object.class) {
            return;
        }
        // First, check base type
        _introspect(currType.getSuperclass(), props);

        JsonFlatten flattenClass = currType.getAnnotation(JsonFlatten.class);


        for (Field f : currType.getDeclaredFields()) {
            JsonIgnore ignore = f.getAnnotation(JsonIgnore.class);
            if (ignore != null) {
                continue;
            }

            JsonFlatten flattenProp = f.getAnnotation(JsonFlatten.class);
            boolean flatten = flattenClass != null || flattenProp != null;
            JsonProperty annotation = f.getAnnotation(JsonProperty.class);
            if (annotation != null) {

                String name = annotation.value();
                if (name == null || name.isEmpty()) {
                    name = f.getName();
                }

                boolean unwrap = false;
                if (f.getName().equals("additionalProperties") && f.getType().isAssignableFrom(Map.class) && annotation.value().isEmpty()) {
                    // todo anygetter

                    unwrap = true;
                }
                _propFrom(props, name, f.getName()).withField(f).makeUnwrapped(unwrap).makeFlat(flatten);
            }
        }

        if (props.isEmpty()) {
            // look for jsonValue

            for (Field f : currType.getDeclaredFields()) {
                if (f.isAnnotationPresent(JsonValue.class)) {
                    _propFrom(props, "", "").withField(f);
                    return;
                }
            }

            for (Method m : currType.getDeclaredMethods()) {
                if (m.isAnnotationPresent(JsonValue.class) && m.getParameterCount() == 0) {
                    _propFrom(props, "", "").withGetter(m);
                    return;
                }
            }
        }

        for (Method m : currType.getDeclaredMethods()) {
            JsonAnyGetter anyGetter = m.getAnnotation(JsonAnyGetter.class);
            if (anyGetter != null && m.getReturnType().isAssignableFrom(Map.class) && m.getParameterCount() == 0) {
                String methodName = m.getName();
                String fieldName = methodName;
                if (methodName.startsWith("get")) {
                    fieldName = methodName.substring(3);
                }

                _propFrom(props, fieldName, fieldName).withGetter(m).makeUnwrapped(true).makeFlat(flattenClass != null);
            }

            JsonAnySetter anySetter = m.getAnnotation(JsonAnySetter.class);
            if (anySetter != null && m.getParameterCount() == 2 &&
                m.getParameterTypes()[0].isAssignableFrom(String.class)) {
                String methodName = m.getName();
                String fieldName = methodName;
                if (methodName.startsWith("set") ||methodName.startsWith("add")) {
                    fieldName = methodName.substring(3);
                    if (Character.isUpperCase(fieldName.charAt(0))) {
                        fieldName =Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
                    }
                }
                _propFrom(props, fieldName, fieldName).withSetter(m).makeUnwrapped(true).makeFlat(flattenClass != null);
            }
        }
    }

    private static POJODefinition.PropBuilder _propFrom(Map<String, POJODefinition.PropBuilder> props, String name, String uniqueName) {
        POJODefinition.PropBuilder prop = props.get(name);
        if (prop == null) {
            prop = POJODefinition.Prop.builder(name, uniqueName);
            props.put(uniqueName, prop);
        }
        return prop;
    }
}
