package de.deepamehta.plugins.files;

import org.apache.commons.fileupload.FileItem;

import java.io.File;



/**
 * An uploaded file.
 * <p>
 * Files are uploaded via the REST API by POSTing <code>multipart/form-data</code> to the <code>/files</code>
 * resource.
 * <p>
 * Client-side support: the public API of the <code>deepamehta-files</code> plugin provides a method
 * <code>dm4c.get_plugin("de.deepamehta.files").open_upload_dialog()</code> that allows the user to
 * choose and upload a file.</p>
 * <p>
 * At server-side a plugin accesses the upload file via the
 * {@link de.deepamehta.core.service.Plugin#executeCommandHook}. ### FIXDOC</p>
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class UploadedFile {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private FileItem fileItem;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public UploadedFile(FileItem fileItem) {
        this.fileItem = fileItem;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    /**
     * Returns the original (client-side) file name.
     */
    public String getName() {
        return fileItem.getName();
    }

    public long getSize() {
        return fileItem.getSize();
    }

    /**
     * Returns the content type passed by the browser or null if not defined.
     */
    public String getMediaType() {
        return fileItem.getContentType();
    }

    // ---

    @Override
    public String toString() {
        return "file \"" + getName() + "\" (" + getMediaType() + "), " + getSize() + " bytes";
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void write(File file) throws Exception {
        fileItem.write(file);   // throws Exception
    }
}
