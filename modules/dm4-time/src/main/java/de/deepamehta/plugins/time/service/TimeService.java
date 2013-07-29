package de.deepamehta.plugins.time.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.PluginService;

import java.util.Collection;



public interface TimeService extends PluginService {

    // === Timestamps ===

    long getCreationTime(DeepaMehtaObject object);

    long getModificationTime(DeepaMehtaObject object);

    // === Retrieval ===

    Collection<Topic> getTopicsByCreationTime(long from, long to);

    Collection<Topic> getTopicsByModificationTime(long from, long to);

    Collection<Association> getAssociationsByCreationTime(long from, long to);

    Collection<Association> getAssociationsByModificationTime(long from, long to);
}
