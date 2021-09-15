package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.io.SegmentedStringWriter;

import java.io.Closeable;
import java.io.IOException;

public class JSON {
    private static final JsonFactory _jsonFactory = new JsonFactory();
    private static final WriterCache writerCache = new WriterCache();
    private static final ReaderCache readerCache = new ReaderCache();
    private static JSONWriter _jsonWriter = new JSONWriter(writerCache);
    private static JSONReader _jsonReader = new JSONReader(readerCache);

    public static String writeVal(Object value) throws IOException {
        SegmentedStringWriter sw = new SegmentedStringWriter(_jsonFactory._getBufferRecycler());
        try {
            JsonGenerator g =  _jsonFactory.createGenerator(sw);
            try {
                _jsonWriter.writeValue(value, g);
            } finally {
                _close(g);
            }
        } catch (JsonProcessingException e) {
            throw e;
        }
        return sw.getAndClear();
    }


    public static <T> T readVal(String str, Class<T> type) throws IOException {

        JsonParser p = _jsonFactory.createParser(str);
        try {
            JsonToken t = p.currentToken();
            if (t == null) { // and then we must get something...
                t = p.nextToken();
                if (t == null) { // not cool is it?
                    throw new IOException("No content to map due to end-of-input");
                }
            }

            T result = _jsonReader.readBean(type, p);

            return result;
        } catch (Exception e) {
            throw  e;
        } finally {
            _close(p);
        }
    }

    protected static void _close(Closeable cl) {
        if (cl != null) {
            try {
                cl.close();
            } catch (IOException ioe) { }
        }
    }
}
