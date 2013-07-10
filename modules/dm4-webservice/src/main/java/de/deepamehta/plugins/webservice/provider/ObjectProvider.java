package de.deepamehta.plugins.webservice.provider;

import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.io.InputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;



@Provider
public class ObjectProvider implements MessageBodyReader<Object> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    // *** MessageBodyReader Implementation ***

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        // Note: unlike equals() isCompatible() ignores parameters like "charset" in "application/json;charset=UTF-8"
        return getJSONConstructor(type) != null && mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                           MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                                                                  throws IOException, WebApplicationException {
        try {
            String json = JavaUtils.readText(entityStream);
            return getJSONConstructor(type).newInstance(new JSONObject(json));
        } catch (Exception e) {
            throw new RuntimeException("Creating Object from message body failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private Constructor<?> getJSONConstructor(Class<?> type) {
        try {
            return type.getDeclaredConstructor(JSONObject.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
