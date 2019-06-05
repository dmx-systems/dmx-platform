package systems.dmx.timestamps;

import systems.dmx.core.Assoc;
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

    Collection<Assoc> getAssociationsByCreationTime(long from, long to);

    Collection<Assoc> getAssociationsByModificationTime(long from, long to);
}
