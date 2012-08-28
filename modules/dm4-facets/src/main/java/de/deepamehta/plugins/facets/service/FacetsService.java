package de.deepamehta.plugins.facets.service;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.PluginService;

import java.util.List;
import java.util.Set;



public interface FacetsService extends PluginService {

    Topic getFacet(long topicId, String facetTypeUri);

    /**
     * Retrieves a facet.
     *
     * @param   object          The facetted object: a topic, association, a type ...
     * @param   facetTypeUri    URI of the facet type.
     *
     * @return  The retrieved facet.
     */
    Topic getFacet(DeepaMehtaObject object, String facetTypeUri);

    // ---

    /**
     * Retrieves a multi-facet.
     *
     * @param   object          The facetted object: a topic, association, a type ...
     * @param   facetTypeUri    URI of the facet type.
     *
     * @return  The retrieved multi-facet.
     */
    Set<RelatedTopic> getFacets(DeepaMehtaObject object, String facetTypeUri);

    // ---

    void addFacetTypeToTopic(long topicId, String facetTypeUri);

    // ---

    /**
     * Updates a facet.
     *
     * @param   object          The facetted object: a topic, association, a type ...
     * @param   facetTypeUri    URI of the facet type.
     * @param   facetValue      The new facet value.
     */
    void updateFacet(DeepaMehtaObject object, String facetTypeUri, TopicModel facetValue,
                                                                   ClientState clientState, Directives directives);

    /**
     * Updates a multi-facet.
     *
     * @param   object          The facetted object: a topic, association, a type ...
     * @param   facetTypeUri    URI of the facet type.
     * @param   facetValues     The new facet values.
     */
    void updateFacets(DeepaMehtaObject object, String facetTypeUri, List<TopicModel> facetValues,
                                                                    ClientState clientState, Directives directives);
}
