package de.deepamehta.plugins.server.provider;

import de.deepamehta.core.model.CommandParams;
import de.deepamehta.core.util.JavaUtils;
import de.deepamehta.core.util.JSONHelper;
import de.deepamehta.core.util.UploadedFile;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
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
            throw new IOException("Reading a CommandParams object from request stream failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private Map<String, Object> multiPartToMap() {
        try {
            FileItemFactory factory = new DiskFileItemFactory();        // create a factory for disk-based file items
            ServletFileUpload upload = new ServletFileUpload(factory);  // create a new file upload handler
            List<FileItem> items = upload.parseRequest(request);        // parse the request
            // FIXME: check if we can use a FileUpload low-level method to parse the request body instead of the
            // entire request. Advantage: a) no need to inject the HttpServletRequest, b) no double work as the
            // request already parsed by jersey, c) no dependency on servlet-api.
            //
            Map<String, Object> params = new HashMap();
            for (FileItem item : items) {
                if (!item.isFormField()) {
                    String fieldName = item.getFieldName();
                    String fileName = item.getName();
                    String mimeType = item.getContentType();
                    UploadedFile uploadedFile = new UploadedFile(null, fileName, mimeType);
                    logger.info("### \"" + fieldName + "\" => " + uploadedFile);
                    params.put(fieldName, uploadedFile);
                } else {
                    throw new RuntimeException("Regular form fields are not yet supported");
                }
            }
            return params;
        } catch (FileUploadException e) {
            throw new RuntimeException("Processing multipart/form-data request failed", e);
        }
    }
}
