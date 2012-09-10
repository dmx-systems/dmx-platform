package de.deepamehta.plugins.files;

import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONObject;



public class FileItem implements JSONEnabled {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String FILE_REPOSITORY_PATH = System.getProperty("dm4.filerepo.path");

    ItemKind kind; // FILE or DIRECTORY
    String name;
    String path;
    long size; // for files only
    String type; // for files only

    /**
     * Constructs a DIRECTORY item.
     */
    FileItem(String name, String path) {
        this.kind = ItemKind.DIRECTORY;
        this.name = name;
        this.path = truncate(path);
    }

    /**
     * Constructs a FILE item.
     */
    FileItem(String name, String path, long size, String type) {
        this.kind = ItemKind.FILE;
        this.name = name;
        this.path = truncate(path);
        this.size = size;
        this.type = type;
    }

    // ---

    public ItemKind getItemKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    public String getMediaType() {
        return type;
    }

    // ---

    private String truncate(String path) {
        // error check
        if (!path.startsWith(FILE_REPOSITORY_PATH)) {
            throw new RuntimeException("Path \"" + path + "\" is not a file repository path");
        }
        //
        return JavaUtils.stripDriveLetter(path.substring(FILE_REPOSITORY_PATH.length()));
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject item = new JSONObject();
            item.put("kind", kind.name().toLowerCase());
            item.put("name", name);
            item.put("path", path);
            if (kind.equals("file")) {
                item.put("size", size);
                item.put("type", type);
            }
            return item;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }
}
