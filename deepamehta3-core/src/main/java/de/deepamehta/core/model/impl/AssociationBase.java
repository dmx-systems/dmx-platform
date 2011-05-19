package de.deepamehta.core.model.impl;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.RelatedTopic;
import de.deepamehta.core.model.Role;
import de.deepamehta.core.model.Topic;

import org.codehaus.jettison.json.JSONObject;

import java.util.Set;



public class AssociationBase implements Association {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private AssociationModel model;

    // ---------------------------------------------------------------------------------------------------- Constructors

    protected AssociationBase(AssociationModel model) {
        this.model = model;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Association Implementation ===

    @Override
    public long getId() {
        return model.getId();
    }

    @Override
    public String getTypeUri() {
        return model.getTypeUri();
    }

    // ---

    @Override
    public Role getRole1() {
        return model.getRole1();
    }

    @Override
    public Role getRole2() {
        return model.getRole2();
    }

    // ---

    @Override
    public Topic getTopic(String roleTypeUri) {
        throw new RuntimeException("Association is not attached to the core service ("
            + getClass() + ", " + this + ")");
    }

    @Override
    public Set<Topic> getTopics(String roleTypeUri) {
        throw new RuntimeException("Association is not attached to the core service ("
            + getClass() + ", " + this + ")");
    }

    // === Traversal ===

    @Override
    public Topic getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                            String othersTopicTypeUri,
                                                                            boolean fetchComposite) {
        throw new RuntimeException("Association is not attached to the core service ("
            + getClass() + ", " + this + ")");
    }

    @Override
    public Set<RelatedTopic> getRelatedTopics(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                         String othersTopicTypeUri,
                                                                                         boolean fetchComposite) {
        throw new RuntimeException("Association is not attached to the core service ("
            + getClass() + ", " + this + ")");
    }

    // ---

    @Override
    public JSONObject toJSON() {
        return model.toJSON();
    }



    // === Java API ===

    @Override
    public boolean equals(Object o) {
        return ((AssociationBase) o).model.equals(model);
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
    // ### It is called from the subclass constructor, but on a differnt AssociationBase instance.
    // ### See de.deepamehta.core.service.impl.AttachedAssociation.
    public AssociationModel getModel() {
        return model;
    }
}
