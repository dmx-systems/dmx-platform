package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;

import org.codehaus.jettison.json.JSONObject;

import java.util.List;
import java.util.logging.Logger;



/**
 * A DeepaMehta object model that is attached to the DB.
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

    private DeepaMehtaObjectModel model;            // underlying model

    private AttachedCompositeValue childTopics;     // attached object cache

    protected final EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedDeepaMehtaObject(DeepaMehtaObjectModel model, EmbeddedService dms) {
        this.model = model;
        this.dms = dms;
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
        DeepaMehtaTransaction tx = dms.beginTx();   // ### TODO: only resource methods should create a transaction
        try {
            getCompositeValue().update(comp, clientState, directives);
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
    public void loadChildTopics() {
        getCompositeValue().loadChildTopics();
    }

    @Override
    public void loadChildTopics(String childTypeUri) {
        getCompositeValue().loadChildTopics(childTypeUri);
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
        updateValue(newModel, clientState, directives);
    }

    // ---

    @Override
    public void updateChildTopic(TopicModel newChildTopic, AssociationDefinition assocDef,
                                                           ClientState clientState, Directives directives) {
        getCompositeValue().updateChildTopics(newChildTopic, null, assocDef, clientState, directives);
    }

    @Override
    public void updateChildTopics(List<TopicModel> newChildTopics, AssociationDefinition assocDef,
                                                                   ClientState clientState, Directives directives) {
        getCompositeValue().updateChildTopics(null, newChildTopics, assocDef, clientState, directives);
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
        ResultList<RelatedTopic> childTopics = getRelatedTopics("dm4.core.composition",
            "dm4.core.parent", "dm4.core.child", null, false, false, 0);
        for (Topic childTopic : childTopics) {
            childTopic.delete(directives);
        }
        // 2) delete direct associations
        for (Association assoc : getAssociations()) {       // getAssociations() is abstract
            assoc.delete(directives);
        }
    }



    // === Traversal ===

    // --- Topic Retrieval ---

    @Override
    public RelatedTopic getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                String othersTopicTypeUri, boolean fetchComposite,
                                                boolean fetchRelatingComposite) {
        RelatedTopicModel topic = fetchRelatedTopic(assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri);
        // fetchRelatedTopic() is abstract
        return topic != null ? dms.instantiateRelatedTopic(topic, fetchComposite, fetchRelatingComposite) : null;
    }

    @Override
    public ResultList<RelatedTopic> getRelatedTopics(String assocTypeUri, int maxResultSize) {
        return getRelatedTopics(assocTypeUri, null, null, null, false, false, maxResultSize);
    }

    @Override
    public ResultList<RelatedTopic> getRelatedTopics(String assocTypeUri, String myRoleTypeUri,
                                            String othersRoleTypeUri, String othersTopicTypeUri,
                                            boolean fetchComposite, boolean fetchRelatingComposite, int maxResultSize) {
        ResultList<RelatedTopicModel> topics = fetchRelatedTopics(assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri, maxResultSize);     // fetchRelatedTopics() is abstract
        return dms.instantiateRelatedTopics(topics, fetchComposite, fetchRelatingComposite);
    }

    // Note: this method is implemented in the subclasses (this is an abstract class):
    //     getRelatedTopics(List assocTypeUris, ...)

    // --- Association Retrieval ---

    // Note: these methods are implemented in the subclasses (this is an abstract class):
    //     getAssociation(...);
    //     getAssociations();



    // === Misc ===

    @Override
    public Object getDatabaseVendorObject() {
        return dms.storageDecorator.getDatabaseVendorObject(getId());
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

    // ### TODO: Directive getUpdateDirective()
    abstract void addUpdateDirective(Directives directives);

    abstract void storeUri();

    abstract void storeTypeUri();

    // ---

    abstract RelatedTopicModel fetchRelatedTopic(String assocTypeUri, String myRoleTypeUri,
                                                String othersRoleTypeUri, String othersTopicTypeUri);

    abstract ResultList<RelatedTopicModel> fetchRelatedTopics(String assocTypeUri, String myRoleTypeUri,
                                                String othersRoleTypeUri, String othersTopicTypeUri, int maxResultSize);

    // ---

    // ### TODO: add to public interface
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
            logger.info("### Changing URI of " + className() + " " + getId() +
                " from \"" + uri + "\" -> \"" + newUri + "\"");
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
            logger.info("### Changing type URI of " + className() + " " + getId() +
                " from \"" + typeUri + "\" -> \"" + newTypeUri + "\"");
            setTypeUri(newTypeUri);
        }
    }

    private void updateValue(DeepaMehtaObjectModel newModel, ClientState clientState, Directives directives) {
        if (getType().getDataTypeUri().equals("dm4.core.composite")) {
            getCompositeValue().update(newModel.getCompositeValueModel(), clientState, directives);
        } else {
            updateSimpleValue(newModel.getSimpleValue());
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
            logger.info("### Changing simple value of " + className() + " " + getId() +
                " from \"" + value + "\" -> \"" + newValue + "\"");
            setSimpleValue(newValue);
        }
    }
}
