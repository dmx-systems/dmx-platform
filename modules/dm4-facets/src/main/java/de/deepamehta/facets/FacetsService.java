package de.deepamehta.facets;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.facets.FacetValueModel;

import java.util.List;



public interface FacetsService {

    // ### TODO: rename to getFacetValue?
    RelatedTopic getFacet(long topicId, String facetTypeUri);

    /**
     * Retrieves a facet value.
     * ### TODO: rename to getFacetValue?
     *
     * @param   object          The facetted object: a topic, association, a type ...
     * @param   facetTypeUri    URI of the facet type.
     *
     * @return  The retrieved facet value (including its child topics) or <code>null</code> if no such topic extists.
     */
    RelatedTopic getFacet(DeepaMehtaObject object, String facetTypeUri);

    // ---

    // ### TODO: rename to getFacetValues?
    List<RelatedTopic> getFacets(long topicId, String facetTypeUri);

    /**
     * Retrieves the values of a multi-facet.
     * ### TODO: rename to getFacetValues?
     *
     * @param   object          The facetted object: a topic, association, a type ...
     * @param   facetTypeUri    URI of the facet type.
     *
     * @return  The retrieved facet values (including their child topics). The list may be empty.
     */
    List<RelatedTopic> getFacets(DeepaMehtaObject object, String facetTypeUri);

    // ---

    Topic getFacettedTopic(long topicId, List<String> facetTypeUris);

    void addFacetTypeToTopic(long topicId, String facetTypeUri);

    // ---

    void updateFacet(long topicId, String facetTypeUri, FacetValueModel value);

    /**
     * Updates a facet.
     *
     * @param   object          The facetted object: a topic, association, a type ...
     * @param   facetTypeUri    URI of the facet type.
     * @param   value           The new facet value.
     */
    void updateFacet(DeepaMehtaObject object, String facetTypeUri, FacetValueModel value);

    // ---

    boolean hasFacet(long topicId, String facetTypeUri, long facetTopicId);
}
