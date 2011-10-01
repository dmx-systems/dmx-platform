package de.deepamehta.plugins.webservice.provider;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;



@Provider
public class TopicProvider implements MessageBodyReader<TopicModel>, MessageBodyWriter<Topic> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ****************************************
    // *** MessageBodyReader Implementation ***
    // ****************************************



    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        // Note: unlike equals() isCompatible() ignores parameters like "charset" in "application/json;charset=UTF-8"
        return type == TopicModel.class && mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public TopicModel readFrom(Class<TopicModel> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                                                                throws IOException, WebApplicationException {
        try {
            String json = JavaUtils.readText(entityStream);
            return new TopicModel(new JSONObject(json));
        } catch (Exception e) {
            throw new IOException("Creating TopicModel from message body failed", e);
        }
    }



    // ****************************************
    // *** MessageBodyWriter Implementation ***
    // ****************************************



    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        // Note: unlike equals() isCompatible() ignores parameters like "charset" in "application/json;charset=UTF-8"
        return Topic.class.isAssignableFrom(type) && mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public long getSize(Topic topic, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Topic topic, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                        throws IOException, WebApplicationException {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(entityStream));
            topic.toJSON().write(writer);
            writer.flush();
        } catch (Exception e) {
            throw new IOException("Writing message body failed (" + topic + ")", e);
        }
    }
}
