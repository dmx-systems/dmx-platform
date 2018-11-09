package systems.dmx.timestamps;

import systems.dmx.core.Association;
import systems.dmx.core.DMXObject;
import systems.dmx.core.Topic;

import java.util.Collection;



public interface TimestampsService {

    // === Timestamps ===

    long getCreationTime(long objectId);

    long getModificationTime(long objectId);

    // ---

    void setModified(DMXObject object);

    // === Retrieval ===

    Collection<Topic> getTopicsByCreationTime(long from, long to);

    Collection<Topic> getTopicsByModificationTime(long from, long to);

    Collection<Association> getAssociationsByCreationTime(long from, long to);

    Collection<Association> getAssociationsByModificationTime(long from, long to);
}
