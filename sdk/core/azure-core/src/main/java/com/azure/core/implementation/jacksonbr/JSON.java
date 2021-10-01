package com.azure.core.implementation.jacksonbr;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.io.SegmentedStringWriter;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.Closeable;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class JSON {
    private static final JsonFactory _jsonFactory = new JsonFactory();
    private static final WriterCache writerCache = new WriterCache();
    private static final ReaderCache readerCache = new ReaderCache();
    private static JSONWriter _jsonWriter = new JSONWriter(writerCache);
    private static JSONReader _jsonReader = new JSONReader(readerCache);

    public static <T> void registerSerializer(Class<T> type, BeanWriter<T> instance) {
        writerCache.registerWriter(type, instance);
    }
    public static <T> void registerDeserializer(Class<T> type, ValueReader<T> instance) {
        readerCache.registerReader(type, instance);
    }

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

    public static byte[] writeValAsBytes(Object value)
        throws IOException
    {
        ByteArrayBuilder bb = new ByteArrayBuilder(_jsonFactory._getBufferRecycler());
        try {
            JsonGenerator g =  _jsonFactory.createGenerator(bb);
            try {
                _jsonWriter.writeValue(value, g);
            } finally {
                _close(g);
            }

        } catch (JsonProcessingException e) { // to support [JACKSON-758]
            throw e;
        } catch (IOException e) { // shouldn't really happen, but is declared as possibility so:
            throw JsonMappingException.fromUnexpectedIOE(e);
        }
        byte[] result = bb.toByteArray();
        bb.release();
        return result;
    }

    public static void writeValAsStream(OutputStream out, Object value)
        throws IOException
    {
        try {
            JsonGenerator g =  _jsonFactory.createGenerator(out);
            try {
                _jsonWriter.writeValue(value, g);
            } finally {
                _close(g);
            }

        } catch (JsonProcessingException e) { // to support [JACKSON-758]
            throw e;
        } catch (IOException e) { // shouldn't really happen, but is declared as possibility so:
            throw JsonMappingException.fromUnexpectedIOE(e);
        }
    }

    public static <T> T readVal(String str, Class<T> type) throws IOException {

        JsonParser p = _jsonFactory.createParser(str);
        try {
            JsonToken t = p.currentToken();
            if (t == null) { // and then we must get something...
                t = p.nextToken();
                if (t == null) { // not cool is it?
                    return null;
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

    public static <T> T readVal(byte[] src, Class<T> type)
        throws IOException
    {
        JsonParser p = _jsonFactory.createParser(src);
        try {
            JsonToken t = p.currentToken();
            if (t == null) { // and then we must get something...
                t = p.nextToken();
                if (t == null) { // not cool is it?
                    return null;
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

    public static <T> T readVal(InputStream src, Class<T> type)
        throws IOException
    {
        JsonParser p = _jsonFactory.createParser(src);
        try {
            JsonToken t = p.currentToken();
            if (t == null) { // and then we must get something...
                t = p.nextToken();
                if (t == null) { // not cool is it?
                    return null;
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
