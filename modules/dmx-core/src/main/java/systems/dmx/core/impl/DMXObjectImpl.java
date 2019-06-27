package systems.dmx.core.impl;

import systems.dmx.core.CompDef;
import systems.dmx.core.DMXObject;
import systems.dmx.core.DMXType;
import systems.dmx.core.RelatedAssoc;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.SimpleValue;

import org.codehaus.jettison.json.JSONObject;

import java.util.List;



/**
 * A DMX object model that is attached to the DB.
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
abstract class DMXObjectImpl implements DMXObject {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    DMXObjectModelImpl model;    // underlying model

    AccessLayer al;
    ModelFactoryImpl mf;

    // ---------------------------------------------------------------------------------------------------- Constructors

    DMXObjectImpl(DMXObjectModelImpl model, AccessLayer al) {
        this.model = model;
        this.al = al;
        this.mf = al.mf;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *****************
    // *** DMXObject ***
    // *****************



    // === Model ===

    // --- ID ---

    @Override
    public final long getId() {
        return model.getId();
    }

    // --- URI ---

    @Override
    public final String getUri() {
        return model.getUri();
    }

    @Override
    public final void setUri(String uri) {
        model.updateUri(uri);
    }

    // --- Type URI ---

    @Override
    public final String getTypeUri() {
        return model.getTypeUri();
    }

    @Override
    public final void setTypeUri(String typeUri) {
        model.updateTypeUri(typeUri);
    }

    // --- Simple Value ---

    @Override
    public final SimpleValue getSimpleValue() {
        return model.getSimpleValue();
    }

    // ---

    @Override
    public final void setSimpleValue(String value) {
        setSimpleValue(new SimpleValue(value));
    }

    @Override
    public final void setSimpleValue(int value) {
        setSimpleValue(new SimpleValue(value));
    }

    @Override
    public final void setSimpleValue(long value) {
        setSimpleValue(new SimpleValue(value));
    }

    @Override
    public final void setSimpleValue(boolean value) {
        setSimpleValue(new SimpleValue(value));
    }

    @Override
    public final void setSimpleValue(SimpleValue value) {
        model.updateSimpleValue(value);     // TODO: call model._updateSimpleValue() instead?
    }

    // --- Child Topics ---

    @Override
    public final ChildTopicsImpl getChildTopics() {
        return new ChildTopicsImpl(model.childTopics, model, al);
    }

    @Override
    public final void setChildTopics(ChildTopicsModel childTopics) {
        model.updateChildTopics(childTopics);
    }

    // ---

    @Override
    public DMXObject loadChildTopics() {
        model.loadChildTopics(true);                // deep=true
        return this;
    }

    @Override
    public DMXObject loadChildTopics(String compDefUri) {
        model.loadChildTopics(compDefUri, true);   // deep=true
        return this;
    }

    // ---

    @Override
    public final DMXType getType() {
        return model.getType().instantiate();
    }

    @Override
    public DMXObjectModelImpl getModel() {
        return model;
    }



    // === Updating ===

    @Override
    public final void updateChildTopics(ChildTopicsModel updateModel, CompDef compDef) {
        model.updateChildTopics(updateModel, compDef.getModel());
    }



    // === Traversal ===

    // --- Topic Retrieval ---

    @Override
    public final RelatedTopic getRelatedTopic(String assocTypeUri, String myRoleTypeUri,
                                              String othersRoleTypeUri, String othersTopicTypeUri) {
        RelatedTopicModelImpl topic = model.getRelatedTopic(assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri);
        return topic != null ? topic.checkReadAccess().instantiate() : null;
    }

    @Override
    public final List<RelatedTopic> getRelatedTopics() {
        return getRelatedTopics(null, null, null, null);
    }

    @Override
    public final List<RelatedTopic> getRelatedTopics(String assocTypeUri, String myRoleTypeUri,
                                                     String othersRoleTypeUri, String othersTopicTypeUri) {
        List<RelatedTopicModelImpl> topics = model.getRelatedTopics(assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri);
        return al.instantiate(al.filterReadables(topics));
    }

    // --- Assoc Retrieval ---

    @Override
    public final List<RelatedAssoc> getRelatedAssocs() {
        return getRelatedAssocs(null, null, null, null);
    }

    // Note: these methods are implemented in the subclasses (this is an abstract class):
    //     getRelatedAssoc(...)
    //     getRelatedAssocs(...)
    //     getAssoc(...)
    //     getAssocs()



    // === Properties ===

    @Override
    public final Object getProperty(String propUri) {
        return al.db.fetchProperty(getId(), propUri);
    }

    @Override
    public final boolean hasProperty(String propUri) {
        return al.db.hasProperty(getId(), propUri);
    }

    @Override
    public final void setProperty(String propUri, Object propValue, boolean addToIndex) {
        model.storeProperty(propUri, propValue, addToIndex);
    }

    @Override
    public final void removeProperty(String propUri) {
        model.removeProperty(propUri);
    }



    // === Misc ===

    @Override
    public final Object getDatabaseVendorObject() {
        return al.db.getDatabaseVendorObject(getId());
    }



    // *******************
    // *** JSONEnabled ***
    // *******************



    @Override
    public final JSONObject toJSON() {
        return model.toJSON();
    }



    // ****************
    // *** Java API ***
    // ****************



    @Override
    public final boolean equals(Object o) {
        return ((DMXObjectImpl) o).model.equals(model);
    }

    @Override
    public final int hashCode() {
        return model.hashCode();
    }

    @Override
    public final String toString() {
        return model.toString();
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    final String className() {
        return model.className();
    }
}
