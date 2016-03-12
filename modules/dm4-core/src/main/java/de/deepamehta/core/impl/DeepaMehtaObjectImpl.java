package de.deepamehta.core.impl;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.service.ResultList;

import org.codehaus.jettison.json.JSONObject;

import java.util.List;



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
abstract class DeepaMehtaObjectImpl implements DeepaMehtaObject {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    DeepaMehtaObjectModelImpl model;    // underlying model

    PersistenceLayer pl;
    ModelFactoryImpl mf;

    // ---------------------------------------------------------------------------------------------------- Constructors

    DeepaMehtaObjectImpl(DeepaMehtaObjectModelImpl model, PersistenceLayer pl) {
        this.model = model;
        this.pl = pl;
        this.mf = pl.mf;
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
        model.updateUri(uri);
    }

    // --- Type URI ---

    @Override
    public String getTypeUri() {
        return model.getTypeUri();
    }

    @Override
    public void setTypeUri(String typeUri) {
        model.updateTypeUri(typeUri);
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
        model.updateSimpleValue(value);
    }

    // --- Child Topics ---

    @Override
    public ChildTopicsImpl getChildTopics() {
        return new ChildTopicsImpl(model.childTopics, model, pl);
    }

    // ### FIXME: no UPDATE directive for *this* object is added. No UPDATE event for *this* object is fired.
    // We should call the abstract updateChildTopics() instead.
    @Override
    public void setChildTopics(ChildTopicsModel childTopics) {
        try {
            model._updateChildTopics(childTopics);
        } catch (Exception e) {
            throw new RuntimeException("Setting the child topics failed (" + childTopics + ")", e);
        }
    }

    // ---

    @Override
    public DeepaMehtaObject loadChildTopics() {
        model.loadChildTopics();
        return this;
    }

    @Override
    public DeepaMehtaObject loadChildTopics(String assocDefUri) {
        model.loadChildTopics(assocDefUri);
        return this;
    }

    // ---

    // Note: getType() remains abstract

    @Override
    public DeepaMehtaObjectModelImpl getModel() {
        return model;
    }



    // === Updating ===

    @Override
    public final void update(DeepaMehtaObjectModel newModel) {
        model.update(newModel);
    }

    // ---

    // ### FIXME: no UPDATE directive for *this* object is added. No UPDATE event for *this* object is fired.
    // Directives/events is handled only in the high-level update() method.
    // Here however we need to call the low-level updateChildTopics() method in order to pass an arbitrary assoc def.
    @Override
    public void updateChildTopic(RelatedTopicModel newChildTopic, AssociationDefinition assocDef) {
        model.updateChildTopics(newChildTopic, null, assocDef.getModel());      // newChildTopics=null
    }

    // ### FIXME: no UPDATE directive for *this* object is added. No UPDATE event for *this* object is fired.
    // Directives/events is handled only in the high-level update() method.
    // Here however we need to call the low-level updateChildTopics() method in order to pass an arbitrary assoc def.
    @Override
    public void updateChildTopics(List<? extends RelatedTopicModel> newChildTopics, AssociationDefinition assocDef) {
        model.updateChildTopics(null, newChildTopics, assocDef.getModel());     // newChildTopic=null
    }



    // === Deletion ===

    @Override
    public final void delete() {
        model.delete();
    }



    // === Traversal ===

    // --- Topic Retrieval ---

    @Override
    public RelatedTopic getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                   String othersTopicTypeUri) {
        RelatedTopicModelImpl topic = model.getRelatedTopic(assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri);
        return topic != null ? pl.<RelatedTopic>checkReadAccessAndInstantiate(topic) : null;
    }

    @Override
    public ResultList<RelatedTopic> getRelatedTopics(String assocTypeUri) {
        return getRelatedTopics(assocTypeUri, null, null, null);
    }

    @Override
    public ResultList<RelatedTopic> getRelatedTopics(String assocTypeUri, String myRoleTypeUri,
                                                     String othersRoleTypeUri, String othersTopicTypeUri) {
        ResultList<RelatedTopicModelImpl> topics = model.getRelatedTopics(assocTypeUri, myRoleTypeUri,
            othersRoleTypeUri, othersTopicTypeUri);
        return new ResultList(pl.checkReadAccessAndInstantiate(topics));
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
        return pl.fetchProperty(getId(), propUri);
    }

    @Override
    public boolean hasProperty(String propUri) {
        return pl.hasProperty(getId(), propUri);
    }

    // Note: these methods are implemented in the subclasses:
    //     setProperty(...)
    //     removeProperty(...)



    // === Misc ===

    @Override
    public Object getDatabaseVendorObject() {
        return pl.getDatabaseVendorObject(getId());
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
        return ((DeepaMehtaObjectImpl) o).model.equals(model);
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

    final String className() {
        return model.className();
    }
}
