package de.deepamehta.plugins.files;

import java.io.File;
import java.util.logging.Logger;



class FileRepository {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String FILES_PATH = System.getProperty("dm4.files.path");
    private static final File filesDir = new File(FILES_PATH);

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void initialize() {
        if (filesDir.exists()) {
            logger.info("Creating file repository ABORTED -- already exists (\"" + filesDir + "\")");
            return;
        }
        //
        logger.info("Creating file repository at \"" + filesDir + "\"");
        filesDir.mkdir();
    }

    void storeFile(UploadedFile file) {
        File repoFile = null;
        try {
            repoFile = new File(filesDir, file.getName());
            logger.info("Storing file \"" + repoFile + "\" in file repository");
            file.write(repoFile);
        } catch (Exception e) {
            throw new RuntimeException("Storing file \"" + repoFile + "\" in file repository failed", e);
        }
    }
}
