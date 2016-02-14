package de.deepamehta.core.model.facets;

import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.RelatedTopicModel;

import java.util.List;



/**
 * A facet value as used in update facet calls.
 * Used for both, single-valued facets and multiple-valued facets.
 */
public interface FacetValueModel extends ChildTopicsModel {



    // === Single-valued Facets ===

    /**
     * Accesses a single-valued facet.
     */
    RelatedTopicModel getTopic();

    /**
     * Accesses a multiple-valued facet.
     */
    List<RelatedTopicModel> getTopics();

    // ---

    /**
     * Puts a value in a single-valued facet.
     */
    FacetValueModel put(RelatedTopicModel value);

    // ---

    /**
     * Convenience method to put a *simple* value in a single-valued facet.
     */
    FacetValueModel put(Object value);

    /**
     * Convenience method to put a *composite* value in a single-valued facet.
     */
    FacetValueModel put(ChildTopicsModel value);

    // ---

    /**
     * Puts a by-ID topic reference in a single-valued facet.
     */
    FacetValueModel putRef(long refTopicId);

    /**
     * Puts a by-URI topic reference in a single-valued facet.
     */
    FacetValueModel putRef(String refTopicUri);



    // === Multiple-valued Facets ===

    /**
     * Sets the values of a multiple-valued facet.
     */
    FacetValueModel put(List<RelatedTopicModel> values);

    // ---

    /**
     * Adds a by-ID topic reference to a multiple-valued facet.
     */
    FacetValueModel addRef(long refTopicId);

    /**
     * Adds a by-URI topic reference to a multiple-valued facet.
     */
    FacetValueModel addRef(String refTopicUri);

    // ---

    /**
     * Adds a by-ID topic deletion reference to a multiple-valued facet.
     */
    FacetValueModel addDeletionRef(long refTopicId);
}
