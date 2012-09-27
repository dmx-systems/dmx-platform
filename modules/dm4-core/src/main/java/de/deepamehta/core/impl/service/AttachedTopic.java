package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaTransaction;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedAssociationModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.ChangeReport;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;



/**
 * A topic that is attached to the {@link DeepaMehtaService}.
 */
class AttachedTopic extends AttachedDeepaMehtaObject implements Topic {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedTopic(EmbeddedService dms) {
        super(dms);
    }

    AttachedTopic(TopicModel model, EmbeddedService dms) {
        super(model, dms);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ******************************************
    // *** AttachedDeepaMehtaObject Overrides ***
    // ******************************************



    // === Implementation of the abstract methods ===

    @Override
    protected String className() {
        return "topic";
    }

    @Override
    protected void storeUri(String uri) {
        dms.storage.setTopicUri(getId(), uri);
    }

    @Override
    protected void storeTypeUri() {
        // remove current assignment
        long assocId = fetchTypeTopic().getAssociation().getId();
        dms.deleteAssociation(assocId, null);  // clientState=null
        // create new assignment
        dms.associateWithTopicType(getModel());
    }    

    @Override
    protected SimpleValue storeValue(SimpleValue value) {
        return dms.storage.setTopicValue(getId(), value);
    }

    @Override
    protected void indexValue(IndexMode indexMode, String indexKey, SimpleValue value, SimpleValue oldValue) {
        dms.storage.indexTopicValue(getId(), indexMode, indexKey, value, oldValue);
    }

    @Override
    protected Type getType() {
        return dms.getTopicType(getTypeUri(), null);    // FIXME: clientState=null
    }

    @Override
    protected RoleModel createRoleModel(String roleTypeUri) {
        return new TopicRoleModel(getId(), roleTypeUri);
    }

    // === Deletion ===

    @Override
    public void delete(Directives directives) {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            // delete sub-topics and associations
            super.delete(directives);
            // delete topic itself
            logger.info("Deleting " + this);
            dms.storage.deleteTopic(getId());
            directives.add(Directive.DELETE_TOPIC, this);
            //
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Deleting topic failed (" + this + ")", e);
        } finally {
            tx.finish();
        }
    }



    // ****************************
    // *** Topic Implementation ***
    // ****************************



    @Override
    public TopicModel getModel() {
        return (TopicModel) super.getModel();
    }

    // === Updating ===

    @Override
    public ChangeReport update(TopicModel model, ClientState clientState, Directives directives) {
        logger.info("Updating topic " + getId() + " (new " + model + ")");
        //
        dms.fireEvent(CoreEvent.PRE_UPDATE_TOPIC, this, model, directives);
        //
        TopicModel oldModel = (TopicModel) getModel().clone();
        ChangeReport report = super.update(model, clientState, directives);
        //
        directives.add(Directive.UPDATE_TOPIC, this);
        //
        dms.fireEvent(CoreEvent.POST_UPDATE_TOPIC, this, model, oldModel, clientState, directives);
        //
        return report;
    }

    // === Traversal ===

    // --- Association Retrieval ---

    @Override
    public RelatedAssociation getRelatedAssociation(String assocTypeUri, String myRoleTypeUri,
                                                    String othersRoleTypeUri, String othersAssocTypeUri,
                                                    boolean fetchComposite, boolean fetchRelatingComposite) {
        Set<RelatedAssociation> assocs = getRelatedAssociations(assocTypeUri, myRoleTypeUri,
                                                                othersRoleTypeUri, othersAssocTypeUri,
                                                                fetchComposite, fetchRelatingComposite);
        switch (assocs.size()) {
        case 0:
            return null;
        case 1:
            return assocs.iterator().next();
        default:
            throw new RuntimeException("Ambiguity: there are " + assocs.size() + " related associations (topicId=" +
                getId() + ", assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri + "\", " +
                "othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersAssocTypeUri=\"" + othersAssocTypeUri + "\")");
        }
    }

    @Override
    public Set<RelatedAssociation> getRelatedAssociations(String assocTypeUri, String myRoleTypeUri,
                                                          String othersRoleTypeUri, String othersAssocTypeUri,
                                                          boolean fetchComposite, boolean fetchRelatingComposite) {
        Set<RelatedAssociationModel> assocs = dms.storage.getTopicRelatedAssociations(getId(), assocTypeUri,
            myRoleTypeUri, othersRoleTypeUri, othersAssocTypeUri);
        return dms.attach(assocs, fetchComposite, fetchRelatingComposite);
    }



    // ***************************************
    // *** DeepaMehtaObject Implementation ***
    // ***************************************



    // === Traversal ===

    // --- Topic Retrieval ---

    @Override
    public AttachedRelatedTopic getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                String othersTopicTypeUri, boolean fetchComposite,
                                                boolean fetchRelatingComposite, ClientState clientState) {
        RelatedTopicModel topic = dms.storage.getTopicRelatedTopic(getId(), assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri);
        return topic != null ? dms.attach(topic, fetchComposite, fetchRelatingComposite, clientState) : null;
    }

    @Override
    public ResultSet<RelatedTopic> getRelatedTopics(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                    String othersTopicTypeUri, boolean fetchComposite, boolean fetchRelatingComposite,
                                    int maxResultSize, ClientState clientState) {
        ResultSet<RelatedTopicModel> topics = dms.storage.getTopicRelatedTopics(getId(), assocTypeUri,
            myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri, maxResultSize);
        return dms.attach(topics, fetchComposite, fetchRelatingComposite, clientState);
    }

    @Override
    public ResultSet<RelatedTopic> getRelatedTopics(List<String> assocTypeUris, String myRoleTypeUri, String othersRoleTypeUri,
                                    String othersTopicTypeUri, boolean fetchComposite, boolean fetchRelatingComposite,
                                    int maxResultSize, ClientState clientState) {
        ResultSet<RelatedTopicModel> topics = dms.storage.getTopicRelatedTopics(getId(), assocTypeUris,
            myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri, maxResultSize);
        return dms.attach(topics, fetchComposite, fetchRelatingComposite, clientState);
    }

    // --- Association Retrieval ---

    @Override
    public Association getAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                   long othersTopicId) {
        AssociationModel assoc = dms.storage.getAssociation(assocTypeUri, getId(), othersTopicId, myRoleTypeUri,
                                                                                                  othersRoleTypeUri);
        return assoc != null ? dms.attach(assoc, false) : null;                             // fetchComposite=false
    }

    @Override
    public Set<Association> getAssociations(String myRoleTypeUri) {
        return dms.attach(dms.storage.getTopicAssociations(getId(), myRoleTypeUri), false); // fetchComposite=false
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    @Override
    void store(ClientState clientState, Directives directives) {
        dms.storage.createTopic(getModel());
        dms.associateWithTopicType(getModel());
        super.store(clientState, directives);
    }

    /**
     * Convenience method.
     */
    TopicType getTopicType() {
        return (TopicType) getType();
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private RelatedTopic fetchTypeTopic() {
        return getRelatedTopic("dm4.core.instantiation", "dm4.core.instance", "dm4.core.type", "dm4.core.topic_type",
            false, false, null);     // fetchComposite=false
    }
}
