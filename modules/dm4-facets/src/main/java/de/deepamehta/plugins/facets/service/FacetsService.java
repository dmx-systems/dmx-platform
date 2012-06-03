package de.deepamehta.plugins.facets.service;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.PluginService;

import java.util.Set;



public interface FacetsService extends PluginService {

    /**
     * Retrieves a topic facet from the DB.
     *
     * @param   topic           The facetted topic.
     * @param   facetTypeUri    URI of the facet type.
     *
     * @return  The retrieved facet.
     */
    Topic getFacet(Topic topic, String facetTypeUri);

    /**
     * Retrieves a topic multi-facet from the DB.
     *
     * @param   topic           The facetted topic.
     * @param   facetTypeUri    URI of the facet type.
     *
     * @return  The retrieved multi-facet.
     */
    Set<RelatedTopic> getFacets(Topic topic, String facetTypeUri);

    // ---

    void addFacetTypeToTopic(long topicId, String facetTypeUri);

    /**
     * Updates a topic facet.
     *
     * @param   topic           The facetted topic.
     * @param   facetTypeUri    URI of the facet type.
     * @param   facet           The new facet value.
     */
    void updateFacet(Topic topic, String facetTypeUri, TopicModel facet, ClientState clientState,
                                                                         Directives directives);
}
