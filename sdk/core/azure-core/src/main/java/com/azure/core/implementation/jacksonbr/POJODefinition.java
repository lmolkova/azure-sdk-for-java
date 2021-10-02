// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Definition of a single Bean-style Java class, without assumptions
 * on usage for serialization or deserialization, used as input
 * for constructing bean readers and writers.
 *
 * @since 2.8
 */
public class POJODefinition<T>
{
    protected final Class<T> _type;

    protected final Prop<?>[] _properties;

    public final Constructor<?> defaultCtor;
    public final Constructor<?> stringCtor;
    public final Method fromString;

    public POJODefinition(Class<T> type, Prop<?>[] props,
                          Constructor<T> defaultCtor0,
                          Constructor<T> stringCtor0,
                          //Constructor<?> creatorCtor0,
                          Method fromString0)
    {
        _type = type;
        _properties = props;
        defaultCtor = defaultCtor0;
        stringCtor = stringCtor0;
        //creatorCtor = creatorCtor0;
        fromString = fromString0;
    }

    public Prop<?>[] properties() {
        return _properties;
    }

    /*
    /**********************************************************************
    /* Helper class for containing property definitions
    /**********************************************************************
     */

    public static final class Prop<T>
    {
        public final boolean unwrappedProp;
        public final String name;
        public final SerializedString serializedName;
        public final Class<T> typeId;

        public final Object getValue(T obj) throws IOException {
            try {
                if (field != null) {
                    return field.get(obj);
                } else if (getter != null) {
                    return getter.invoke(obj);
                } else if (value != null) {
                    return value;
                }

            } catch (Exception e) {
                final String accessorDesc = (getter != null)
                    ? String.format("method %s()", getter.getName())
                    : String.format("field %s", field.getName());
                throw new IOException(String.format(
                    "Failed to access property '%s' (using %s); exception (%s): %s",
                    name, e.getClass().getName(), accessorDesc, e.getMessage()), e);
            }

            throw new IOException(String.format(
                "Failed to access property '%s'", name));
        }

        public void setValue(T obj, Object value) throws IOException
        {
            try {
                if (field != null) {
                    field.set(obj, value);
                } else if (setter != null) {
                    setter.invoke(obj, value);
                } else {
                    throw new IOException(String.format( "Failed to set property '%s'", name));
                }
            } catch (Exception e) {
                Throwable t = e;
                if (t instanceof InvocationTargetException) {
                    t = t.getCause();
                }
                final String valueTypeDesc = (value == null) ? "NULL" : value.getClass().getName();
                throw new IOException(String.format(
                    "Failed to set property '%s' (raw type %s) to value of type %s; exception (%s): %s",
                    name, typeId.getName(), valueTypeDesc, e.getClass().getName(), t.getMessage()),
                    t);
            }
        }

        private final Field field;
        private final Method setter, getter;
        private final String value;

        public void makeAccessible() {
            if (setter != null) {
                setter.setAccessible(true);
            }
            if (getter != null) {
                getter.setAccessible(true);
            }
            if (field != null) {
                field.setAccessible(true);
            }
        }

        public static Class<?> getType(Method getter, Field field, Object value) {
            if (getter != null) {
                return getter.getReturnType();
            }

            if (field != null) {
                return field.getType();
            }

            if (value != null) {
                return value.getClass();
            }

            return Object.class; //TODO exception
        }


        public Type getGenericType() {
            if (getter != null) {
                return getter.getGenericReturnType();
            }

            if (field != null) {
                return field.getGenericType();
            }

            if (value != null) {
                return value.getClass();
            }

            return Object.class; //TODO exception
        }

        public static Prop<?> create(String n, Field f,
                              Method setter0, Method getter0, String value0, boolean unwrapped) {
            return new Prop<Object>(n, f, setter0, getter0, value0, unwrapped);
        }

        @SuppressWarnings("unchecked")
        private Prop(String n, Field f,
                Method setter0, Method getter0, String value0, boolean unwrapped)
        {
            name = n;
            serializedName = new SerializedString(n);
            field = f;
            setter = setter0;
            getter = getter0;
            value = value0;
            typeId =  (Class<T>)getType(getter0, field, value0);
            unwrappedProp = unwrapped;
        }

        public static PropBuilder builder(String name) {
            return new PropBuilder(name);
        }

        public boolean hasSetter() {
            return (setter != null) || (field != null);
        }
    }

    static final class PropBuilder {
        private final String _name;

        private Field _field;
        private Method _setter, _getter;
        private String _value;
        private boolean unwrapped = false;

        public PropBuilder(String name) {
            _name = name;
        }

        public Prop<?> build() {
            return Prop.create(_name, _field, _setter, _getter, _value, unwrapped);
        }

        public PropBuilder withField(Field f) {
            _field = f;
            return this;
        }

        public PropBuilder withSetter(Method m) {
            _setter = m;
            return this;
        }

        public PropBuilder withGetter(Method m) {
            _getter = m;
            return this;
        }

        public PropBuilder withValue(String value) {
            _value = value;
            return this;
        }

        public PropBuilder makeUnwrapped() {
            unwrapped = true;
            return this;
        }



    }
}

