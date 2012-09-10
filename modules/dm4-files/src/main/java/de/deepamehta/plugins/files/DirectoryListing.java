package de.deepamehta.plugins.files;

import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;



public class DirectoryListing implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private FileItem dirInfo;
    private List<FileItem> fileItems = new ArrayList<FileItem>();

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

}
