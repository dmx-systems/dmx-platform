package de.deepamehta.core;

import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ChangeReport;
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

    void setCompositeValue(CompositeValue comp, ClientState clientState, Directives directives);

    // ---

    void updateChildTopic(AssociationDefinition assocDef, TopicModel newChildTopic, ClientState clientState,
                                                                                    Directives directives);
    void updateChildTopics(AssociationDefinition assocDef, List<TopicModel> newChildTopics, ClientState clientState,
                                                                                            Directives directives);



    // === Traversal ===

    /**
     * Returns a child topic's value or <code>null</code> if the child topic doesn't exist.
     * ### FIXME: to be dropped?
     */
    SimpleValue getChildTopicValue(String assocDefUri);

    // ### FIXME: to be dropped?
    void setChildTopicValue(String assocDefUri, SimpleValue value);

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
                                 String othersTopicTypeUri, boolean fetchComposite, boolean fetchRelatingComposite,
                                 ClientState clientState);

    ResultSet<RelatedTopic> getRelatedTopics(String assocTypeUri, int maxResultSize, ClientState clientState);

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
                                    int maxResultSize, ClientState clientState);

    /**
     * @param   assocTypeUris       may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    ResultSet<RelatedTopic> getRelatedTopics(List<String> assocTypeUris, String myRoleTypeUri, String othersRoleTypeUri,
                                    String othersTopicTypeUri, boolean fetchComposite, boolean fetchRelatingComposite,
                                    int maxResultSize, ClientState clientState);

     // --- Association Retrieval ---

     Association getAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                             long othersTopicId);

     Set<Association> getAssociations();

     Set<Association> getAssociations(String myRoleTypeUri);



    // === Updating ===

    ChangeReport update(DeepaMehtaObjectModel model, ClientState clientState, Directives directives);



    // === Deletion ===

    /**
     * Deletes the DeepaMehta object in its entirety, that is
     * - the object itself (the <i>whole</i>)
     * - all sub-topics associated via "dm4.core.composition" (the <i>parts</i>), recusively
     * - all the remaining direct associations, e.g. "dm4.core.instantiation"
     */
    void delete(Directives directives);
}
