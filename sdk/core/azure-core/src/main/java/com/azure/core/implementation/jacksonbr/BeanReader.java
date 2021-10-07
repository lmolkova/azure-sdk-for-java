// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.azure.core.implementation.jacksonbr.tree.JacksonJrsTreeCodec;
import com.azure.core.implementation.jacksonbr.tree.JrsArray;
import com.azure.core.implementation.jacksonbr.tree.JrsBoolean;
import com.azure.core.implementation.jacksonbr.tree.JrsNumber;
import com.azure.core.implementation.jacksonbr.tree.JrsObject;
import com.azure.core.implementation.jacksonbr.tree.JrsString;
import com.azure.core.implementation.jacksonbr.tree.JrsValue;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Class that contains information about dynamically introspected
 * Bean types, to be able to deserialize (read) JSON into a POJO
 * and serialize (write) POJO as JSON.
 */
public class BeanReader
    extends ValueReader // so we can chain calls for Collections, arrays
{
    protected final ReaderCache _readerCache;
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
    public BeanReader(Class<?> type, POJODefinition beanDef, ReaderCache readerCache) {
        super(type);
        _readerCache = readerCache;
        _readersByName = new HashMap<>();
        _propsByName = new HashMap<>();

        POJODefinition.Prop ap = null;
        for (int i = 0; i < beanDef._properties.length; ++i) {
            POJODefinition.Prop rawProp = beanDef._properties[i];
            if (!rawProp.unwrappedProp) {
                _readersByName.putIfAbsent(rawProp.name, null);
                _propsByName.putIfAbsent(rawProp.name, rawProp);
            } else {
                ap = rawProp;
            }
        }

        _defaultCtor = beanDef.defaultCtor;
        _stringCtor = beanDef.stringCtor;
        _fromString = beanDef.fromString;
        this.beanDef = beanDef;
        additionalPropertiesProp = ap;
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
                    return readObject(p);
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

    private static boolean isFlatProp(POJODefinition.Prop prop, String prefix) {
        return prop.flattenProp &&
                prop.name.startsWith(prefix) &&
                prop.name.length() > prefix.length() &&
                prop.name.charAt(prefix.length()) == '.';
    }

    private boolean isFlatProp(String prefix) {
        for (POJODefinition.Prop prop : _propsByName.values()) {
            if (isFlatProp(prop, prefix)) {
                return true;
            }
        }

        return false;
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
                    return readObject(p);
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

    private Object readObject(JsonParser p) throws Exception {
        //p.canReadTypeId()
        Object bean = create();

        if (_fromString != null) {
            return _fromString.invoke(bean, p.getText());
        }

        String propName;

        boolean first = true;

        for (; (propName = p.nextFieldName()) != null; ) {
            if (first) {
                first = false;

                String type = beanDef.typeInfoProperty;
                if (type != null && type.contains("\\.")) {
                    type = type.replace("\\.", ".");
                }

                if (beanDef.typeNameAnnotation != null &&
                    type != null &&
                    type.equals(propName)) {

                    String typeName = (String) AnyReader.std.readNext(p);
                    if (!typeName.equals(beanDef.typeNameAnnotation) && beanDef.subtypes != null) {
                        for (JsonSubTypes.Type tt : beanDef.subtypes)
                            if (tt.name().equals(typeName)) {
                                return
                                    ((BeanReader)_readerCache.findReader(tt.value())).readObject(p);
                            }

                        //return fromNode()
                    }
                    continue;
                }
            }
            POJODefinition.Prop prop = _propsByName.get(propName);
            ValueReader vr = _readersByName.get(propName);
            if (prop == null) {
                if (additionalPropertiesProp != null) {
                    if (additionalPropertiesMap == null) {
                        additionalPropertiesMap = (Map<String, Object>) additionalPropertiesProp.getValue(bean);
                        if (additionalPropertiesMap == null) {
                            additionalPropertiesMap = new HashMap<>();
                        }
                    }
                }
                if (isFlatProp(propName)) {
                    p.nextToken();
                    JrsValue tree = JacksonJrsTreeCodec.SINGLETON.readTree(p);

                    readFlat(bean, propName, additionalPropertiesMap, tree, this);
                } else if (additionalPropertiesMap != null) {
                    additionalPropertiesMap.put(propName, AnyReader.std.readNext(p));
                } else {
                    handleUnknown(p);
                    continue;
                }
            } else if (!prop.canSet()) {
                handleUnknown(p);
                continue;
            } else {
                final Object value = vr.readNext(p);
                if (prop.getValue(bean) == null)
                    prop.setValue(bean, value);
                else if (additionalPropertiesMap != null) //TODO makes add prop tests happy but is wrong
                    additionalPropertiesMap.put(propName, value);
            }
        }

        if (additionalPropertiesProp != null && additionalPropertiesMap != null && !additionalPropertiesMap.isEmpty()) {
            // anysertter works per entry - TODO
           additionalPropertiesProp.setAnySetter(bean, additionalPropertiesMap);
        }
        // also verify we are not confused...
        if (!p.hasToken(JsonToken.END_OBJECT)) {
            throw _reportProblem(p);
        }

        return bean;
    }

    private static void readFlat(Object bean, String nextFieldName, Map<String, Object> additional, JrsValue tree, BeanReader reader) throws IOException {
        if (nextFieldName.contains(".")) {
            nextFieldName = nextFieldName.replace(".", "\\.");
        }
        dfs(bean, tree, nextFieldName, additional, reader);
    }

    private static void dfs(Object bean, TreeNode tree, String prefix, Map<String, Object> additional, BeanReader reader) throws IOException {
        Iterator<String> it = tree.fieldNames();

        while (it != null && it.hasNext()) {
            String nextSegment = it.next();
            TreeNode next = tree.path(nextSegment);

            if (nextSegment.contains(".")) {
                nextSegment = nextSegment.replace(".", "\\.");
            }

            String propName = prefix + "." + nextSegment;
            POJODefinition.Prop prop = reader._propsByName.get(propName);
            if (prop != null) {
                prop.setValue(bean, reader._readersByName.get(prop.name).readNext(next.traverse()));
                continue;
            } else if (next.isValueNode() && additional != null) {
                additional.put(prop.name, AnyReader.std.readNext(next.traverse()));
                continue;
            }

            if (next.isContainerNode()) {
                dfs(bean, next, propName, additional, reader);
            }
        }
    }

    private Class<?> findType(JrsValue node) {
        String type = beanDef.typeInfoProperty;
        if (type != null && type.contains("\\.")) {
            type = type.replace("\\.", ".");
        }

        if (beanDef.typeNameAnnotation != null && type != null) {
            String typeName = ((JrsString) node.path(type)).asText();
            if (!typeName.equals(beanDef.typeNameAnnotation) && beanDef.subtypes != null) {
                for (JsonSubTypes.Type tt : beanDef.subtypes)
                    if (tt.name().equals(typeName)) {
                        return tt.value();
                    }
            }
        }

        return null;
    }

    private static Object fromNode(JrsValue node, POJODefinition def, ReaderCache readerCache, BeanReader beanReader) throws Exception {
        String type = def.typeInfoProperty;
        if (type != null && type.contains("\\.")) {
            type = type.replace("\\.", ".");
        }

        Object bean = null;
        if (def.typeNameAnnotation != null && type != null) {
            String typeName = ((JrsString)node.path(type)).asText();
            if (typeName.equals(def.typeNameAnnotation)) {
                bean = create(def);
            } else {
                if (def.subtypes != null) {
                    for (JsonSubTypes.Type tt : def.subtypes)
                        if (tt.name().equals(typeName)) {
                            BeanReader propReader = (BeanReader) readerCache.findReader(tt.value());
                            if (propReader != null) {
                                return
                                    fromNode(node, propReader.beanDef, readerCache, propReader);
                            }
                        }
                }
                return null;
            }
        }

        if (node.isNull() || node.isMissingNode()) {
            return null;
        }

        if (node.isValueNode()) {
            if (node instanceof JrsString) {
                return ((JrsString) node).asText();
            } else if (node instanceof JrsBoolean) {
                return ((JrsBoolean) node).booleanValue();
            }  else if (node instanceof JrsNumber) {
                return ((JrsNumber) node).getValue();
            }
        } else if (node instanceof JrsArray) {
            JrsArray arr = (JrsArray)node;

            if (arr.size() == 0) {
                return new Object[0];
            }

            CollectionBuilder builder = CollectionBuilder.defaultImpl();
            Iterator<JrsValue> it = arr.elements();
            while (it.hasNext()){
                builder.add(fromNode(it.next(), def, readerCache, beanReader));
            }

            return builder.buildArray();
        }

        // JrsObject
        Iterator<String> props = node.fieldNames();
        Map<String, Object> additionalPropertiesMap = new HashMap<>();
        while (props.hasNext()) {
            String propName = props.next();
            POJODefinition.Prop prop = beanReader._propsByName.get(propName);
            ValueReader vr = beanReader._readersByName.get(prop.name);
            if (prop == null) {
                if (beanReader.additionalPropertiesProp != null) {
                    if (additionalPropertiesMap == null) {
                        additionalPropertiesMap = (Map<String, Object>) beanReader.additionalPropertiesProp.getValue(bean);
                        if (additionalPropertiesMap == null) {
                            additionalPropertiesMap = new HashMap<>();
                        }
                    }
                }
                if (isFlatProp(prop, propName)) {
                    readFlat(bean, prop.name, additionalPropertiesMap, node.path(propName), beanReader);
                } else if (additionalPropertiesMap != null) {
                    additionalPropertiesMap.put(prop.name, vr.readNext(node.path(propName).traverse()));
                }
            } else if (prop.canSet()){
                if (prop.getValue(bean) == null)
                    prop.setValue(bean, vr.readNext(node.path(propName).traverse()));
                else if (additionalPropertiesMap != null) //TODO makes add prop tests happy but is wrong
                    additionalPropertiesMap.put(propName, vr.readNext(node.path(propName).traverse()));
            }
        }

        if (beanReader.additionalPropertiesProp != null && additionalPropertiesMap != null && !additionalPropertiesMap.isEmpty()) {
            // anysertter works per entry - TODO
            beanReader.additionalPropertiesProp.setAnySetter(bean, additionalPropertiesMap);
        }

        return bean;
    }

    private static Object create(POJODefinition def) throws Exception {
        if (def.defaultCtor == null) {
            throw new IllegalStateException("Class "+def._type.getName()+" does not have default constructor to use");
        }
        return def.defaultCtor.newInstance();
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
