package de.deepamehta.plugins.files.model;

import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;



public class DirectoryListing {
    
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

    public JSONObject toJSON() {
        try {
            JSONObject dir = dirInfo.toJSON();
            JSONArray items = new JSONArray();
            for (FileItem item : fileItems) {
                items.put(item.toJSON());
            }
            dir.put("items", items);
            return dir;
        } catch (JSONException e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // --------------------------------------------------------------------------------------------------- Inner Classes

    class FileItem {

        String kind;    // "file", "directory"
        String name;
        String path;
        long   size;    // for files only
        String type;    // for files only

        /**
         * Constructs a "directory" item.
         */
        FileItem(String name, String path) {
            this.kind = "directory";
            this.name = name;
            this.path = path;
        }

        /**
         * Constructs a "file" item.
         */
        FileItem(String name, String path, long size, String type) {
            this.kind = "file";
            this.name = name;
            this.path = path;
            this.size = size;
            this.type = type;
        }

        // ---

        JSONObject toJSON() {
            try {
                JSONObject item = new JSONObject();
                item.put("kind", kind);
                item.put("name", name);
                item.put("path", path);
                if (kind.equals("file")) {
                    item.put("size", size);
                    item.put("type", type);
                }
                return item;
            } catch (JSONException e) {
                throw new RuntimeException("Serialization failed (" + this + ")", e);
            }
        }        
    }
}
