package de.deepamehta.core.impl;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.ResultList;

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

    private DeepaMehtaObjectModel model;        // underlying model

    private AttachedChildTopics childTopics;    // attached object cache

    protected final EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedDeepaMehtaObject(DeepaMehtaObjectModel model, EmbeddedService dms) {
        this.model = model;
        this.dms = dms;
        this.childTopics = new AttachedChildTopics(model.getChildTopicsModel(), this, dms);
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

    // --- Child Topics ---

    @Override
    public AttachedChildTopics getChildTopics() {
        return childTopics;
    }

    // ### FIXME: no UPDATE directive for *this* object is added. No UPDATE event for *this* object is fired.
    // We should call the abstract updateChildTopics() instead.
    @Override
    public void setChildTopics(ChildTopicsModel childTopics) {
        try {
            getChildTopics().update(childTopics);
        } catch (Exception e) {
            throw new RuntimeException("Setting the child topics failed (" + childTopics + ")", e);
        }
    }

    // ---

    @Override
    public DeepaMehtaObject loadChildTopics() {
        getChildTopics().loadChildTopics();
        return this;
    }

    @Override
    public DeepaMehtaObject loadChildTopics(String childTypeUri) {
        getChildTopics().loadChildTopics(childTypeUri);
        return this;
    }

    // ---

    @Override
    public DeepaMehtaObjectModel getModel() {
        return model;
    }



    // === Updating ===

    @Override
    public void update(DeepaMehtaObjectModel newModel) {
        updateUri(newModel.getUri());
        updateTypeUri(newModel.getTypeUri());
        if (getType().getDataTypeUri().equals("dm4.core.composite")) {
            getChildTopics().update(newModel.getChildTopicsModel());
        } else {
            updateSimpleValue(newModel.getSimpleValue());
        }
        //
        Directives.get().add(getUpdateDirective(), this);
    }

    // ---

    // ### FIXME: no UPDATE directive for *this* object is added. No UPDATE event for *this* object is fired.
    // Directives/events is handled only in the high-level update() method.
    // Here however we need to call the low-level updateChildTopics() method in order to pass an arbitrary assoc def.
    @Override
    public void updateChildTopic(RelatedTopicModel newChildTopic, AssociationDefinition assocDef) {
        getChildTopics().updateChildTopics(newChildTopic, null, assocDef);      // newChildTopics=null
    }

    // ### FIXME: no UPDATE directive for *this* object is added. No UPDATE event for *this* object is fired.
    // Directives/events is handled only in the high-level update() method.
    // Here however we need to call the low-level updateChildTopics() method in order to pass an arbitrary assoc def.
    @Override
    public void updateChildTopics(List<RelatedTopicModel> newChildTopics, AssociationDefinition assocDef) {
        getChildTopics().updateChildTopics(null, newChildTopics, assocDef);     // newChildTopic=null
    }



    // === Deletion ===

    /**
     * Deletes 1) this DeepaMehta object's child topics (recursively) which have an underlying association definition of
     * type "Composition Definition" and 2) deletes all the remaining direct associations of this DeepaMehta object.
     * <p>
     * Note: deletion of the object itself is up to the subclasses.
     */
    @Override
    public void delete() {
        // 1) delete child topics (recursively)
        for (AssociationDefinition assocDef : getType().getAssocDefs()) {
            if (assocDef.getTypeUri().equals("dm4.core.composition_def")) {
                for (Topic childTopic : getRelatedTopics(assocDef.getInstanceLevelAssocTypeUri(),
                        "dm4.core.parent", "dm4.core.child", assocDef.getChildTypeUri())) {
                    childTopic.delete();
                }
            }
        }
        // 2) delete direct associations
        for (Association assoc : getAssociations()) {       // getAssociations() is abstract
            assoc.delete();
        }
    }



    // === Traversal ===

    // --- Topic Retrieval ---

    @Override
    public RelatedTopic getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                   String othersTopicTypeUri) {
        RelatedTopicModel topic = fetchRelatedTopic(assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri);
        // fetchRelatedTopic() is abstract
        return topic != null ? dms.instantiateRelatedTopic(topic) : null;
    }

    @Override
    public ResultList<RelatedTopic> getRelatedTopics(String assocTypeUri) {
        return getRelatedTopics(assocTypeUri, null, null, null);
    }

    @Override
    public ResultList<RelatedTopic> getRelatedTopics(String assocTypeUri, String myRoleTypeUri,
                                                     String othersRoleTypeUri, String othersTopicTypeUri) {
        ResultList<RelatedTopicModel> topics = fetchRelatedTopics(assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri);    // fetchRelatedTopics() is abstract
        return dms.instantiateRelatedTopics(topics);
    }

    // Note: this method is implemented in the subclasses (this is an abstract class):
    //     getRelatedTopics(List assocTypeUris, ...)

    // --- Association Retrieval ---

    // Note: these methods are implemented in the subclasses (this is an abstract class):
    //     getAssociation(...)
    //     getAssociations()



    // === Properties ===

    @Override
    public Object getProperty(String propUri) {
        return dms.getProperty(getId(), propUri);
    }

    @Override
    public boolean hasProperty(String propUri) {
        return dms.hasProperty(getId(), propUri);
    }

    // Note: these methods are implemented in the subclasses:
    //     setProperty(...)
    //     removeProperty(...)



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

    abstract void updateChildTopics(ChildTopicsModel childTopics);

    abstract Directive getUpdateDirective();

    abstract void storeUri();

    abstract void storeTypeUri();

    // ---

    abstract RelatedTopicModel fetchRelatedTopic(String assocTypeUri, String myRoleTypeUri,
                                                 String othersRoleTypeUri, String othersTopicTypeUri);

    abstract ResultList<RelatedTopicModel> fetchRelatedTopics(String assocTypeUri, String myRoleTypeUri,
                                                              String othersRoleTypeUri, String othersTopicTypeUri);

    // ---

    // ### TODO: add to public interface?
    abstract Type getType();

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
