package de.deepamehta.files;

import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.util.DeepaMehtaUtils;
import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;



public class DirectoryListing implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private PathMapper pathMapper;
    private FileItem dirInfo;
    private List<FileItem> fileItems = new ArrayList<FileItem>();

    // ---------------------------------------------------------------------------------------------------- Constructors

    public DirectoryListing(File directory, PathMapper pathMapper) {
        this.pathMapper = pathMapper;
        this.dirInfo = new FileItem(directory);
        for (File file : directory.listFiles()) {
            fileItems.add(new FileItem(file));
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
            return dirInfo.toJSON().put("items", DeepaMehtaUtils.toJSONArray(fileItems));
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Nested Classes

    public class FileItem implements JSONEnabled {

        ItemKind kind;  // FILE or DIRECTORY
        String name;
        String path;
        long size;      // for files only
        String type;    // for files only

        FileItem(File file) {
            this.kind = file.isDirectory() ? ItemKind.DIRECTORY : ItemKind.FILE;
            this.name = file.getName();
            this.path = pathMapper.repoPath(file);
            if (kind == ItemKind.FILE) {
                this.size = file.length();
                this.type = JavaUtils.getFileType(name);
            }
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

        @Override
        public JSONObject toJSON() {
            try {
                JSONObject item = new JSONObject();
                item.put("kind", kind.stringify());
                item.put("name", name);
                item.put("path", path);
                if (kind == ItemKind.FILE) {
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
