package de.deepamehta.plugins.server.provider;

import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicModel;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;



@Provider
public class TopicCollectionProvider implements MessageBodyWriter<Collection<Topic>> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ****************************************
    // *** MessageBodyWriter Implementation ***
    // ****************************************



    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (genericType instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
            if (Collection.class.isAssignableFrom(type) && typeArgs.length == 1 && typeArgs[0] == Topic.class) {
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
    public long getSize(Collection<Topic> topics, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Collection<Topic> topics, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                        throws IOException, WebApplicationException {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(entityStream));
            TopicModel.topicsToJSON(topics).write(writer);
            writer.flush();
        } catch (Exception e) {
            throw new IOException("Writing message body failed (" + topics + ")", e);
        }
    }
}
