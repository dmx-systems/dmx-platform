package de.deepamehta.plugins.time;

import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.Topic;

import java.util.Collection;



public interface TimeService {

    // === Timestamps ===

    long getCreationTime(long objectId);

    long getModificationTime(long objectId);

    // ---

    void setModified(DeepaMehtaObject object);

    // === Retrieval ===

    Collection<Topic> getTopicsByCreationTime(long from, long to);

    Collection<Topic> getTopicsByModificationTime(long from, long to);

    Collection<Association> getAssociationsByCreationTime(long from, long to);

    Collection<Association> getAssociationsByModificationTime(long from, long to);
}
