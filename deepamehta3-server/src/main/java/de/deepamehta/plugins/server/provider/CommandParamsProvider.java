package de.deepamehta.plugins.server.provider;

import de.deepamehta.core.model.CommandParams;
import de.deepamehta.core.util.JavaUtils;
import de.deepamehta.core.util.JSONHelper;
import de.deepamehta.core.util.UploadedFile;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import org.codehaus.jettison.json.JSONObject;

import java.io.InputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;



@Provider
public class CommandParamsProvider implements MessageBodyReader<CommandParams> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Context HttpServletRequest request;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ****************************************
    // *** MessageBodyReader Implementation ***
    // ****************************************



    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        // Note: unlike equals() isCompatible() ignores parameters like "charset" in "application/json;charset=UTF-8"
        return type == CommandParams.class && (mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE) ||
                                               mediaType.isCompatible(MediaType.MULTIPART_FORM_DATA_TYPE));
    }

    @Override
    public CommandParams readFrom(Class<CommandParams> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                                                                throws IOException, WebApplicationException {
        try {
            if (mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
                String json = JavaUtils.readText(entityStream);
                return new CommandParams(JSONHelper.toMap(new JSONObject(json)));
            } else if (mediaType.isCompatible(MediaType.MULTIPART_FORM_DATA_TYPE)) {
                return new CommandParams(multiPartToMap());
            } else {
                throw new RuntimeException("Unexpected media type: " + mediaType);
            }
        } catch (Exception e) {
            throw new IOException("Creating CommandParams from message body failed", e);
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private Map<String, Object> multiPartToMap() {
        try {
            Map<String, Object> params = new HashMap();
            //
            FileItemFactory factory = new DiskFileItemFactory();        // create a factory for disk-based file items
            ServletFileUpload upload = new ServletFileUpload(factory);  // create a new file upload handler
            List<FileItem> items = upload.parseRequest(request);        // parse the request
            // FIXME: check if we can use a FileUpload low-level method to parse the request body instead of the
            // entire request. Advantage: a) no need to inject the HttpServletRequest, b) no double work as the
            // request is already parsed by jersey, c) no dependency on servlet-api.
            logger.info("### Parsing multipart/form-data request (" + items.size() + " parts)");
            for (FileItem item : items) {
                String fieldName = item.getFieldName();
                if (item.isFormField()) {
                    String value = item.getString();
                    logger.info("### \"" + fieldName + "\" => \"" + value + "\"");
                    params.put(fieldName, value);
                } else {
                    UploadedFile uploadedFile = new UploadedFile(item.getInputStream(), item.getName(),
                                                                 item.getContentType());
                    logger.info("### \"" + fieldName + "\" => " + uploadedFile);
                    params.put(fieldName, uploadedFile);
                }
            }
            return params;
        } catch (Exception e) {
            throw new RuntimeException("Parsing multipart/form-data request failed", e);
        }
    }
}
