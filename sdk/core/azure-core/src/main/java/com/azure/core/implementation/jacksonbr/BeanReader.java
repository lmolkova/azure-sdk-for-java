// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
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
    protected final Map<String, ValueReader> _readersByName;
    protected final Map<String, POJODefinition.Prop> _propsByName;

    private final POJODefinition beanDef;
    protected final Constructor<?> _defaultCtor;
    protected final Constructor<?> _stringCtor;
    protected final Method _fromString;
    private final POJODefinition.Prop additionalPropertiesProp;
    private Map<String, Object> additionalPropertiesMap;
    /**
     * Constructors used for deserialization use case
     */
    public BeanReader(Class<?> type, POJODefinition beanDef) {
        super(type);

        _readersByName = new HashMap<String, ValueReader>();
        _propsByName = new HashMap<String, POJODefinition.Prop>();
        for (int i = 0; i < beanDef._properties.length; ++i) {
            POJODefinition.Prop rawProp = beanDef._properties[i];
            _readersByName.put(rawProp.name, null);
            _propsByName.put(rawProp.name, rawProp);

        }

        _defaultCtor = beanDef.defaultCtor;
        _stringCtor = beanDef.stringCtor;
        _fromString = beanDef.fromString;
        this.beanDef = beanDef;
        additionalPropertiesProp = _propsByName.get("additionalProperties");
    }

    public Map<String,ValueReader> readersByName() { return _readersByName; }

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

                        POJODefinition.Prop prop = _propsByName.get(propName);
                        ValueReader vr = _readersByName.get(propName);
                        if (prop == null) {
                            if (additionalPropertiesProp != null) {
                                if (additionalPropertiesMap == null) {
                                    additionalPropertiesMap = (Map<String, Object>)additionalPropertiesProp.getValue(bean);
                                }

                                if (additionalPropertiesMap != null) {
                                    additionalPropertiesMap.put(propName, vr.readNext(p));
                                }
                            } else {
                                handleUnknown(p);
                                continue;
                            }
                        }

                        prop.setValue(bean, vr.readNext(p));
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
                        POJODefinition.Prop prop = _propsByName.get(propName);
                        if (prop == null) {
                            handleUnknown(p);
                            continue;
                        }
                        ValueReader vr = _readersByName.get(propName);

                        final Object value = vr.readNext(p);
                        prop.setValue(bean, value);
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
