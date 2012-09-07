package de.deepamehta.plugins.files;

import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;



public class DirectoryListing implements JSONEnabled {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String FILE_REPOSITORY_PATH = System.getProperty("dm4.filerepo.path");

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private FileItem dirInfo;
    private List<FileItem> fileItems = new ArrayList();

    // ---------------------------------------------------------------------------------------------------- Constructors

    public DirectoryListing(File directory) {
        dirInfo = new FileItem(directory.getName(), directory.getPath());
        for (File file : directory.listFiles()) {
            String name = file.getName();
            String path = file.getPath();
            if (file.isDirectory()) {
                fileItems.add(new FileItem(name, path));
            } else {
                fileItems.add(new FileItem(name, path, file.length(), JavaUtils.getFileType(file.getName())));
            }
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public List<FileItem> getFileItems() {
        return fileItems;
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject dir = dirInfo.toJSON();
            JSONArray items = new JSONArray();
            for (FileItem item : fileItems) {
                items.put(item.toJSON());
            }
            dir.put("items", items);
            return dir;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // --------------------------------------------------------------------------------------------------- Inner Classes

    public enum ItemKind {
        FILE, DIRECTORY;
    }

    public class FileItem implements JSONEnabled {

        ItemKind kind;  // FILE or DIRECTORY
        String name;
        String path;
        long size;      // for files only
        String type;    // for files only

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
}
