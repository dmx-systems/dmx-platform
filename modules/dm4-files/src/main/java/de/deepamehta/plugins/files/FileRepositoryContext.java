package de.deepamehta.plugins.files;

import de.deepamehta.core.Topic;



interface FileRepositoryContext {

    Topic createFileTopic(String fileName, String path, String mediaType, long size);
}
