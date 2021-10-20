package com.azure.core.implementation.jacksonbr;

import com.azure.core.implementation.jacksonbr.type.ResolvedType;
import com.azure.core.implementation.jacksonbr.type.TypeResolver;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.io.SegmentedStringWriter;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

public class XML {
    private static final XmlFactory xmlFactory = new XmlFactory();
    private static final WriterCache writerCache = new WriterCache();
    private static final ReaderCache readerCache = new ReaderCache();
    private static JSONWriter xmlWriter = new JSONWriter(writerCache);
    private static JSONReader xmlReader = new JSONReader(readerCache);
    private static final TypeResolver typeResolver = new TypeResolver();

    public static <T> void registerSerializer(Class<T> type, BeanWriter<T> instance) {
        writerCache.registerWriter(type, instance);
    }
    public static <T> void registerDeserializer(Class<T> type, ValueReader<T> instance) {
        readerCache.registerReader(typeResolver.resolve(type), instance);
    }

    public static String writeVal(Object value) throws IOException {
        SegmentedStringWriter sw = new SegmentedStringWriter(xmlFactory._getBufferRecycler());
        try {
            JsonGenerator g =  xmlFactory.createGenerator(sw);
            try {
                xmlWriter.writeValue(value, g);
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
        ByteArrayBuilder bb = new ByteArrayBuilder(xmlFactory._getBufferRecycler());
        try {
            JsonGenerator g =  xmlFactory.createGenerator(bb);
            try {
                xmlWriter.writeValue(value, g);
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
            JsonGenerator g =  xmlFactory.createGenerator(out);
            try {
                xmlWriter.writeValue(value, g);
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
        return readVal(str, typeResolver.resolve(type));
    }

    public static <T> T readVal(byte[] src, Class<T> type)
        throws IOException  {
        return readVal(src, typeResolver.resolve(type));
    }

    public static <T> T readVal(InputStream src, Class<T> type)
        throws IOException{
        return readVal(src, typeResolver.resolve(type));
    }

    public static <T> T readVal(String str, Type type) throws IOException {
        return readVal(str, typeResolver.resolve(type));
    }

    public static <T> T readVal(byte[] src, Type type)
        throws IOException  {
        return readVal(src, typeResolver.resolve(type));
    }

    public static <T> T readVal(InputStream src, Type type)
        throws IOException{
        return readVal(src, typeResolver.resolve(type));
    }

    public static <T> T readVal(String str, ResolvedType type) throws IOException {

        JsonParser p = xmlFactory.createParser(str);
        try {
            JsonToken t = p.currentToken();
            if (t == null) { // and then we must get something...
                t = p.nextToken();
                if (t == null) { // not cool is it?
                    return null;
                }
            }

            T result = xmlReader.readBean(type, p);

            return result;
        } catch (Exception e) {
            throw  e;
        } finally {
            _close(p);
        }
    }

    public static <T> T readVal(byte[] src, ResolvedType type)
        throws IOException
    {
        JsonParser p = xmlFactory.createParser(src);
        try {
            JsonToken t = p.currentToken();
            if (t == null) { // and then we must get something...
                t = p.nextToken();
                if (t == null) { // not cool is it?
                    return null;
                }
            }

            T result = xmlReader.readBean(type, p);

            return result;
        } catch (Exception e) {
            throw  e;
        } finally {
            _close(p);
        }
    }

    public static <T> T readVal(InputStream src, ResolvedType type)
        throws IOException
    {
        JsonParser p = xmlFactory.createParser(src);
        try {
            JsonToken t = p.currentToken();
            if (t == null) { // and then we must get something...
                t = p.nextToken();
                if (t == null) { // not cool is it?
                    return null;
                }
            }

            T result = xmlReader.readBean(type, p);

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
