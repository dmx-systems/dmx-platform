package de.deepamehta.files;

import org.apache.commons.fileupload.FileItem;

import java.io.File;
import java.io.InputStream;



/**
 * An uploaded file.
 * <p>
 * Files are uploaded via the REST API by POSTing <code>multipart/form-data</code> to a resource method
 * which consumes <code>multipart/form-data</code> and has UploadedFile as its entity parameter.
 * <p>
 * Client-side support: the public API of the Files plugin provides a method
 * <code>dm4c.get_plugin("de.deepamehta.files").open_upload_dialog()</code> that allows the user to
 * choose and upload a file.</p>
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class UploadedFile {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private FileItem fileItem;
    private DiskQuotaCheck diskQuotaCheck;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public UploadedFile(FileItem fileItem, DiskQuotaCheck diskQuotaCheck) {
        this.fileItem = fileItem;
        this.diskQuotaCheck = diskQuotaCheck;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // === File Metadata ===

    /**
     * Returns the original filename in the client's filesystem, as provided by the browser (or other client software).
     * In most cases, this will be the base file name, without path information. However, some clients, such as the
     * Opera browser, do include path information.
     */
    public String getName() {
        return fileItem.getName();
    }

    /**
     * Returns the size of the file.
     */
    public long getSize() {
        return fileItem.getSize();
    }

    /**
     * Returns the content type passed by the browser or <code>null</code> if not defined.
     */
    public String getMediaType() {
        return fileItem.getContentType();
    }

    // === File Content ===

    /**
     * Returns the contents of the uploaded file as a String, using the default character encoding.
     */
    public String getString() {
        return fileItem.getString();
    }

    /**
     * Returns the contents of the uploaded file as a String, using the specified encoding.
     */
    public String getString(String encoding) {
        try {
            return fileItem.getString(encoding);    // throws UnsupportedEncodingException
        } catch (Exception e) {
            throw new RuntimeException("Getting contents of uploaded file failed (" + this + ")", e);
        }
    }

    /**
     * Returns the contents of the uploaded file as an array of bytes.
     */
    public byte[] getBytes() {
        return fileItem.get();
    }

    /**
     * Returns an InputStream that can be used to retrieve the contents of the uploaded file.
     */
    public InputStream getInputStream() {
        try {
            return fileItem.getInputStream();       // throws IOException
        } catch (Exception e) {
            throw new RuntimeException("Getting input stream of uploaded file failed (" + this + ")", e);
        }
    }

    // === Storage ===

    /**
     * A convenience method to write the uploaded file to disk.
     */
    public void write(File file) {
        try {
            diskQuotaCheck.check(getSize());
            fileItem.write(file);                   // throws Exception
        } catch (Exception e) {
            throw new RuntimeException("Writing uploaded file to disk failed (" + this + ")", e);
        }
    }

    // ===

    @Override
    public String toString() {
        return "file \"" + getName() + "\" (" + getMediaType() + ", " + getSize() + " bytes)";
    }
}
