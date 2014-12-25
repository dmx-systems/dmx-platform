package de.deepamehta.core.service.accesscontrol;

import java.util.HashMap;
import java.util.Map;



public enum SharingMode {

    // ### TODO: move to dm4.core namespace?
    PRIVATE("dm4.workspaces.private"),
    CONFIDENTIAL("dm4.workspaces.confidential"),
    COLLABORATIVE("dm4.workspaces.collaborative"),
    PUBLIC("dm4.workspaces.public"),
    COMMON("dm4.workspaces.common");

    // ---

    public static SharingMode fromUri(String uri) {
        SharingMode sharingMode = sharingModes.get(uri);
        if (sharingMode == null) {
            throw new RuntimeException("\"" + uri + "\" is an unexpected sharing mode URI");
        }
        return sharingMode;
    }

    public String getUri() {
        return uri;
    }

    // ---

    private final String uri;

    private static Map<String, SharingMode> sharingModes;

    private SharingMode(String uri) {
        this.uri = uri;
        put(uri, this);
        // sharingModes.put(uri, this);   // ### "illegal reference to static field from initializer"
    }

    private void put(String uri, SharingMode sharingMode) {
        if (sharingModes == null) {
            sharingModes = new HashMap();
        }
        sharingModes.put(uri, sharingMode);
    }
}
