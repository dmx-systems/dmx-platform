package de.deepamehta.plugins.facets.service;

import de.deepamehta.plugins.facets.model.FacetValues;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.PluginService;

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
     * @return  The retrieved facet.
     */
    Topic getFacet(DeepaMehtaObject object, String facetTypeUri);

    // ---

    // ### TODO: rename to getFacetValues
    List<RelatedTopic> getFacets(long topicId, String facetTypeUri);

    /**
     * Retrieves the values of a multi-facet.
     * ### TODO: rename to getFacetValues
     *
     * @param   object          The facetted object: a topic, association, a type ...
     * @param   facetTypeUri    URI of the facet type.
     *
     * @return  The retrieved multi-facet.
     */
    List<RelatedTopic> getFacets(DeepaMehtaObject object, String facetTypeUri);

    // ---

    void addFacetTypeToTopic(long topicId, String facetTypeUri);

    // ---

    void updateFacet(long topicId, String facetTypeUri, TopicModel facetValue, ClientState clientState);

    /**
     * Updates a facet.
     *
     * @param   object          The facetted object: a topic, association, a type ...
     * @param   facetTypeUri    URI of the facet type.
     * @param   facetValue      The new facet value.
     * @param   directives      Must be not null at least for aggregated facets ### FIXME: directives are ignored
     */
    void updateFacet(DeepaMehtaObject object, String facetTypeUri, TopicModel facetValue, ClientState clientState,
                                                                                          Directives directives);

    // ---

    // ### TODO: rename to updateMultiFacet
    void updateFacets(long topicId, String facetTypeUri, FacetValues facetValues, ClientState clientState);

    /**
     * Updates a multi-facet.
     * ### TODO: rename to updateMultiFacet
     *
     * @param   object          The facetted object: a topic, association, a type ...
     * @param   facetTypeUri    URI of the facet type.
     * @param   facetValues     The new facet values.
     * @param   directives      Must be not null at least for aggregated facets ### FIXME: directives are ignored
     */
    void updateFacets(DeepaMehtaObject object, String facetTypeUri, FacetValues facetValues,
                                                                    ClientState clientState, Directives directives);

    // ---

    boolean hasFacet(long topicId, String facetTypeUri, long facetTopicId);
}
