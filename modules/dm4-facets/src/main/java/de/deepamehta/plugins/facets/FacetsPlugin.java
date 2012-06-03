package de.deepamehta.plugins.facets;

import de.deepamehta.plugins.facets.service.FacetsService;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Plugin;

import java.util.Set;
import java.util.logging.Logger;



public class FacetsPlugin extends Plugin implements FacetsService {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************************
    // *** FacetsService Implementation ***
    // ************************************



    @Override
    public Topic getFacet(Topic topic, String facetTypeUri) {
        // ### TODO: integrity check: is the topic an instance of that facet type?
        return fetchChildTopic(topic, getAssocDef(facetTypeUri), true);     // fetchComposite=true
    }

    @Override
    public Set<RelatedTopic> getFacets(Topic topic, String facetTypeUri) {
        // ### TODO: integrity check: is the topic an instance of that facet type?
        return fetchChildTopics(topic, getAssocDef(facetTypeUri), true);    // fetchComposite=true
    }

    // ---

    @Override
    public void addFacetTypeToTopic(long topicId, String facetTypeUri) {
        dms.createAssociation(new AssociationModel("dm4.core.instantiation", 
            new TopicRoleModel(topicId,      "dm4.core.instance"),
            new TopicRoleModel(facetTypeUri, "dm4.facets.facet")), null);   // clientState=null
    }

    @Override
    public void updateFacet(Topic topic, String facetTypeUri, TopicModel facet, ClientState clientState,
                                                                                Directives directives) {
        // ### TODO: incorporate the Facets module into the DeepaMehta core.
        // ### TODO: many cardinality
        topic.updateChildTopic(getAssocDef(facetTypeUri), facet, clientState, directives);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Fetches and returns a child topic or <code>null</code> if no such topic extists.
     * <p>
     * ### Note: There is a principal copy in AttachedDeepaMehtaObject but here the precondition is different:
     * The given AssociationDefinition is not necessarily part of the given topic's type.
     */
    private RelatedTopic fetchChildTopic(Topic topic, AssociationDefinition assocDef, boolean fetchComposite) {
        String assocTypeUri       = assocDef.getInstanceLevelAssocTypeUri();
        String myRoleTypeUri      = assocDef.getWholeRoleTypeUri();
        String othersRoleTypeUri  = assocDef.getPartRoleTypeUri();
        String othersTopicTypeUri = assocDef.getPartTopicTypeUri();
        //
        return topic.getRelatedTopic(assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri,
            fetchComposite, false, null);
    }

    /**
     * Fetches and returns child topics.
     * <p>
     * ### Note: There is a principal copy in AttachedDeepaMehtaObject but here the precondition is different:
     * The given AssociationDefinition is not necessarily part of the given topic's type.
     */
    private Set<RelatedTopic> fetchChildTopics(Topic topic, AssociationDefinition assocDef, boolean fetchComposite) {
        String assocTypeUri       = assocDef.getInstanceLevelAssocTypeUri();
        String myRoleTypeUri      = assocDef.getWholeRoleTypeUri();
        String othersRoleTypeUri  = assocDef.getPartRoleTypeUri();
        String othersTopicTypeUri = assocDef.getPartTopicTypeUri();
        //
        return topic.getRelatedTopics(assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri,
            fetchComposite, false, 0, null).getItems();
    }

    // ---

    private AssociationDefinition getAssocDef(String facetTypeUri) {
        // Note: a facet type has exactly *one* association definition
        return dms.getTopicType(facetTypeUri, null).getAssocDefs().values().iterator().next();
    }
}
