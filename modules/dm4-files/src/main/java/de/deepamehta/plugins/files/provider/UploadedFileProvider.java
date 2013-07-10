package de.deepamehta.plugins.files.provider;

import de.deepamehta.plugins.files.UploadedFile;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import java.io.InputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;



@Provider
public class UploadedFileProvider implements MessageBodyReader<UploadedFile> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Context
    private HttpServletRequest request;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    // *** MessageBodyReader Implementation ***

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        // Note: unlike equals() isCompatible() ignores parameters like "charset" in "application/json;charset=UTF-8"
        return type == UploadedFile.class && mediaType.isCompatible(MediaType.MULTIPART_FORM_DATA_TYPE);
    }

    @Override
    public UploadedFile readFrom(Class<UploadedFile> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                                                                throws IOException, WebApplicationException {
        try {
            return parseMultiPart();
        } catch (Exception e) {
            throw new RuntimeException("Creating UploadedFile from message body failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private UploadedFile parseMultiPart() {
        try {
            UploadedFile file = null;
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
                    logger.info("### field \"" + fieldName + "\" => \"" + value + "\"");
                    throw new RuntimeException("\"" + fieldName + "\" is an unexpected field (value=\"" + value +
                        "\")");
                } else {
                    if (file != null) {
                        throw new RuntimeException("Only single file uploads are supported");
                    }
                    file = new UploadedFile(item);
                    logger.info("### field \"" + fieldName + "\" => " + file);
                }
            }
            if (file == null) {
                throw new RuntimeException("Request does not contain a file part");
            }
            return file;
        } catch (Exception e) {
            throw new RuntimeException("Parsing multipart/form-data request failed", e);
        }
    }
}
