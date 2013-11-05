package de.deepamehta.core;

import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;

import java.util.List;
import java.util.Set;



public interface DeepaMehtaObject extends Identifiable, JSONEnabled {



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

    // --- Composite Value ---

    CompositeValue getCompositeValue();

    void setCompositeValue(CompositeValueModel comp, ClientState clientState, Directives directives);

    // ---

    void loadChildTopics(String childTypeUri);

    // ---

    DeepaMehtaObjectModel getModel();



    // === Updating ===

    void update(DeepaMehtaObjectModel model, ClientState clientState, Directives directives);

    // ---

    void updateChildTopic(TopicModel newChildTopic, AssociationDefinition assocDef, ClientState clientState,
                                                                                    Directives directives);
    void updateChildTopics(List<TopicModel> newChildTopics, AssociationDefinition assocDef, ClientState clientState,
                                                                                            Directives directives);



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
                                 String othersTopicTypeUri, boolean fetchComposite, boolean fetchRelatingComposite);

    ResultSet<RelatedTopic> getRelatedTopics(String assocTypeUri, int maxResultSize);

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     * @param   fetchComposite
     * @param   fetchRelatingComposite
     * @param   maxResultSize       Result size limit. Pass 0 for no limit.
     */
    ResultSet<RelatedTopic> getRelatedTopics(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                    String othersTopicTypeUri, boolean fetchComposite, boolean fetchRelatingComposite,
                                    int maxResultSize);

    /**
     * @param   assocTypeUris       may *not* be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    ResultSet<RelatedTopic> getRelatedTopics(List assocTypeUris, String myRoleTypeUri, String othersRoleTypeUri,
                                    String othersTopicTypeUri, boolean fetchComposite, boolean fetchRelatingComposite,
                                    int maxResultSize);

     // --- Association Retrieval ---

     Association getAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                             long othersTopicId);

     Set<Association> getAssociations();



    // === Deletion ===

    /**
     * Deletes the DeepaMehta object in its entirety, that is
     * - the object itself (the <i>parent</i>)
     * - all child topics associated via "dm4.core.composition", recusively
     * - all the remaining direct associations, e.g. "dm4.core.instantiation"
     */
    void delete(Directives directives);



    // === Properties ===

    Object getProperty(String propUri);

    void setProperty(String propUri, Object propValue, boolean addToIndex);

    boolean hasProperty(String propUri);

    void removeProperty(String propUri);
}
