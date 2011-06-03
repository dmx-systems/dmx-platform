package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.impl.model.AssociationBase;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.Role;
import de.deepamehta.core.model.TopicRole;

import java.util.HashSet;
import java.util.Set;

import java.util.logging.Logger;



/**
 * An association that is attached to the {@link DeepaMehtaService}.
 */
class AttachedAssociation extends AssociationBase {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedAssociation(Association assoc, EmbeddedService dms) {
        super(((AssociationBase) assoc).getModel());
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Association Overrides ===

    @Override
    public void setTypeUri(String assocTypeUri) {
        // 1) update memory
        super.setTypeUri(assocTypeUri);
        // 2) update DB
        storeTypeUri();
    }

    // ---

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
        filterTopic(getRole1(), roleTypeUri, topics);
        filterTopic(getRole2(), roleTypeUri, topics);
        return topics;
    }

    // ---

    @Override
    public AttachedRelatedTopic getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                           String othersTopicTypeUri,
                                                                                           boolean fetchComposite) {
        RelatedTopic topic = dms.storage.getAssociationRelatedTopic(getId(), assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri);
        return topic != null ? dms.attach(topic, fetchComposite) : null;
    }

    @Override
    public Set<RelatedTopic> getRelatedTopics(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                         String othersTopicTypeUri,
                                                                                         boolean fetchComposite) {
        return dms.attach(dms.storage.getAssociationRelatedTopics(getId(), assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri), fetchComposite);
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    void update(AssociationModel assocModel) {
        logger.info("Updating association " + getId() + " (new " + assocModel + ")");
        String typeUri = assocModel.getTypeUri();
        //
        boolean typeUriChanged = !getTypeUri().equals(typeUri);
        //
        if (typeUriChanged) {
            logger.info("Changing type from \"" + getTypeUri() + "\" -> \"" + typeUri + "\"");
            setTypeUri(typeUri);
        }
        //
        if (!typeUriChanged) {
            logger.info("Updating association " + getId() + " ABORTED -- no changes made by user");
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private RelatedTopic fetchTypeTopic() {
        // assocTypeUri=null (supposed to be "dm3.core.instantiation" but not possible ### explain)
        return getRelatedTopic(null, "dm3.core.instance", "dm3.core.type", "dm3.core.assoc_type",
            false);     // fetchComposite=false
    }

    // ---

    private void filterTopic(Role role, String roleTypeUri, Set<Topic> topics) {
        if (role instanceof TopicRole && role.getRoleTypeUri().equals(roleTypeUri)) {
            topics.add(fetchRoleTopic((TopicRole) role));
        }
    }

    private Topic fetchRoleTopic(TopicRole role) {
        return dms.getTopic(role.getTopicId(), false, null);    // fetchComposite=false, clientContext=null
    }



    // === Store ===

    private void storeTypeUri() {
        // remove current assignment
        long assocId = fetchTypeTopic().getAssociation().getId();
        dms.deleteAssociation(assocId, null);  // clientContext=null
        // create new assignment
        dms.associateWithAssociationType(this);
    }    
}
