package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.CompositeValue;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicDeletionModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;

import org.codehaus.jettison.json.JSONObject;

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
 *  - fetchXX()         Fetches value from DB.              ### FIXDOC
 *  - storeXX()         Stores current value (model) to DB. ### FIXDOC
 */
abstract class AttachedDeepaMehtaObject implements DeepaMehtaObject {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DeepaMehtaObjectModel model;
    protected final EmbeddedService dms;

    private AttachedCompositeValue childTopics;     // attached object cache
    private boolean isCompositeFetched;             // tracks lazy loading

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedDeepaMehtaObject(DeepaMehtaObjectModel model, EmbeddedService dms) {
        this.model = model;
        this.dms = dms;
        this.isCompositeFetched = model.getCompositeValueModel().size() > 0;    // ### FIXME: respect data type
        this.childTopics = new AttachedCompositeValue(model.getCompositeValueModel(), this, dms);
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
        storeUri();         // abstract
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
        dms.valueStorage.setSimpleValue(getModel(), value);
    }

    // --- Composite Value ---

    @Override
    public AttachedCompositeValue getCompositeValue() {
        return childTopics;
    }

    @Override
    public void setCompositeValue(CompositeValueModel comp, ClientState clientState, Directives directives) {
        DeepaMehtaTransaction tx = dms.beginTx();   // ### FIXME: all other writing API methods need transaction as well
        try {
            updateCompositeValue(comp, clientState, directives);
            dms.valueStorage.refreshLabel(getModel());
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
    public DeepaMehtaObjectModel getModel() {
        return model;
    }



    // === Updating ===

    @Override
    public void update(DeepaMehtaObjectModel newModel, ClientState clientState, Directives directives) {
        updateUri(newModel.getUri());
        updateTypeUri(newModel.getTypeUri());
        //
        if (getType().getDataTypeUri().equals("dm4.core.composite")) {
            updateCompositeValue(newModel.getCompositeValueModel(), clientState, directives);
            dms.valueStorage.refreshLabel(getModel());
        } else {
            updateSimpleValue(newModel.getSimpleValue());
        }
    }

    // ---

    @Override
    public void updateChildTopic(TopicModel newChildTopic, AssociationDefinition assocDef,
                                                           ClientState clientState, Directives directives) {
        // Note: updating the child topics requires them to be loaded
        requireChildTopics(assocDef);
        //
        updateChildTopics(newChildTopic, null, assocDef, clientState, directives);
    }

    @Override
    public void updateChildTopics(List<TopicModel> newChildTopics, AssociationDefinition assocDef,
                                                                   ClientState clientState, Directives directives) {
        // Note: updating the child topics requires them to be loaded
        requireChildTopics(assocDef);
        //
        updateChildTopics(null, newChildTopics, assocDef, clientState, directives);
    }



    // === Traversal ===

    // ### TODO: drop?
    @Override
    public SimpleValue getChildTopicValue(String assocDefUri) {
        return fetchChildTopicValue(getAssocDef(assocDefUri));
    }

    // ### TODO: drop?
    @Override
    public void setChildTopicValue(String assocDefUri, SimpleValue value) {
        // update memory
        getCompositeValue().getModel().put(assocDefUri, value.value());
        // update DB
        storeChildTopicValue(assocDefUri, value);
        //
        dms.valueStorage.refreshLabel(getModel());
    }

    // --- Topic Retrieval ---

    @Override
    public RelatedTopic getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                String othersTopicTypeUri, boolean fetchComposite,
                                                boolean fetchRelatingComposite, ClientState clientState) {
        RelatedTopicModel topic = fetchRelatedTopic(assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri);
        // fetchRelatedTopic() is abstract
        return topic != null ? dms.attach(topic, fetchComposite, fetchRelatingComposite, clientState) : null;
    }

    @Override
    public ResultSet<RelatedTopic> getRelatedTopics(String assocTypeUri, int maxResultSize, ClientState clientState) {
        return getRelatedTopics(assocTypeUri, null, null, null, false, false, maxResultSize, clientState);
    }

    @Override
    public ResultSet<RelatedTopic> getRelatedTopics(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                    String othersTopicTypeUri, boolean fetchComposite, boolean fetchRelatingComposite,
                                    int maxResultSize, ClientState clientState) {
        ResultSet<RelatedTopicModel> topics = fetchRelatedTopics(assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri, maxResultSize);     // fetchRelatedTopics() is abstract
        return dms.attach(topics, fetchComposite, fetchRelatingComposite, clientState);
    }

    // Note: this method is implemented in the subclasses (this is an abstract class):
    //     getRelatedTopics(List assocTypeUris, ...)

    // --- Association Retrieval ---

    // Note: these methods are implemented in the subclasses (this is an abstract class):
    //     getAssociation(...);
    //     getAssociations();



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
        for (Association assoc : getAssociations()) {       // getAssociations() is abstract
            assoc.delete(directives);
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



    // ----------------------------------------------------------------------------------------- Package Private Methods

    abstract String className();

    abstract void storeUri();

    abstract void storeTypeUri();

    // ---

    abstract RelatedTopicModel fetchRelatedTopic(String assocTypeUri, String myRoleTypeUri,
                                                String othersRoleTypeUri, String othersTopicTypeUri);

    abstract ResultSet<RelatedTopicModel> fetchRelatedTopics(String assocTypeUri, String myRoleTypeUri,
                                                String othersRoleTypeUri, String othersTopicTypeUri, int maxResultSize);

    // ---

    Type getType() {
        return dms.valueStorage.getType(getModel());
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Update ===

    private void updateUri(String newUri) {
        // abort if no update is requested
        if (newUri == null) {
            return;
        }
        //
        String uri = getUri();
        if (!uri.equals(newUri)) {
            logger.info("### Changing URI from \"" + uri + "\" -> \"" + newUri + "\"");
            setUri(newUri);
        }
    }

    private void updateTypeUri(String newTypeUri) {
        // abort if no update is requested
        if (newTypeUri == null) {
            return;
        }
        //
        String typeUri = getTypeUri();
        if (!typeUri.equals(newTypeUri)) {
            logger.info("### Changing type URI from \"" + typeUri + "\" -> \"" + newTypeUri + "\"");
            setTypeUri(newTypeUri);
        }
    }

    private void updateSimpleValue(SimpleValue newValue) {
        // abort if no update is requested
        if (newValue == null) {
            return;
        }
        //
        SimpleValue value = getSimpleValue();
        if (!value.equals(newValue)) {
            logger.info("### Changing simple value from \"" + value + "\" -> \"" + newValue + "\"");
            setSimpleValue(newValue);
        }
    }

    // ---

    private void updateCompositeValue(CompositeValueModel newComp, ClientState clientState, Directives directives) {
        try {
            // Note: updating the composite value requires it to be loaded
            requireCompositeValue();
            //
            for (AssociationDefinition assocDef : getType().getAssocDefs()) {
                String childTypeUri   = assocDef.getPartTypeUri();
                String cardinalityUri = assocDef.getPartCardinalityUri();
                TopicModel newChildTopic        = null;     // only used for "one"
                List<TopicModel> newChildTopics = null;     // only used for "many"
                if (cardinalityUri.equals("dm4.core.one")) {
                    newChildTopic = newComp.getTopic(childTypeUri, null);        // defaultValue=null
                    // skip if not contained in update request
                    if (newChildTopic == null) {
                        continue;
                    }
                } else if (cardinalityUri.equals("dm4.core.many")) {
                    newChildTopics = newComp.getTopics(childTypeUri, null);      // defaultValue=null
                    // skip if not contained in update request
                    if (newChildTopics == null) {
                        continue;
                    }
                } else {
                    throw new RuntimeException("\"" + cardinalityUri + "\" is an unexpected cardinality URI");
                }
                //
                updateChildTopics(newChildTopic, newChildTopics, assocDef, clientState, directives);
            }
        } catch (Exception e) {
            throw new RuntimeException("Updating composite value of " + className() + " " + getId() +
                " failed (newComp=" + newComp + ")", e);
        }
    }

    private void updateChildTopics(TopicModel newChildTopic, List<TopicModel> newChildTopics,
                                   AssociationDefinition assocDef, ClientState clientState, Directives directives) {
        String assocTypeUri = assocDef.getTypeUri();
        boolean one = newChildTopic != null;
        if (assocTypeUri.equals("dm4.core.composition_def")) {
            if (one) {
                getCompositeValue().updateCompositionOne(newChildTopic, assocDef, clientState, directives);
            } else {
                getCompositeValue().updateCompositionMany(newChildTopics, assocDef, clientState, directives);
            }
        } else if (assocTypeUri.equals("dm4.core.aggregation_def")) {
            if (one) {
                getCompositeValue().updateAggregationOne(newChildTopic, assocDef, clientState, directives);
            } else {
                getCompositeValue().updateAggregationMany(newChildTopics, assocDef, clientState, directives);
            }
        } else {
            throw new RuntimeException("Association type \"" + assocTypeUri + "\" not supported");
        }
    }



    // === Fetch ===

    /**
     * Lazy-loads the composite value (model) of this object and updates the attached object cache accordingly.
     */
    private void requireCompositeValue() {
        if (!isCompositeFetched) {
            logger.fine("### Lazy-loading composite value of " + className() + " " + getId());
            dms.valueStorage.fetchCompositeValue(getModel());
            childTopics.reinit();
            isCompositeFetched = true;
        }
    }

    /**
     * Lazy-loads certain child topics (model) of this object and updates the attached object cache accordingly.
     * Used for facet update.
     *
     * @param   assocDef    the child topics according to this association definition are loaded.
     *                      Note: the association definition must not necessarily originate from this object's
     *                      type definition.
     */
    private void requireChildTopics(AssociationDefinition assocDef) {
        String childTypeUri = assocDef.getPartTypeUri();
        logger.fine("### Lazy-loading \"" + childTypeUri + "\" child topic(s) of " + className() + " " + getId());
        dms.valueStorage.fetchChildTopics(getModel(), assocDef);
        childTopics.reinit(childTypeUri);
    }

    // ---

    /**
     * ### TODO: drop?
     * Fetches and returns a child topic or <code>null</code> if no such topic extists.
     */
    private RelatedTopic fetchChildTopic(AssociationDefinition assocDef, boolean fetchComposite) {
        String assocTypeUri  = assocDef.getInstanceLevelAssocTypeUri();
        String othersTypeUri = assocDef.getPartTypeUri();
        return getRelatedTopic(assocTypeUri, "dm4.core.whole", "dm4.core.part", othersTypeUri, fetchComposite,
            false, null);
    }

    // ### TODO: drop?
    private SimpleValue fetchChildTopicValue(AssociationDefinition assocDef) {
        Topic childTopic = fetchChildTopic(assocDef, false);                    // fetchComposite=false
        if (childTopic != null) {
            return childTopic.getSimpleValue();
        }
        return null;
    }



    // === Store ===

    /**
     * ### TODO: drop?
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
                String topicTypeUri = assocDef.getPartTypeUri();
                childTopic = dms.createTopic(new TopicModel(topicTypeUri, value), null);  // ### FIXME: clientState=null
                // associate child topic
                dms.valueStorage.associateChildTopic(childTopic.getId(), getModel(), assocDef, null); // ### FIXME: clie
            }
            return childTopic;
        } catch (Exception e) {
            throw new RuntimeException("Storing child topic value failed (assocDefUri=\"" + assocDefUri +
                "\", value=\"" + value + "\", parentTopic=" + this + ")", e);
        }
    }



    // === Helper ===

    private AssociationDefinition getAssocDef(String assocDefUri) {
        return getType().getAssocDef(assocDefUri);
    }
}
