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

    /**
     * Retrieves the CREATED/MODIFIED timestamps and stores them in the given object's model (under synthetic child type
     * URIs <code>dmx.timestamps.created</code> and <code>dmx.timestamps.modified</code>).
     */
    void enrichWithTimestamps(DMXObject object);

    // === Retrieval ===

    Collection<Topic> getTopicsByCreationTime(long from, long to);

    Collection<Topic> getTopicsByModificationTime(long from, long to);

    Collection<Assoc> getAssocsByCreationTime(long from, long to);

    Collection<Assoc> getAssocsByModificationTime(long from, long to);
}
