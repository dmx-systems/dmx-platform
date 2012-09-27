package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.DeepaMehtaTransaction;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicDeletionModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.ChangeReport;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.util.DeepaMehtaUtils;
import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;



/**
 * DeepaMehtaObject implementation that takes a DeepaMehtaObjectModel and attaches it to the DB.
 *
 * Method name conventions and semantics:
 *  - getXX()           Reads from memory (model).
 *  - setXX(arg)        Writes to memory (model) and DB. Elementary operation.
 *  - updateXX(arg)     Compares arg with current value (model) and calls setXX() method(s) if required.
 *                      Can be called with arg=null which indicates no update is requested.
 *                      Typically returns nothing.
 *  - fetchXX()         Fetches value from DB.
 *  - storeXX()         Stores current value (model) to DB.
 */
abstract class AttachedDeepaMehtaObject implements DeepaMehtaObject {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String LABEL_SEPARATOR = " ";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DeepaMehtaObjectModel model;
    protected final EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedDeepaMehtaObject(EmbeddedService dms) {
        this.model = null;      // ### initialized through fetch().
        this.dms = dms;
    }

    AttachedDeepaMehtaObject(DeepaMehtaObjectModel model, EmbeddedService dms) {
        // set default values
        if (model.getUri() == null) {
            model.setUri("");
        }
        if (model.getSimpleValue() == null) {
            model.setSimpleValue("");
        }
        //
        this.model = model;
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ***************************************
    // *** DeepaMehtaObject Implementation ***
    // ***************************************



    // === Model ===

    // --- ID ---

    @Override
    public long getId() {
        return model.getId();
    }

    // --- URI ---

    @Override
    public String getUri() {
        return model.getUri();
    }

    @Override
    public void setUri(String uri) {
        // update memory
        model.setUri(uri);
        // update DB
        storeUri(uri);      // abstract
    }

    // --- Type URI ---

    @Override
    public String getTypeUri() {
        return model.getTypeUri();
    }

    @Override
    public void setTypeUri(String typeUri) {
        // update memory
        model.setTypeUri(typeUri);
        // update DB
        storeTypeUri();     // abstract
    }

    // --- Simple Value ---

    @Override
    public SimpleValue getSimpleValue() {
        return model.getSimpleValue();
    }

    // ---

    @Override
    public void setSimpleValue(String value) {
        setSimpleValue(new SimpleValue(value));
    }

    @Override
    public void setSimpleValue(int value) {
        setSimpleValue(new SimpleValue(value));
    }

    @Override
    public void setSimpleValue(long value) {
        setSimpleValue(new SimpleValue(value));
    }

    @Override
    public void setSimpleValue(boolean value) {
        setSimpleValue(new SimpleValue(value));
    }

    @Override
    public void setSimpleValue(SimpleValue value) {
        if (value == null) {
            throw new IllegalArgumentException("Tried to set a null SimpleValue (" + this + ")");
        }
        // update memory
        model.setSimpleValue(value);
        // update DB
        storeAndIndexValue(value);
    }

    // --- Composite Value ---

    @Override
    public CompositeValue getCompositeValue() {
        return model.getCompositeValue();
    }

    @Override
    public void setCompositeValue(CompositeValue comp, ClientState clientState, Directives directives) {
        DeepaMehtaTransaction tx = dms.beginTx();   // ### FIXME: all other writing API methods need transaction as well
        try {
            updateCompositeValue(comp, clientState, directives);
            refreshLabel();
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Setting composite value failed (" + comp + ")", e);
        } finally {
            tx.finish();
        }
    }

    // ---

    @Override
    public void updateChildTopic(AssociationDefinition assocDef, TopicModel newChildTopic, ClientState clientState,
                                                                                           Directives directives) {
        updateChildTopics(assocDef, true, newChildTopic, null, clientState, directives);    // one=true
    }

    @Override
    public void updateChildTopics(AssociationDefinition assocDef, List<TopicModel> newChildTopics,
                                                                  ClientState clientState, Directives directives) {
        updateChildTopics(assocDef, false, null, newChildTopics, clientState, directives);  // one=false
    }



    // === Traversal ===

    @Override
    public SimpleValue getChildTopicValue(String assocDefUri) {
        return fetchChildTopicValue(getAssocDef(assocDefUri));
    }

    @Override
    public void setChildTopicValue(String assocDefUri, SimpleValue value) {
        // update memory
        getCompositeValue().put(assocDefUri, value.value());
        // update DB
        storeChildTopicValue(assocDefUri, value);
        //
        refreshLabel();
    }

    // --- Topic Retrieval ---

    @Override
    public ResultSet<RelatedTopic> getRelatedTopics(String assocTypeUri, int maxResultSize, ClientState clientState) {
        return getRelatedTopics(assocTypeUri, null, null, null, false, false, maxResultSize, clientState);
    }

    // Note: these methods are implemented in the subclasses (this is an abstract class):
    //     getRelatedTopic(String assocTypeUri, ...)
    //     getRelatedTopics(String assocTypeUri, ...)
    //     getRelatedTopics(List assocTypeUris, ...)

    // --- Association Retrieval ---

    @Override
    public Set<Association> getAssociations() {
        return getAssociations(null);
    }

    // Note: these methods are implemented in the subclasses (this is an abstract class):
    //     getAssociation(...);
    //     getAssociations(String myRoleTypeUri);



    // === Updating ===

    @Override
    public ChangeReport update(DeepaMehtaObjectModel model, ClientState clientState, Directives directives) {
        ChangeReport report = new ChangeReport();
        updateUri(model.getUri());
        updateTypeUri(model.getTypeUri(), report);
        // ### TODO: compare new model with current one and update only if changed.
        if (getType().getDataTypeUri().equals("dm4.core.composite")) {
            updateCompositeValue(model.getCompositeValue(), clientState, directives);
            refreshLabel();
        } else {
            updateSimpleValue(model.getSimpleValue());
        }
        //
        return report;
    }



    // === Deletion ===

    /**
     * Deletes all sub-topics of this DeepaMehta object (associated via "dm4.core.composition", recursively) and
     * deletes all the remaining direct associations of this DeepaMehta object.
     * <p>
     * Note: deletion of the object itself is up to the subclasses.
     */
    @Override
    public void delete(Directives directives) {
        // Note: directives must be not null.
        // The subclass's delete() methods add DELETE_TOPIC and DELETE_ASSOCIATION directives to it respectively.
        if (directives == null) {
            throw new IllegalArgumentException("directives is null");
        }
        // 1) recursively delete sub-topics
        ResultSet<RelatedTopic> partTopics = getRelatedTopics("dm4.core.composition",
            "dm4.core.whole", "dm4.core.part", null, false, false, 0, null);
        for (Topic partTopic : partTopics) {
            partTopic.delete(directives);
        }
        // 2) delete direct associations
        for (Association assoc : getAssociations()) {
            try {
                assoc.delete(directives);
            } catch (IllegalStateException e) {
                // Note: this can happen in a particular situation and is no problem: let A1 and A2 be direct
                // associations of this DeepaMehta object and let A2 point to A1. If A1 gets deleted first
                // (the association set order is non-deterministic), A2 is implicitely deleted with it
                // (because it is a direct association of A1 as well). Then when the loop comes to A2
                // "IllegalStateException: Node[1327] has been deleted in this tx" is thrown because A2
                // has been deleted already. (The Node appearing in the exception is the auxiliary node of A2.)
                // If, on the other hand, A2 gets deleted first no error would occur.
                //
                // This particular situation exists when e.g. a topicmap is deleted while one of its mapcontext
                // associations is also a part of the topicmap itself. This originates e.g. when the user reveals
                // a topicmap's mapcontext association and then deletes the topicmap.
                //
                if (e.getMessage().matches("Node\\[\\d+\\] has been deleted in this tx")) {
                    logger.info("### Association " + assoc.getId() + " has already been deleted in this transaction. " +
                        "This can happen while deleting a topic with direct associations A1 and A2 while A2 points " +
                        "to A1");
                } else {
                    throw e;
                }
            }
        }
    }



    // **********************************
    // *** JSONEnabled Implementation ***
    // **********************************



    @Override
    public JSONObject toJSON() {
        return model.toJSON();
    }



    // ****************
    // *** Java API ***
    // ****************



    @Override
    public boolean equals(Object o) {
        return ((AttachedDeepaMehtaObject) o).model.equals(model);
    }

    @Override
    public int hashCode() {
        return model.hashCode();
    }

    @Override
    public String toString() {
        return model.toString();
    }



    // ----------------------------------------------------------------------------------------------- Protected Methods

    // ### This is supposed to be protected, but doesn't compile! ### FIXME: re-check
    // ### It is called from the subclasses constructors, but on a differnt TopicBase instance.
    // ### See de.deepamehta.core.impl.storage.MGTopic and de.deepamehta.core.impl.service.AttachedTopic.
    public DeepaMehtaObjectModel getModel() {
        return model;
    }

    protected final void setModel(DeepaMehtaObjectModel model) {
        this.model = model;
    }

    // ---

    protected abstract String className();

    protected abstract void storeUri(String uri);

    protected abstract void storeTypeUri();

    protected abstract SimpleValue storeValue(SimpleValue value);

    protected abstract void indexValue(IndexMode indexMode, String indexKey, SimpleValue value, SimpleValue oldValue);

    protected abstract Type getType();

    protected abstract RoleModel createRoleModel(String roleTypeUri);



    // ----------------------------------------------------------------------------------------- Package Private Methods

    void store(ClientState clientState, Directives directives) {
        if (getType().getDataTypeUri().equals("dm4.core.composite")) {
            CompositeValue comp = getCompositeValue();
            // Note: we build the composite value memory representation from scratch.
            // Uninitialized IDs, URIs, and values in the TopicModels must be replaced with the real ones.
            // In case of many-relationships the TopicModel arrays can not be updated incrementally because
            // they can't be looked up by ID (because topics to be created have no ID yet.)
            model.setCompositeValue(new CompositeValue());
            updateCompositeValue(comp, clientState, directives);
            refreshLabel();
        } else {
            storeAndIndexValue(getSimpleValue());
        }
    }

    /**
     * Called from {@link EmbeddedService#attach} (indirectly)
     */
    void loadComposite() {
        // fetch from DB
        CompositeValue comp = fetchComposite();
        // update memory
        model.setCompositeValue(comp);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    // === Update ===

    private void updateUri(String newUri) {
        if (newUri != null) {
            String uri = getUri();
            if (!uri.equals(newUri)) {
                logger.info("### Changing URI from \"" + uri + "\" -> \"" + newUri + "\"");
                setUri(newUri);
            }
        }
    }

    private void updateTypeUri(String newTypeUri, ChangeReport report) {
        if (newTypeUri != null) {
            String typeUri = getTypeUri();
            if (!typeUri.equals(newTypeUri)) {
                logger.info("### Changing type URI from \"" + typeUri + "\" -> \"" + newTypeUri + "\"");
                report.typeUriChanged(typeUri, newTypeUri);
                setTypeUri(newTypeUri);
            }
        }
    }

    private void updateSimpleValue(SimpleValue newValue) {
        if (newValue != null) {
            SimpleValue value = getSimpleValue();
            if (!value.equals(newValue)) {
                logger.info("### Changing simple value from \"" + value + "\" -> \"" + newValue + "\"");
                setSimpleValue(newValue);
            }
        }
    }

    // ---

    private void updateCompositeValue(CompositeValue newComp, ClientState clientState, Directives directives) {
        try {
            for (AssociationDefinition assocDef : getType().getAssocDefs().values()) {
                String assocDefUri    = assocDef.getUri();
                String cardinalityUri = assocDef.getPartCardinalityUri();
                TopicModel newChildTopic        = null;     // only used for "one"
                List<TopicModel> newChildTopics = null;     // only used for "many"
                boolean one = false;
                if (cardinalityUri.equals("dm4.core.one")) {
                    newChildTopic = newComp.getTopic(assocDefUri, null);        // defaultValue=null
                    // skip if not contained in update request
                    if (newChildTopic == null) {
                        continue;
                    }
                    //
                    one = true;
                } else if (cardinalityUri.equals("dm4.core.many")) {
                    newChildTopics = newComp.getTopics(assocDefUri, null);      // defaultValue=null
                    // skip if not contained in update request
                    if (newChildTopics == null) {
                        continue;
                    }
                } else {
                    throw new RuntimeException("\"" + cardinalityUri + "\" is an unexpected cardinality URI");
                }
                //
                updateChildTopics(assocDef, one, newChildTopic, newChildTopics, clientState, directives);
            }
        } catch (Exception e) {
            throw new RuntimeException("Updating the composite value of " + className() + " " + getId() +
                " failed (newComp=" + newComp + ")", e);
        }
    }

    private void updateChildTopics(AssociationDefinition assocDef, boolean one, TopicModel newChildTopic,
                                   List<TopicModel> newChildTopics, ClientState clientState, Directives directives) {
        String assocTypeUri = assocDef.getTypeUri();
        if (assocTypeUri.equals("dm4.core.composition_def")) {
            if (one) {
                updateCompositionOne(assocDef, newChildTopic, clientState, directives);
            } else {
                updateCompositionMany(assocDef, newChildTopics, clientState, directives);
            }
        } else if (assocTypeUri.equals("dm4.core.aggregation_def")) {
            if (one) {
                updateAggregationOne(assocDef, newChildTopic, clientState, directives);
            } else {
                updateAggregationMany(assocDef, newChildTopics, clientState, directives);
            }
        } else {
            throw new RuntimeException("Association type \"" + assocTypeUri + "\" not supported");
        }
    }

    // --- Composition ---

    private void updateCompositionOne(AssociationDefinition assocDef, TopicModel newChildTopic, ClientState clientState,
                                                                                                Directives directives) {
        // 1) update DB
        Topic childTopic = fetchChildTopic(assocDef, newChildTopic);
        if (childTopic != null) {
            // update existing child
            childTopic.update(newChildTopic, clientState, directives);
        } else {
            // create new child
            childTopic = dms.createTopic(newChildTopic, clientState);
            associateChildTopic(assocDef, childTopic.getId(), clientState);
        }
        // 2) update memory
        updateCompositeModel(assocDef, childTopic.getModel());
    }

    private void updateCompositionMany(AssociationDefinition assocDef, List<TopicModel> newChildTopics,
                                                                       ClientState clientState, Directives directives) {
        for (TopicModel newChildTopic : newChildTopics) {
            if (newChildTopic instanceof TopicDeletionModel) {
                deleteChildTopic(assocDef, newChildTopic, clientState, directives);
            } else {
                updateCompositionOne(assocDef, newChildTopic, clientState, directives);
            }
        }
    }

    private void deleteChildTopic(AssociationDefinition assocDef, TopicModel childTopic, ClientState clientState,
                                                                                         Directives directives) {
        // 1) update DB
        dms.getTopic(childTopic.getId(), false, null).delete(directives);       // fetchComposite=false
        // 2) update memory
        updateCompositeModelDeletion(assocDef, childTopic);
    }

    // ---

    /**
     * Updates memory.
     */
    private void updateCompositeModel(AssociationDefinition assocDef, TopicModel topic) {
        CompositeValue comp = getCompositeValue();
        String assocDefUri = assocDef.getUri();
        String cardinalityUri = assocDef.getPartCardinalityUri();
        //
        if (cardinalityUri.equals("dm4.core.one")) {
            comp.put(assocDefUri, topic);
        } else if (cardinalityUri.equals("dm4.core.many")) {
            List<TopicModel> topics = comp.getTopics(assocDefUri, null);        // defaultValue=null
            // Note: topics just created have no child topics yet
            if (topics == null) {
                topics = new ArrayList<TopicModel>();
                comp.put(assocDefUri, topics);
            }
            topics.remove(topic);
            topics.add(topic);
        } else {
            throw new RuntimeException("\"" + cardinalityUri + "\" is an unexpected cardinality URI");
        }
    }

    /**
     * Updates memory.
     */
    private void updateCompositeModelDeletion(AssociationDefinition assocDef, TopicModel topic) {
        CompositeValue comp = getCompositeValue();
        String assocDefUri = assocDef.getUri();
        //
        List<TopicModel> topics = comp.getTopics(assocDefUri, null);            // defaultValue=null
        topics.remove(topic);
    }

    // --- Aggregation ---

    private void updateAggregationOne(AssociationDefinition assocDef, TopicModel newChildTopic, ClientState clientState,
                                                                                                Directives directives) {
        // 1) update DB
        // remove current assignment
        RelatedTopic childTopic = fetchChildTopic(assocDef, false);             // fetchComposite=false
        if (childTopic != null) {
            childTopic.getAssociation().delete(directives);
        }
        // create new assignment
        Topic topic = createAssignment(assocDef, newChildTopic, clientState);
        // 2) update memory
        updateCompositeModelOne(assocDef, topic.getModel());
    }

    private void updateAggregationMany(AssociationDefinition assocDef, List<TopicModel> newChildTopics,
                                                                       ClientState clientState, Directives directives) {
        // 1) update DB
        // remove current assignments
        // ### FIXME: in case of "aggregation many" we expect the update request to contain *all* the childs.
        // ### Childs not contained in the update request are removed.
        for (RelatedTopic childTopic : fetchChildTopics(assocDef, false)) {     // fetchComposite=false
            childTopic.getAssociation().delete(directives);
        }
        // create new assignments
        List<TopicModel> topics = new ArrayList<TopicModel>();
        for (TopicModel newChildTopic : newChildTopics) {
            Topic topic = createAssignment(assocDef, newChildTopic, clientState);
            topics.add(topic.getModel());
        }
        // 2) update memory
        updateCompositeModelMany(assocDef, topics);
    }

    /**
     * Updates the DB.
     */
    Topic createAssignment(AssociationDefinition assocDef, TopicModel newChildTopic, ClientState clientState) {
        long childTopicId = newChildTopic.getId();
        String childTopicUri = newChildTopic.getUri();
        if (childTopicId != -1) {
            // assign existing child (ref'd by ID)
            associateChildTopic(assocDef, childTopicId, clientState);
            return fetchChildTopic(assocDef, childTopicId, false);              // fetchComposite=false
        } else if (!childTopicUri.equals("")) {
            // assign existing child (ref'd by URI)
            associateChildTopic(assocDef, childTopicUri, clientState);
            return fetchChildTopic(assocDef, childTopicUri, false);             // fetchComposite=false
        } else {
            // create new child
            Topic childTopic = dms.createTopic(newChildTopic, clientState);
            associateChildTopic(assocDef, childTopic.getId(), clientState);
            return childTopic;
        }
    }

    // ---

    /**
     * Updates memory.
     */
    private void updateCompositeModelOne(AssociationDefinition assocDef, TopicModel topic) {
        getCompositeValue().put(assocDef.getUri(), topic);
    }

    /**
     * Updates memory.
     */
    private void updateCompositeModelMany(AssociationDefinition assocDef, List<TopicModel> topics) {
        getCompositeValue().put(assocDef.getUri(), topics);
    }

    // === Fetch ===

    private CompositeValue fetchComposite() {
        try {
            CompositeValue comp = new CompositeValue();
            for (AssociationDefinition assocDef : getType().getAssocDefs().values()) {
                String cardinalityUri = assocDef.getPartCardinalityUri();
                if (cardinalityUri.equals("dm4.core.one")) {
                    Topic childTopic = fetchChildTopic(assocDef, true);                     // fetchComposite=true
                    if (childTopic != null) {
                        comp.put(assocDef.getUri(), childTopic.getModel());
                    }
                } else if (cardinalityUri.equals("dm4.core.many")) {
                    ResultSet<RelatedTopic> childTopics = fetchChildTopics(assocDef, true); // fetchComposite=true
                    comp.put(assocDef.getUri(), DeepaMehtaUtils.toTopicModels(childTopics));
                } else {
                    throw new RuntimeException("\"" + cardinalityUri + "\" is an unexpected cardinality URI");
                }
            }
            return comp;
        } catch (Exception e) {
            throw new RuntimeException("Fetching the " + className() + "'s composite failed (" + this + ")", e);
        }
    }

    // ---

    /**
     * Fetches and returns a child topic or <code>null</code> if no such topic extists.
     */
    private RelatedTopic fetchChildTopic(String assocDefUri, boolean fetchComposite) {
        return fetchChildTopic(getAssocDef(assocDefUri), fetchComposite);
    }

    /**
     * Fetches and returns a child topic or <code>null</code> if no such topic extists.
     */
    private RelatedTopic fetchChildTopic(AssociationDefinition assocDef, boolean fetchComposite) {
        String assocTypeUri       = assocDef.getInstanceLevelAssocTypeUri();
        String myRoleTypeUri      = assocDef.getWholeRoleTypeUri();
        String othersRoleTypeUri  = assocDef.getPartRoleTypeUri();
        String othersTopicTypeUri = assocDef.getPartTopicTypeUri();
        //
        return getRelatedTopic(assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri, fetchComposite,
            false, null);
    }

    /**
     * Fetches and returns the child topic with the specified ID. If that topic is not a child topic of this object
     * according to the specified association definition an exception is thrown.
     */
    private Topic fetchChildTopic(AssociationDefinition assocDef, long childTopicId, boolean fetchComposite) {
        Topic childTopic = dms.getTopic(childTopicId, fetchComposite, null);                // clientState=null
        // error check
        if (getAssociation(assocDef, childTopicId) == null) {
            throw new RuntimeException("Topic " + childTopicId + " is not a child of " + className() +
                " " + getId() + " according to " + assocDef);
        }
        //
        return childTopic;
    }

    /**
     * Fetches and returns the child topic with the specified URI. If that topic is not a child topic of this object
     * according to the specified association definition an exception is thrown.
     */
    private Topic fetchChildTopic(AssociationDefinition assocDef, String childTopicUri, boolean fetchComposite) {
        Topic childTopic = dms.getTopic("uri", new SimpleValue(childTopicUri), fetchComposite, null);
        // error check                                                                      // clientState=null
        if (getAssociation(assocDef, childTopic.getId()) == null) {
            throw new RuntimeException("Topic with URI \"" + childTopicUri + "\" is not a child of " + className() +
                " " + getId() + " according to " + assocDef);
        }
        //
        return childTopic;
    }

    /**
     * Fetches and returns the child topic that matches an update topic model,
     * or <code>null</code> if no such topic extists.
     */
    private Topic fetchChildTopic(AssociationDefinition assocDef, TopicModel newChildTopic) {
        String cardinalityUri = assocDef.getPartCardinalityUri();
        if (cardinalityUri.equals("dm4.core.one")) {
            // Note: for cardinality one the simple request format is sufficient. The child's topic ID is not required.
            // ### TODO: possibly sanity check: if child's topic ID *is* provided it must match with the fetched topic.
            return fetchChildTopic(assocDef, true);                             // fetchComposite=true
        } else {
            long childTopicId = newChildTopic.getId();
            if (childTopicId != -1) {
                // Note: the child topic's composite must be fetched. It needs to be passed to the
                // POST_UPDATE_TOPIC hook as part of the "old model" (when the child topic is updated). ### FIXDOC
                return fetchChildTopic(assocDef, childTopicId, true);           // fetchComposite=true
            } else {
                return null;
            }
        }
    }

    // ---

    private ResultSet<RelatedTopic> fetchChildTopics(AssociationDefinition assocDef, boolean fetchComposite) {
        String assocTypeUri       = assocDef.getInstanceLevelAssocTypeUri();
        String myRoleTypeUri      = assocDef.getWholeRoleTypeUri();
        String othersRoleTypeUri  = assocDef.getPartRoleTypeUri();
        String othersTopicTypeUri = assocDef.getPartTopicTypeUri();
        //
        return getRelatedTopics(assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri,
            fetchComposite, false, 0, null);
    }

    // ---

    private SimpleValue fetchChildTopicValue(AssociationDefinition assocDef) {
        Topic childTopic = fetchChildTopic(assocDef, false);                    // fetchComposite=false
        if (childTopic != null) {
            return childTopic.getSimpleValue();
        }
        return null;
    }

    // === Store ===

    /**
     * Stores a child's topic value in the database. If the child topic does not exist it is created.
     *
     * @param   assocDefUri     The "axis" that leads to the child: the URI of an {@link AssociationDefinition}.
     * @param   value           The value to set. If <code>null</code> nothing is set. The child topic is potentially
     *                          created and returned anyway.
     *
     * @return  The child topic.
     */
    private Topic storeChildTopicValue(String assocDefUri, final SimpleValue value) {
        try {
            AssociationDefinition assocDef = getAssocDef(assocDefUri);
            Topic childTopic = fetchChildTopic(assocDef, false);    // fetchComposite=false
            if (childTopic != null) {
                if (value != null) {
                    childTopic.setSimpleValue(value);
                }
            } else {
                // create child topic
                String topicTypeUri = assocDef.getPartTopicTypeUri();
                childTopic = dms.createTopic(new TopicModel(topicTypeUri, value), null);  // ### FIXME: clientState=null
                // associate child topic
                associateChildTopic(assocDef, childTopic.getId(), null);                  // ### FIXME: clientState=null
            }
            return childTopic;
        } catch (Exception e) {
            throw new RuntimeException("Storing child topic value failed (parentTopic=" + this +
                ",\nassocDefUri=" + assocDefUri + ",\nvalue=\"" + value + "\")", e);
        }
    }

    private void storeAndIndexValue(SimpleValue value) {
        SimpleValue oldValue = storeValue(value);               // abstract
        indexValue(value, oldValue);
    }

    private void indexValue(SimpleValue value, SimpleValue oldValue) {
        Type type = getType();
        String indexKey = type.getUri();
        // strip HTML tags before indexing
        if (type.getDataTypeUri().equals("dm4.core.html")) {
            value = new SimpleValue(JavaUtils.stripHTML(value.toString()));
            if (oldValue != null) {
                oldValue = new SimpleValue(JavaUtils.stripHTML(oldValue.toString()));
            }
        }
        //
        for (IndexMode indexMode : type.getIndexModes()) {
            indexValue(indexMode, indexKey, value, oldValue);   // abstract
        }
    }

    // === Label ===

    /**
     * Prerequisite: this is a composite object.
     */
    private void refreshLabel() {
        try {
            String label;
            // does the type have a label configuration?
            if (getType().getLabelConfig().size() > 0) {
                label = buildLabel();
            } else {
                label = buildDefaultLabel();
            }
            //
            setSimpleValue(label);
        } catch (Exception e) {
            throw new RuntimeException("Refreshing the " + className() + "'s label failed", e);
        }
    }

    /**
     * Builds this object's label according to its type's label configuration.
     */
    private String buildLabel() {
        Type type = getType();
        if (type.getDataTypeUri().equals("dm4.core.composite")) {
            StringBuilder label = new StringBuilder();
            for (String assocDefUri : type.getLabelConfig()) {
                Topic childTopic = fetchChildTopic(assocDefUri, false);     // fetchComposite=false
                // Note: topics just created have no child topics yet
                if (childTopic != null) {
                    String l = ((AttachedDeepaMehtaObject) childTopic).buildLabel();
                    // add separator
                    if (label.length() > 0 && l.length() > 0) {
                        label.append(LABEL_SEPARATOR);
                    }
                    //
                    label.append(l);
                }
            }
            return label.toString();
        } else {
            return getSimpleValue().toString();
        }
    }

    private String buildDefaultLabel() {
        Type type = getType();
        if (type.getDataTypeUri().equals("dm4.core.composite")) {
            Iterator<AssociationDefinition> i = type.getAssocDefs().values().iterator();
            // Note: types just created might have no child types yet
            if (i.hasNext()) {
                AssociationDefinition assocDef = i.next();
                Topic childTopic = fetchChildTopic(assocDef, false);        // fetchComposite=false
                // Note: topics just created have no child topics yet
                if (childTopic != null) {
                    return ((AttachedDeepaMehtaObject) childTopic).buildDefaultLabel();
                }
            }
            return "";
        } else {
            return getSimpleValue().toString();
        }
    }

    // === Helper ===

    private void associateChildTopic(AssociationDefinition assocDef, long childTopicId, ClientState clientState) {
        dms.createAssociation(assocDef.getInstanceLevelAssocTypeUri(),
            createRoleModel(assocDef.getWholeRoleTypeUri()),
            new TopicRoleModel(childTopicId, assocDef.getPartRoleTypeUri()), clientState
        );
    }

    private void associateChildTopic(AssociationDefinition assocDef, String childTopicUri, ClientState clientState) {
        dms.createAssociation(assocDef.getInstanceLevelAssocTypeUri(),
            createRoleModel(assocDef.getWholeRoleTypeUri()),
            new TopicRoleModel(childTopicUri, assocDef.getPartRoleTypeUri()), clientState
        );
    }

    // ---

    private Association getAssociation(AssociationDefinition assocDef, long childTopicId) {
        String assocTypeUri      = assocDef.getInstanceLevelAssocTypeUri();
        String myRoleTypeUri     = assocDef.getWholeRoleTypeUri();
        String othersRoleTypeUri = assocDef.getPartRoleTypeUri();
        return getAssociation(assocTypeUri, myRoleTypeUri, othersRoleTypeUri, childTopicId);
    }

    // ---

    private AssociationDefinition getAssocDef(String assocDefUri) {
        return getType().getAssocDef(assocDefUri);
    }
}
