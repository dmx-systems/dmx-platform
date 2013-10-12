package de.deepamehta.plugins.topicmaps.provider;

import de.deepamehta.plugins.topicmaps.ClusterCoords;
import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONArray;

import java.io.InputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;



// Note: we can't rely on the generic ObjectProvider (Webservice module) as it
// intantiates a JSONObject but cluster coordinates are serialzed as JSON array.
// ### TODO: we could serialze cluster coordinates as a JSON object.
@Provider
public class ClusterCoordsProvider implements MessageBodyReader<ClusterCoords> {

    // *** MessageBodyReader Implementation ***

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        // Note: unlike equals() isCompatible() ignores parameters like "charset" in "application/json;charset=UTF-8"
        return type == ClusterCoords.class && mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public ClusterCoords readFrom(Class<ClusterCoords> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                                                                throws IOException, WebApplicationException {
        try {
            String json = JavaUtils.readText(entityStream);
            return new ClusterCoords(new JSONArray(json));
        } catch (Exception e) {
            throw new RuntimeException("Creating ClusterCoords from message body failed", e);
        }
    }
}
