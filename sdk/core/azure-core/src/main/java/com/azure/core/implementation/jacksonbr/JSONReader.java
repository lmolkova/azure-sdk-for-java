// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.implementation.jacksonbr;

import com.azure.core.implementation.jacksonbr.type.ResolvedType;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

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
    public <T> T readBean(ResolvedType type, JsonParser parser) throws IOException {
        final Object ob = _readerLocator.findReader(type)
            .read(parser);
        return (T) ob;
    }

}
