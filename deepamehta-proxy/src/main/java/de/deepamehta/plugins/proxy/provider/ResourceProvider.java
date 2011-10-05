package de.deepamehta.plugins.proxy.provider;

import de.deepamehta.plugins.proxy.model.Resource;

import org.apache.commons.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;



@Provider
public class ResourceProvider implements MessageBodyWriter<Resource> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ****************************************
    // *** MessageBodyWriter Implementation ***
    // ****************************************



    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Resource.class;
    }

    @Override
    public long getSize(Resource resource, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Resource resource, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                        throws IOException, WebApplicationException {
        try {
            if (resource.dir != null) {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(entityStream));
                httpHeaders.putSingle("Content-Type", "application/json");
                resource.dir.toJSON().write(writer);
                writer.flush();
            } else {
                if (resource.mediaType != null) {
                    httpHeaders.putSingle("Content-Type", resource.mediaType);
                }
                if (resource.size != 0) {
                    httpHeaders.putSingle("Content-Length", resource.size);
                }
                // ### httpHeaders.putSingle("Content-Disposition", "inline");
                IOUtils.copy(resource.uri.openStream(), entityStream);
                entityStream.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Writing message body failed (" + resource + ")", e);
        }
    }
}
