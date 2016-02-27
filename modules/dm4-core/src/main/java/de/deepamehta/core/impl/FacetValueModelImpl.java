package de.deepamehta.core.impl;

import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.facets.FacetValueModel;
import de.deepamehta.core.service.ModelFactory;

import java.util.HashMap;
import java.util.List;



class FacetValueModelImpl extends ChildTopicsModelImpl implements FacetValueModel {

    private String childTypeUri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    FacetValueModelImpl(String childTypeUri, PersistenceLayer pl) {
        super(new HashMap(), pl);
        this.childTypeUri = childTypeUri;
    }

    FacetValueModelImpl(ChildTopicsModelImpl childTopics) {
        super(childTopics);
        this.childTypeUri = iterator().next();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // === Single-valued Facets ===

    /**
     * Accesses a single-valued facet.
     */
    public RelatedTopicModel getTopic() {
        return getTopic(childTypeUri);
    }

    /**
     * Accesses a multiple-valued facet.
     */
    public List<RelatedTopicModel> getTopics() {
        return getTopics(childTypeUri);
    }

    // ---

    /**
     * Puts a value in a single-valued facet.
     */
    public FacetValueModel put(RelatedTopicModel value) {
        return (FacetValueModel) put(childTypeUri, value);
    }

    // ---

    /**
     * Convenience method to put a *simple* value in a single-valued facet.
     */
    public FacetValueModel put(Object value) {
        return (FacetValueModel) put(childTypeUri, value);
    }

    /**
     * Convenience method to put a *composite* value in a single-valued facet.
     */
    public FacetValueModel put(ChildTopicsModel value) {
        return (FacetValueModel) put(childTypeUri, value);
    }

    // ---

    /**
     * Puts a by-ID topic reference in a single-valued facet.
     */
    public FacetValueModel putRef(long refTopicId) {
        return (FacetValueModel) putRef(childTypeUri, refTopicId);
    }

    /**
     * Puts a by-URI topic reference in a single-valued facet.
     */
    public FacetValueModel putRef(String refTopicUri) {
        return (FacetValueModel) putRef(childTypeUri, refTopicUri);
    }

    // === Multiple-valued Facets ===

    /**
     * Sets the values of a multiple-valued facet.
     */
    public FacetValueModel put(List<RelatedTopicModel> values) {
        return (FacetValueModel) put(childTypeUri, values);
    }

    // ---

    /**
     * Adds a by-ID topic reference to a multiple-valued facet.
     */
    public FacetValueModel addRef(long refTopicId) {
        return (FacetValueModel) addRef(childTypeUri, refTopicId);
    }

    /**
     * Adds a by-URI topic reference to a multiple-valued facet.
     */
    public FacetValueModel addRef(String refTopicUri) {
        return (FacetValueModel) addRef(childTypeUri, refTopicUri);
    }

    // ---

    /**
     * Adds a by-ID topic deletion reference to a multiple-valued facet.
     */
    public FacetValueModel addDeletionRef(long refTopicId) {
        return (FacetValueModel) addDeletionRef(childTypeUri, refTopicId);
    }
}
