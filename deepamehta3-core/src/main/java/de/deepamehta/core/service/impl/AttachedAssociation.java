package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.AssociationData;
import de.deepamehta.core.model.Role;
import de.deepamehta.core.model.Topic;

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
        for (Role role : getRoles()) {
            if (role.getRoleTypeUri().equals(roleTypeUri)) {
                // Note: storage low-level call used here ### explain
                topics.add(dms.getTopic(role.getTopicId(), null));
            }
        }
        return topics;
    }

    // ---

    @Override
    public void addRole(Role role) {
        // update memory
        super.addRole(role);
        // update DB
        dms.addAssociationRole(getId(), role);
    }
}
