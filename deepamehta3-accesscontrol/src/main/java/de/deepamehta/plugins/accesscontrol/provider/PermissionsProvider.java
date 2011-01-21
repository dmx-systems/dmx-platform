package de.deepamehta.plugins.accesscontrol.provider;

import de.deepamehta.core.util.JavaUtils;
import de.deepamehta.plugins.accesscontrol.model.Permissions;

import org.codehaus.jettison.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;



@Provider
public class PermissionsProvider implements MessageBodyReader<Permissions> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ****************************************
    // *** MessageBodyReader Implementation ***
    // ****************************************



    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        // logger.info("### mediaType=" + mediaType);
        // Note: unlike equals() isCompatible() ignores parameters like "charset" in "application/json;charset=UTF-8"
        return type == Permissions.class && mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public Permissions readFrom(Class<Permissions> type, Type genericType, Annotation[] annotations, MediaType mediaType, 
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        try {
            // logger.info("Reading Permissions object from request stream");
            String json = JavaUtils.readText(entityStream);
            return new Permissions(new JSONObject(json));
        } catch (Exception e) {
            throw new IOException("Error while reading a Permissions object from request stream", e);
        }
    }
}
