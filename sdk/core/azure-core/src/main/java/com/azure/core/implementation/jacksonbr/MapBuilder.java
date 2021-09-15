// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class that is used for constructing {@link Map}s
 * to map JSON Object values in.
 *<p>
 * Objects server both as "factories" for creating new builders (blueprint
 * style), and as actual builders. For each distinct read operation,
 * {@link #newBuilder} will be called at least once; this instance
 * may be used and reused multiple times, as calling {@link #start}
 * will reset the state so that more {@link List}s may be built.
 */
public abstract class MapBuilder
{
    protected final boolean _checkDups;

    /**
     * Optional {@link Map} implementation class, used when specific
     * implementation is desired.
     */
    protected final Class<?> _mapType;

    protected MapBuilder(Class<?> type) {
        _checkDups = false;
        _mapType = type;
    }

    /**
     * Factory method for getting a blueprint instance of the default
     * {@link MapBuilder} implementation.
     */
    public static MapBuilder defaultImpl() {
        return new Default(null);
    }

    public abstract MapBuilder newBuilder(int features);

    public abstract MapBuilder newBuilder(Class<?> mapImpl);

    public MapBuilder newBuilder() {
        return newBuilder();
    }

    public abstract MapBuilder start();

    public abstract MapBuilder put(String key, Object value);

    public abstract Map<String, Object> build();

    /**
     * Specialized method that is called when an empty list needs to
     * be constructed; this may be a new list, or an immutable shared
     * List, depending on implementation.
     *<p>
     * Default implementation simply calls:
     *<pre>
     *  start().build();
     *</pre>
     * which assumes that a builder has been constructed with {@link #newBuilder}
     */
    public Map<String, Object> emptyMap() throws IOException {
        return start().build();
    }

    /**
     * Specialized method that is called when an empty list needs to
     * be constructed; this may be a new list, or an immutable shared
     * List, depending on implementation.
     *<p>
     * Default implementation simply calls:
     *<pre>
     *  start().put(key, value).build();
     *</pre>
     */
    public Map<String, Object> singletonMap(String key, Object value) throws IOException {
        return start().put(key, value).build();
    }

    /*
    /**********************************************************
    /* Default implementation
    /**********************************************************
     */

    public static class Default extends MapBuilder
    {
        protected Map<String, Object> _current;

        protected Default(Class<?> type) {
            super(type);
        }

        @Override
        public MapBuilder newBuilder(int features) {
            return new Default(_mapType);
        }

        @Override
        public MapBuilder newBuilder(Class<?> mapImpl) {
            return new Default(mapImpl);
        }

        @Override
        public MapBuilder start() {
            // If this builder is "busy", create a new one...
            if (_current != null) {
                return newBuilder().start();
            }
            _current = _map(12);
            return this;
        }

        @Override
        public Map<String, Object> build() {
            Map<String, Object> result = _current;
            _current = null;
            return result;
        }

        @Override
        public MapBuilder put(String key, Object value) {
            if (_checkDups) {
                if (_current.containsKey(key)) {
                    // 14-Apr-2017, tatu: Note that choice of `IllegalArgumentException` is arbitrary
                    //   but not random: caller catches and re-packages it to give context
                    throw new IllegalArgumentException("Duplicate key (key '"+key+"')");
                }
            }
            _current.put(key, value);
            return this;
        }

        @Override
        public Map<String, Object> emptyMap() {
            return _map(4);
        }

        private final Map<String, Object> _map(int initialSize) {
            if (_mapType != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String,Object> m = (Map<String,Object>) _mapType.getDeclaredConstructor().newInstance();
                    return m;
                } catch (Exception e) {
                    Throwable t = e;
                    while (t.getCause() != null) {
                        t = t.getCause();
                    }
                    throw new IllegalArgumentException("Failed to create an instance of "
                            +_mapType.getName()+" ("+t.getClass().getName()+"): "+t.getMessage());

                }
            }

            return new HashMap<String, Object>(initialSize);
        }
    }
}
