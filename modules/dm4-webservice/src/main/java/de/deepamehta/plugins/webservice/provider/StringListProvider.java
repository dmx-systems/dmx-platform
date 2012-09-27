package de.deepamehta.plugins.webservice.provider;

import de.deepamehta.core.util.DeepaMehtaUtils;
import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONArray;

import java.io.InputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;



@Provider
public class StringListProvider implements MessageBodyReader<List<String>> {

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ****************************************
    // *** MessageBodyReader Implementation ***
    // ****************************************



    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericType;
            Type[] typeArgs = pt.getActualTypeArguments();
            if (pt.getRawType() == List.class && typeArgs.length == 1 && typeArgs[0] == String.class) {
                // Note: unlike equals() isCompatible() ignores parameters
                // like "charset" in "application/json;charset=UTF-8"
                if (mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<String> readFrom(Class<List<String>> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                                                                throws IOException, WebApplicationException {
        try {
            String json = JavaUtils.readText(entityStream);
            return DeepaMehtaUtils.toList(new JSONArray(json));
        } catch (Exception e) {
            throw new IOException("Creating a list of strings from message body failed", e);
        }
    }
}
