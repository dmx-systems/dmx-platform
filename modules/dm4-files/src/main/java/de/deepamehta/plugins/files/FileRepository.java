package de.deepamehta.plugins.files;

import de.deepamehta.core.DeepaMehtaTransaction;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.DeepaMehtaService;

import java.io.File;
import java.util.logging.Logger;



class FileRepository {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String FILE_REPOSITORY_PATH = System.getProperty("dm4.proxy.files.path");
    private static final String FILE_REPOSITORY_URI = "/filerepo";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DeepaMehtaService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    FileRepository(FileRepositoryContext context, DeepaMehtaService dms) {
        this.dms = dms;
        context.publishDirectory(FILE_REPOSITORY_PATH, FILE_REPOSITORY_URI);
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void createFolder(String folder_name, String storagePath) {
        // ### TODO
    }

    StoredFile storeFile(UploadedFile file, String storagePath) {
        File repoFile = null;
        try {
            Topic fileTopic = createFileTopic(file, storagePath);
            repoFile = repoFile(fileTopic);
            logger.info("Storing file \"" + repoFile + "\" in file repository");
            file.write(repoFile);
            return new StoredFile(fileName(fileTopic), fileTopic.getId());
        } catch (Exception e) {
            throw new RuntimeException("Storing file \"" + repoFile + "\" in file repository failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * @param   storagePath     Begins with "/". Has no "/" at end.
     */
    private Topic createFileTopic(UploadedFile file, String storagePath) {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            Topic fileTopic = dms.createTopic(new TopicModel("dm4.files.file"), null);       // FIXME: clientState=null
            //
            String fileName = fileTopic.getId() + "-" + file.getName();
            String path = storagePath + "/" + fileName;
            String mediaType = file.getMediaType();
            long size = file.getSize();
            //
            CompositeValue comp = new CompositeValue();
            comp.put("dm4.files.file_name", fileName);
            comp.put("dm4.files.path", path);
            if (mediaType != null) {
                comp.put("dm4.files.media_type", mediaType);
            }
            comp.put("dm4.files.size", size);
            //
            fileTopic.setCompositeValue(comp, null, null);
            //
            tx.success();
            return fileTopic;
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Creating file topic for uploaded file failed (" +
                file + ", storagePath=\"" + storagePath + "\")", e);
        } finally {
            tx.finish();
        }
    }

    /**
     * Calculates the storage location for the file represented by the specified file topic.
     */
    private File repoFile(Topic fileTopic) {
        return new File(FILE_REPOSITORY_PATH, path(fileTopic));
    }

    // ---

    private String fileName(Topic fileTopic) {
        return fileTopic.getCompositeValue().getString("dm4.files.file_name");
    }

    private String path(Topic fileTopic) {
        return fileTopic.getCompositeValue().getString("dm4.files.path");
    }
}
