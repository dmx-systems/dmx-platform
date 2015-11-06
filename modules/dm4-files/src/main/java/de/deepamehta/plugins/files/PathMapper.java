package de.deepamehta.plugins.files;

import java.io.File;



interface PathMapper {

    /**
     * Maps an absolute path to a repository path.
     *
     * @param   path    An absolute path.
     *                  Must be canonized.
     *
     * @return  A repository path. Relative to the repository base path.
     *          Begins with slash, no slash at the end.
     */
    String repoPath(File path);
}
