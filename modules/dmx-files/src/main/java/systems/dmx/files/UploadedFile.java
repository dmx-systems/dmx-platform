package systems.dmx.files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;



/**
 * An uploaded file.
 * <p>
 * Files are uploaded via the REST API by POSTing <code>multipart/form-data</code> to a resource method
 * which consumes <code>multipart/form-data</code> and has UploadedFile as its entity parameter.
 *
 * @author <a href="mailto:jri@dmx.berlin">Jörg Richter</a>
 */
public class UploadedFile {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String name;
    private long size;
    private InputStream in;
    private DiskQuotaCheck diskQuotaCheck;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public UploadedFile(String name, long size, InputStream in, DiskQuotaCheck diskQuotaCheck) {
        this.name = name;
        this.size = size;
        this.in = in;
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
        return name;
    }



    // === File Content ===

    /**
     * Returns the contents of the uploaded file as a String, using the default character encoding.
     */
    public String getString() {
        return getString(Charset.defaultCharset().name());
    }

    /**
     * Returns the contents of the uploaded file as a String, using the given encoding.
     */
    public String getString(String encoding) {
        try {
            StringWriter out = new StringWriter();
            IOUtils.copy(in, out, encoding);
            in.close();
            out.close();
            return out.toString();
        } catch (Exception e) {
            throw new RuntimeException("Getting contents of uploaded file failed (" + this + ")", e);
        }
    }

    /**
     * Returns the contents of the uploaded file as an array of bytes.
     * TODO
    public byte[] getBytes() {
        return fileItem.get();
    }*/



    // === Java API ===

    @Override
    public String toString() {
        return "\"" + name + "\" (" + size + " bytes)";
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * Performs a disk quota check for the current user, and if it succeeds, writes this uploaded file to disk.
     */
    void write(File file) {
        try {
            diskQuotaCheck.check(size);
            // TODO: compare with FilesPlugin.createFile()
            OutputStream out = new FileOutputStream(file);
            IOUtils.copy(in, out);
            in.close();
            out.close();
        } catch (Exception e) {
            throw new RuntimeException("Writing uploaded file to disk failed (" + this + ")", e);
        }
    }
}
