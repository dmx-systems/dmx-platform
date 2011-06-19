package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;



abstract class AttachedDeepaMehtaObject implements DeepaMehtaObject {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DeepaMehtaObjectModel model;
    protected final EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedDeepaMehtaObject(EmbeddedService dms) {
        this(null, dms);    // ### The model remain uninitialized.
                            // ### They are initialized later on through fetch().
    }

    AttachedDeepaMehtaObject(DeepaMehtaObjectModel model, EmbeddedService dms) {
        this.model = model;
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ***************************************
    // *** DeepaMehtaObject Implementation ***
    // ***************************************



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
        // Note: updating the DB is up to the subclasses
    }

    // --- Value ---

    @Override
    public TopicValue getValue() {
        return model.getValue();
    }

    // ---

    @Override
    public void setValue(String value) {
        setValue(new TopicValue(value));
    }

    @Override
    public void setValue(int value) {
        setValue(new TopicValue(value));
    }

    @Override
    public void setValue(long value) {
        setValue(new TopicValue(value));
    }

    @Override
    public void setValue(boolean value) {
        setValue(new TopicValue(value));
    }

    @Override
    public void setValue(TopicValue value) {
        // update memory
        model.setValue(value);
        // Note: updating the DB is up to the subclasses
    }

    // --- Type URI ---

    @Override
    public String getTypeUri() {
        return model.getTypeUri();
    }

    @Override
    public void setTypeUri(String assocTypeUri) {
        // update memory
        model.setTypeUri(assocTypeUri);
        // Note: updating the DB is up to the subclasses
    }

    // --- Composite ---

    @Override
    public Composite getComposite() {
        return model.getComposite();
    }

    @Override
    public void setComposite(Composite comp) {
        // update memory
        model.setComposite(comp);
        // Note: updating the DB is up to the subclasses
    }



    // === Serialization ===

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

    // ### This is supposed to be protected, but doesn't compile!
    // ### It is called from the subclasses constructors, but on a differnt TopicBase instance.
    // ### See de.deepamehta.core.impl.storage.HGTopic and de.deepamehta.core.impl.service.AttachedTopic.
    public DeepaMehtaObjectModel getModel() {
        return model;
    }

    protected final void setModel(DeepaMehtaObjectModel model) {
        this.model = model;
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    // ------------------------------------------------------------------------------------------------- Private Methods

}
