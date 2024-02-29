package systems.dmx.files;

import java.io.BufferedInputStream;
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

    // ---------------------------------------------------------------------------------------------------- Constructors

    public UploadedFile(String name, long size, InputStream in) {
        this.name = name;
        this.size = size;
        this.in = in;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    /**
     * Returns the original filename in the client's filesystem, as provided by the browser (or other client software).
     * In most cases, this will be the base file name, without path information. However, some clients, such as the
     * Opera browser, do include path information.
     */
    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    /**
     * Returns an InputStream that can be used to retrieve the contents of the uploaded file.
     */
    public InputStream getInputStream() {
        return in;
    }

    /**
     * Transforms the InputStream into a BufferedInputStream and sets a mark. This allows to call reset() later on in
     * order to read the stream's bytes again. Should be called <i>before</i> you read any bytes from the stream.
     * <p>
     * Call {@link #getInputStream} <i>after</i> calling {@link #setBuffered} in order to get the actual
     * BufferedInputStream.
     * <p>
     * Calling reset() without calling {@link #setBuffered} before throws an IOException.
     */
    public void setBuffered() {
        in = new BufferedInputStream(in, (int) size);
        in.mark((int) size + 1);
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
     * Writes this uploaded file to disk.
     */
    void write(File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            IOUtils.copy(in, out);
            in.close();
            out.close();
        } catch (Exception e) {
            throw new RuntimeException("Writing uploaded file to disk failed (" + this + ")", e);
        }
    }
}
