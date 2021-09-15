// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.core.io.SerializedString;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Key class, used as an efficient and accurate key
 * for locating per-class values from {@link java.util.Map}s.
 *<p>
 * The reason for having a separate key class instead of
 * directly using {@link Class} as key is mostly
 * to allow for redefining <code>hashCode</code> method --
 * for some strange reason, {@link Class} does not
 * redefine {@link Object#hashCode} and thus uses identity
 * hash, which is pretty slow. This makes key access using
 * {@link Class} unnecessarily slow.
 *<p>
 * Note: since class is not strictly immutable, caller must
 * know what it is doing, if changing field values.
 */
public final class ClassKey
{
    private String _className;

    private Class<?> _class;

    /**
     * Additional discriminator flags that may be used to allow
     * variations for same class.
     *
     * @since 2.8
     */
    private int _flags;

    /**
     * Let's cache hash code straight away, since we are
     * almost certain to need it.
     */
    private int _hashCode;

    public ClassKey()
    {
        _class = null;
        _className = null;
        _flags = _hashCode = 0;
    }

    public ClassKey(Class<?> clz, int flags)
    {
        _class = clz;
        _flags = flags;
        _className = clz.getName();
        _hashCode = _className.hashCode() + flags;
    }

    public ClassKey with(Class<?> clz, int flags)
    {
        _class = clz;
        _className = clz.getName();
        _hashCode = _className.hashCode() + flags;
        _flags = flags;
        return this;
    }

    /*
    /**********************************************************
    /* Standard methods
    /**********************************************************
     */

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;
        ClassKey other = (ClassKey) o;

        /* Is it possible to have different Class object for same name + class loader combo?
         * Let's assume answer is no: if this is wrong, will need to uncomment following functionality
         */
        /*
        return (other._flags == _flags)
            && (other._className.equals(_className))
            && (other._class.getClassLoader() == _class.getClassLoader());
        */
        return (other._flags == _flags) && (other._class == _class);
    }

    @Override public int hashCode() { return _hashCode; }

    @Override public String toString() { return _className; }

    public static final class BeanProperty
    {
        public final SerializedString name;
        public final int typeId;

        // TODO improve
        private final Field _field;
        private final Method _method;
        private final Object _value;

        public BeanProperty(int typeId, String n, Field field, Method method, Object value)
        {
            this.typeId = typeId;
            name = new SerializedString(n);
            _field = field;
            _method = method;
            _value = value;
        }

        public Object getValueFor(Object bean) throws IOException
        {
            try {
                if (_field != null) {
                    return _field.get(bean);
                } else if (_method != null) {
                    return _method.invoke(bean);
                } else {
                    return _value;
                }

            } catch (Exception e) {
                final String accessorDesc = (_method != null)
                        ? String.format("method %s()", _method.getName())
                        : String.format("field %s", _field.getName());
                throw new IOException(String.format(
                        "Failed to access property '%s' (using %s); exception (%s): %s",
                        name, e.getClass().getName(), accessorDesc, e.getMessage()), e);
            }
        }
    }

}
