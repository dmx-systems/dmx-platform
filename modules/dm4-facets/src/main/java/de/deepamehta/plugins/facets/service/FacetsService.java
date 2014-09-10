package de.deepamehta.plugins.facets.service;

import de.deepamehta.plugins.facets.model.FacetValue;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.ResultList;

import java.util.List;



public interface FacetsService extends PluginService {

    // ### TODO: rename to getFacetValue
    Topic getFacet(long topicId, String facetTypeUri);

    /**
     * Retrieves a facet value.
     * ### TODO: rename to getFacetValue
     *
     * @param   object          The facetted object: a topic, association, a type ...
     * @param   facetTypeUri    URI of the facet type.
     *
     * @return  The retrieved facet value or <code>null</code> if no such topic extists.
     */
    Topic getFacet(DeepaMehtaObject object, String facetTypeUri);

    // ---

    // ### TODO: rename to getFacetValues
    ResultList<RelatedTopic> getFacets(long topicId, String facetTypeUri);

    /**
     * Retrieves the values of a multi-facet.
     * ### TODO: rename to getFacetValues
     *
     * @param   object          The facetted object: a topic, association, a type ...
     * @param   facetTypeUri    URI of the facet type.
     *
     * @return  The retrieved facet values. The list may be empty.
     */
    ResultList<RelatedTopic> getFacets(DeepaMehtaObject object, String facetTypeUri);

    // ---

    Topic getFacettedTopic(long topicId, List<String> facetTypeUris);

    void addFacetTypeToTopic(long topicId, String facetTypeUri);

    // ---

    void updateFacet(long topicId, String facetTypeUri, FacetValue value);

    /**
     * Updates a facet.
     *
     * @param   object          The facetted object: a topic, association, a type ...
     * @param   facetTypeUri    URI of the facet type.
     * @param   facetValue      The new facet value.
     */
    void updateFacet(DeepaMehtaObject object, String facetTypeUri, FacetValue value);

    // ---

    boolean hasFacet(long topicId, String facetTypeUri, long facetTopicId);
}
