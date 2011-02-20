package de.deepamehta.core.util;

import java.io.InputStream;



/**
 * An uploaded file.
 * <p>
 * Files are uploaded via the REST API by POSTing <code>multipart/form-data</code> to the <code>/command</code>
 * resource.
 * <p>
 * Client-side support: the <a href="http://github.com/jri/deepamehta3-client">deepamehta3-client</a> plugin
 * provides an utility method <code>show_upload_dialog</code> that allows the user to choose and upload a file.</p>
 * <p>
 * At server-side a plugin accesses the upload file via the
 * {@link de.deepamehta.core.service.Plugin#executeCommandHook}.</p>
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class UploadedFile {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private InputStream inputStream;
    private String fileName;
    private String mimeType;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public UploadedFile(InputStream inputStream, String fileName, String mimeType) {
        this.inputStream = inputStream;
        this.fileName = fileName;
        this.mimeType = mimeType;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * Returns the original (client-side) file name.
     */
    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    // ---

    @Override
    public String toString() {
        return "file \"" + fileName + "\" (" + mimeType + ")";
    }
}
