package de.deepamehta.plugins.proxy.model;

import java.net.URL;



/**
 * A Resource represents a local file, a local directory, or a remote resource.
 * Everything an URL can point to.
 */
public class Resource {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    // used for local directories
    public DirectoryListing dir;

    // used for local files and remote resources
    public URL uri;
    public String mediaType;
    public long size;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public Resource(DirectoryListing dir) {
        this.dir = dir;
    }

    public Resource(URL uri, String mediaType, long size) {
        this.uri = uri;
        this.mediaType = mediaType;
        this.size = size;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String toString() {
        return "resource (uri=\"" + uri + "\", mediaType=\"" + mediaType + "\", size=" + size + ", dir=" + dir + ")";
    }
}
