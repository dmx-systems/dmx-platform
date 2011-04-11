package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.AssociationData;
import de.deepamehta.core.model.AssociationRole;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicRole;

import java.util.HashSet;
import java.util.Set;

import java.util.logging.Logger;



/**
 * An association that is attached to a {@link CoreService} implementation.
 */
class AttachedAssociation extends AssociationData implements Association {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedAssociation(Association assoc, EmbeddedService dms) {
        super(assoc);
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Topic getTopic(String roleTypeUri) {
        Set<Topic> topics = getTopics(roleTypeUri);
        switch (topics.size()) {
        case 0:
            return null;
        case 1:
            return topics.iterator().next();
        default:
            throw new RuntimeException("Ambiguity in association: " + topics.size() + " topics have role type \"" +
                roleTypeUri + "\" (" + this + ")");
        }
    }

    @Override
    public Set<Topic> getTopics(String roleTypeUri) {
        Set<Topic> topics = new HashSet();
        for (TopicRole topicRole : getTopicRoles()) {
            if (topicRole.getRoleTypeUri().equals(roleTypeUri)) {
                topics.add(dms.getTopic(topicRole.getTopicId(), null));
            }
        }
        return topics;
    }

    // ---

    @Override
    public void addTopicRole(TopicRole topicRole) {
        // update memory
        super.addTopicRole(topicRole);
        // update DB
        dms.addTopicToAssociation(getId(), topicRole);
    }

    @Override
    public void addAssociationRole(AssociationRole assocRole) {
        // update memory
        super.addAssociationRole(assocRole);
        // update DB
        dms.addAssociationToAssociation(getId(), assocRole);
    }
}
