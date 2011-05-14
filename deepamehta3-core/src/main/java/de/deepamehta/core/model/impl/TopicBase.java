package de.deepamehta.core.model.impl;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.RelatedTopic;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicValue;

import org.codehaus.jettison.json.JSONObject;

import java.util.Set;



public class TopicBase implements Topic {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private TopicModel model;

    // ---------------------------------------------------------------------------------------------------- Constructors

    protected TopicBase(TopicModel model) {
        this.model = model;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Topic Implementation ===

    @Override
    public long getId() {
        return model.getId();
    }

    @Override
    public String getUri() {
        return model.getUri();
    }

    @Override
    public TopicValue getValue() {
        return model.getValue();
    }

    @Override
    public String getTypeUri() {
        return model.getTypeUri();
    }

    @Override
    public Composite getComposite() {
        return model.getComposite();
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
        model.setValue(value);
    }

    // ---

    @Override
    public void setComposite(Composite comp) {
        model.setComposite(comp);
    }

    // --- Traversal ---

    @Override
    public TopicValue getChildTopicValue(String assocDefUri) {
        throw new RuntimeException("Topic is not attached to the core service (" + getClass() + ", " + this + ")");
    }

    @Override
    public void setChildTopicValue(String assocDefUri, TopicValue value) {
        throw new RuntimeException("Topic is not attached to the core service (" + getClass() + ", " + this + ")");
    }

    @Override
    public Set<RelatedTopic> getRelatedTopics(String assocTypeUri) {
        throw new RuntimeException("Topic is not attached to the core service (" + getClass() + ", " + this + ")");
    }

    @Override
    public Topic getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                            String othersTopicTypeUri,
                                                                            boolean fetchComposite) {
        throw new RuntimeException("Topic is not attached to the core service (" + getClass() + ", " + this + ")");
    }

    @Override
    public Set<RelatedTopic> getRelatedTopics(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                         String othersTopicTypeUri,
                                                                                         boolean fetchComposite) {
        throw new RuntimeException("Topic is not attached to the core service (" + getClass() + ", " + this + ")");
    }

    @Override
    public Set<Association> getAssociations(String myRoleTypeUri) {
        throw new RuntimeException("Topic is not attached to the core service (" + getClass() + ", " + this + ")");
    }

    // --- Serialization ---

    @Override
    public JSONObject toJSON() {
        return model.toJSON();
    }



    // === Java API ===

    @Override
    public boolean equals(Object o) {
        return ((TopicBase) o).model.equals(model);
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
    // ### See de.deepamehta.core.storage.impl.HGTopic and de.deepamehta.core.service.impl.AttachedTopic.
    public TopicModel getModel() {
        return model;
    }

    protected final void setModel(TopicModel model) {
        this.model = model;
    }
}
