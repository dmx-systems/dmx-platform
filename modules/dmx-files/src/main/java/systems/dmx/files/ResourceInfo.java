package systems.dmx.files;

import systems.dmx.core.JSONEnabled;

import org.codehaus.jettison.json.JSONObject;

import java.io.File;



public class ResourceInfo implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private ItemKind kind;  // FILE or DIRECTORY

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * Precondition: the file exists.
     */
    public ResourceInfo(File file) {
        kind = file.isDirectory() ? ItemKind.DIRECTORY : ItemKind.FILE;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public ItemKind getItemKind() {
        return kind;
    }

    @Override
    public JSONObject toJSON() {
        try {
            return new JSONObject()
                .put("kind", kind.stringify());
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    @Override
    public String toString() {
        return "resource info (kind=\"" + kind + "\")";
    }
}
