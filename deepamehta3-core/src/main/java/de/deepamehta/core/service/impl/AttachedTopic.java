package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.AssociationDefinition;
import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.Role;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicData;
import de.deepamehta.core.model.TopicTypeDefinition;
import de.deepamehta.core.model.impl.BaseTopic;
import de.deepamehta.core.service.CoreService;
import de.deepamehta.core.storage.DeepaMehtaTransaction;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;



class AttachedTopic extends BaseTopic {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedTopic(Topic topic, EmbeddedService dms) {
        super(topic);
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void setValue(String assocDefUri, Object value) {
        TopicTypeDefinition typeDef = dms.getTopicTypeDefinition(getTypeUri());
        AssociationDefinition assocDef = typeDef.getAssociationDefinition(assocDefUri);
        String assocTypeUri = assocDef.getAssocTypeUri();
        String wholeRoleTypeUri = assocDef.getWholeRoleTypeUri();
        String  partRoleTypeUri = assocDef.getPartRoleTypeUri();
        //
        Topic childTopic = getRelatedTopic(assocTypeUri, wholeRoleTypeUri, partRoleTypeUri);
        if (childTopic == null) {
            // create child topic
            String topicTypeUri = assocDef.getPartTopicTypeUri();
            logger.info("Topic for association definition \"" + assocDefUri + "\" not yet exists (topicTypeUri=\"" +
                topicTypeUri + "\")");
            childTopic = dms.createTopic(new TopicData(null, value, topicTypeUri, null), null);
            // create association
            Set<Role> roles = new HashSet();
            roles.add(new Role(getId(), wholeRoleTypeUri));
            roles.add(new Role(childTopic.getId(), partRoleTypeUri));
            dms.createAssociation(new Association(-1, assocTypeUri, roles), null);
        }
    }

    @Override
    public Topic getRelatedTopic(String assocTypeUri, String myRoleType, String othersRoleType) {
        return dms.getStorage().getRelatedTopic(getId(), assocTypeUri, myRoleType, othersRoleType);
    }
}
