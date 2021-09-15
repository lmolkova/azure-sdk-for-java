// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JSONReader
{
    protected final ReaderCache _readerLocator;

    public JSONReader(ReaderCache loc)
    {
        _readerLocator = loc;
    }

    public Object readValue(JsonParser parser) throws IOException {
        return AnyReader.std.read(parser);
    }

    @SuppressWarnings("unchecked")
    public <T> T readBean(Class<T> type, JsonParser parser) throws IOException {
        final Object ob = _readerLocator.findReader(type)
            .read(parser);
        return (T) ob;
    }

}
