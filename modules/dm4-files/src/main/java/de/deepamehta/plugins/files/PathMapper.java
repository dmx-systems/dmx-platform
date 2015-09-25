package de.deepamehta.plugins.files;

import java.io.File;



interface PathMapper {

    /**
     * Maps an absolute path to a repository path.
     *
     * @param   path    An absolute path.
     */
    String repoPath(File path);
}
