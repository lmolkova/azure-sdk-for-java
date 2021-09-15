// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Class that contains information about dynamically introspected
 * Bean types, to be able to deserialize (read) JSON into a POJO
 * and serialize (write) POJO as JSON.
 */
public class BeanReader
    extends ValueReader // so we can chain calls for Collections, arrays
{
    protected final Map<String, BeanPropertyReader> _propsByName;

    //protected final Constructor<?> _creatorCtor;
    protected final Constructor<?> _defaultCtor;
    protected final Constructor<?> _stringCtor;
    protected final Method _fromString;
    /**
     * Constructors used for deserialization use case
     */
    public BeanReader(Class<?> type, Map<String, BeanPropertyReader> props,
                      Constructor<?> defaultCtor0, Constructor<?> stringCtor0, Method fromString0)
    {
        super(type);
        _propsByName = props;
        _defaultCtor = defaultCtor0;
        _stringCtor = stringCtor0;
        _fromString = fromString0;
    }

    public Map<String,BeanPropertyReader> propertiesByName() { return _propsByName; }

    public BeanPropertyReader findProperty(String name) {
        BeanPropertyReader prop = _propsByName.get(name);
        if (prop == null) {
            return _findAlias(name);
        }
        return prop;
    }

    private final BeanPropertyReader _findAlias(String name) {
        return _propsByName.get(name);
    }

    @Override
    public Object readNext(JsonParser p) throws IOException
    {
        JsonToken t = p.nextToken();
        try {
            switch (t) {
            case VALUE_NULL:
                return null;
            case VALUE_STRING:
                return create(p.getText());
            case START_OBJECT:
                {
                    Object bean = create();

                    if (_fromString != null) {
                        return _fromString.invoke(bean, p.getText());
                    }

                    String propName;

                    for (; (propName = p.nextFieldName()) != null; ) {
                        BeanPropertyReader prop = findProperty(propName);
                        if (prop == null) {
                            handleUnknown(p);
                            continue;
                        }
                        ValueReader vr = prop.getReader();
                        prop.setValueFor(bean, vr.readNext(p));
                    }
                    // also verify we are not confused...
                    if (!p.hasToken(JsonToken.END_OBJECT)) {
                        throw _reportProblem(p);
                    }

                    return bean;
                }
            default:
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to create an instance of "
                    +_valueType.getName()+" due to ("+e.getClass().getName()+"): "+e.getMessage(),
                    e);
        }
        throw new IOException("Can not create a "+_valueType.getName()+" instance out of "+_tokenDesc(p));
    }

    /**
     * Method used for deserialization; will read an instance of the bean
     * type using given parser.
     */
    @Override
    public Object read(JsonParser p) throws IOException
    {
        JsonToken t = p.getCurrentToken();

        try {
            switch (t) {
            case VALUE_NULL:
                return null;
            case VALUE_STRING:
                return create(p.getText());
            case START_OBJECT:
                {
                    Object bean = create();

                    if (_fromString != null) {
                        return _fromString.invoke(bean, p.getText());
                    }

                    String propName;

                    for (; (propName = p.nextFieldName()) != null; ) {
                        BeanPropertyReader prop = findProperty(propName);
                        if (prop == null) {
                            handleUnknown(p);
                            continue;
                        }
                        final Object value = prop.getReader().readNext(p);
                        prop.setValueFor(bean, value);
                    }
                    // also verify we are not confused...
                    if (!p.hasToken(JsonToken.END_OBJECT)) {
                        throw _reportProblem(p);
                    }

                    return bean;
                }
            default:
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(String.format(
                    "Failed to create an instance of %s due to (%s): %s",
                    _valueType.getName(), e.getClass().getName(), e.getMessage()));
        }
        throw new IOException(String.format("Can not create a %s instance out of %s",
                _valueType.getName(), _tokenDesc(p)));
    }

    protected Object create() throws Exception {
        if (_defaultCtor == null) {
            throw new IllegalStateException("Class "+_valueType.getName()+" does not have default constructor to use");
        }
        return _defaultCtor.newInstance();
    }

    protected Object create(String str) throws Exception {
        if (_stringCtor == null) {
            throw new IllegalStateException("Class "+_valueType.getName()+" does not have single-String constructor to use");
        }
        return _stringCtor.newInstance(str);
    }

    protected void handleUnknown(JsonParser parser) throws IOException {
        parser.nextToken();
        parser.skipChildren();
    }

    protected IOException _reportProblem(JsonParser p) {
        return new IOException("Unexpected token "+p.currentToken()+"; should get FIELD_NAME or END_OBJECT");
    }
}
