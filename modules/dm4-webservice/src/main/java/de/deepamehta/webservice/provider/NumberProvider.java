package de.deepamehta.webservice.provider;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;



@Provider
public class NumberProvider implements MessageBodyWriter<Number> {

    // -------------------------------------------------------------------------------------------------- Public Methods

    // *** MessageBodyWriter Implementation ***

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        // Note: unlike equals() isCompatible() ignores parameters like "charset" in "application/json;charset=UTF-8"
        return Number.class.isAssignableFrom(type) && mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public long getSize(Number number, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Number number, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                        throws IOException, WebApplicationException {
        try {
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(entityStream));
            writer.println(number);
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException("Writing message body failed", e);
        }
    }
}
