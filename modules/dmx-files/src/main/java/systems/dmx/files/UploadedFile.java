package systems.dmx.files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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



    // === Java API ===

    @Override
    public String toString() {
        return "\"" + name + "\" (" + size + " bytes)";
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * A convenience method to write the uploaded file to disk.
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
