package de.deepamehta.core.impl.model;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Role;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicRoleModel;

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



    // **********************************
    // *** Association Implementation ***
    // **********************************



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
        throw new RuntimeException("Association is not attached to the core service ("
            + getClass() + ", " + this + ")");
    }

    @Override
    public Role getRole2() {
        throw new RuntimeException("Association is not attached to the core service ("
            + getClass() + ", " + this + ")");
    }

    // ---

    @Override
    public void setTypeUri(String assocTypeUri) {
        model.setTypeUri(assocTypeUri);
    }



    // === Traversal ===

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

    // ---

    @Override
    public RelatedTopic getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
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
    // ### See de.deepamehta.core.impl.service.AttachedAssociation.
    public AssociationModel getModel() {
        return model;
    }
}
