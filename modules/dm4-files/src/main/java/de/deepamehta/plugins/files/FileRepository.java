package de.deepamehta.plugins.files;

import de.deepamehta.core.Topic;

import java.io.File;
import java.util.logging.Logger;



class FileRepository {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String FILES_PATH = System.getProperty("dm4.files.path");

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private File filesDir;
    private FileRepositoryContext context;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    FileRepository(FileRepositoryContext context) {
        this.filesDir = new File(FILES_PATH);
        this.context = context;
    }

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

    StoredFile storeFile(UploadedFile file) {
        File repoFile = null;
        try {
            Topic fileTopic = createFileTopic(file);
            repoFile = repoFile(fileTopic);
            logger.info("Storing file \"" + repoFile + "\" in file repository");
            file.write(repoFile);
            return new StoredFile(fileName(fileTopic), fileTopic.getId());
        } catch (Exception e) {
            throw new RuntimeException("Storing file \"" + repoFile + "\" in file repository failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private Topic createFileTopic(UploadedFile file) {
        String fileName = file.getName();
        String path = "/" + fileName;
        String mediaType = file.getMediaType();
        long size = file.getSize();
        //
        return context.createFileTopic(fileName, path, mediaType, size);
    }

    /**
     * Calculates the storage location for the file represented by the specified topic.
     */
    private File repoFile(Topic fileTopic) {
        String repoFileName = fileTopic.getId() + "-" + fileName(fileTopic);
        return new File(filesDir, repoFileName);
    }

    private String fileName(Topic fileTopic) {
        return fileTopic.getCompositeValue().getString("dm4.files.file_name");
    }
}
