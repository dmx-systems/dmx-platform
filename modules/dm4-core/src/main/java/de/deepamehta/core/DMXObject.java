package de.deepamehta.core;

import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.DMXObjectModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;

import java.util.List;



public interface DMXObject extends Identifiable, JSONEnabled {



    // === Model ===

    // --- ID ---

    long getId();

    // --- URI ---

    String getUri();

    void setUri(String uri);

    // --- Type URI ---

    String getTypeUri();

    void setTypeUri(String typeUri);

    // --- Simple Value ---

    SimpleValue getSimpleValue();

    void setSimpleValue(String value);
    void setSimpleValue(int value);
    void setSimpleValue(long value);
    void setSimpleValue(boolean value);
    void setSimpleValue(SimpleValue value);

    // --- Child Topics ---

    ChildTopics getChildTopics();

    void setChildTopics(ChildTopicsModel childTopics);

    // ---

    DMXObject loadChildTopics();
    DMXObject loadChildTopics(String assocDefUri);

    // ---

    /**
     * Returns the type of this object.
     * <p>
     * No access control is performed as <i>Implicit READ permission</i> applies: if a user has READ access to an object
     * she has READ access to its type as well.
     * <p>
     * Note: if the user would have no READ access to this object the DMX Core would not instantiate it in the
     * first place, but throw an <code>AccessControlException</code>.
     */
    DMXType getType();

    DMXObjectModel getModel();



    // === Updating ===

    void updateChildTopics(ChildTopicsModel updateModel, AssociationDefinition assocDef);

    // ### TODO: drop it!
    void updateChildTopic(RelatedTopicModel newChildTopic, AssociationDefinition assocDef);

    // ### TODO: drop it!
    void updateChildTopics(List<? extends RelatedTopicModel> newChildTopics, AssociationDefinition assocDef);



    // === Deletion ===

    /**
     * Deletes the DMX object in its entirety, that is
     * - the object itself (the <i>parent</i>)
     * - all child topics associated via "dm4.core.composition", recusively
     * - all the remaining direct associations, e.g. "dm4.core.instantiation"
     */
    void delete();



    // === Traversal ===

    // --- Topic Retrieval ---

    /**
     * Fetches and returns a related topic or <code>null</code> if no such topic extists.
     *
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    RelatedTopic getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                 String othersTopicTypeUri);

    List<RelatedTopic> getRelatedTopics(String assocTypeUri);

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    List<RelatedTopic> getRelatedTopics(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                        String othersTopicTypeUri);

    /**
     * @param   assocTypeUris       may *not* be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    List<RelatedTopic> getRelatedTopics(List assocTypeUris, String myRoleTypeUri, String othersRoleTypeUri,
                                        String othersTopicTypeUri);

    // --- Association Retrieval ---

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersAssocTypeUri  may be null
     */
    RelatedAssociation getRelatedAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                             String othersAssocTypeUri);

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersAssocTypeUri  may be null
     */
    List<RelatedAssociation> getRelatedAssociations(String assocTypeUri, String myRoleTypeUri,
                                                    String othersRoleTypeUri, String othersAssocTypeUri);

    // ---

    Association getAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri, long othersTopicId);

    /**
     * Fetches all associations this object is a player in.
     */
    List<Association> getAssociations();



    // === Properties ===

    /**
     * Returns this object's property value associated with the given property URI.
     * If there's no property value associated with the property URI an exception is thrown.
     */
    Object getProperty(String propUri);

    /**
     * Checks whether for this object a property value is associated with a given property URI.
     */
    boolean hasProperty(String propUri);

    void setProperty(String propUri, Object propValue, boolean addToIndex);

    /**
     * Removes this object's property associated with the given property URI.
     * If there's no property value associated with the property URI nothing is performed.
     */
    void removeProperty(String propUri);



    // === Misc ===

    Object getDatabaseVendorObject();
}
