package systems.dmx.core.impl;

import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.facets.FacetValueModel;
import systems.dmx.core.service.ModelFactory;

import java.util.HashMap;
import java.util.List;



class FacetValueModelImpl extends ChildTopicsModelImpl implements FacetValueModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String compDefUri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    FacetValueModelImpl(String compDefUri, ModelFactory mf) {
        super(new HashMap(), mf);
        this.compDefUri = compDefUri;
    }

    FacetValueModelImpl(ChildTopicsModelImpl childTopics) {
        super(childTopics);
        this.compDefUri = iterator().next();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Single-valued Facets ===

    public RelatedTopicModel getTopic() {
        return getTopic(compDefUri);
    }

    public List<? extends RelatedTopicModel> getTopics() {
        return getTopics(compDefUri);
    }

    // ---

    public FacetValueModel set(RelatedTopicModel value) {
        return (FacetValueModel) set(compDefUri, value);
    }

    public FacetValueModel set(TopicModel value) {
        return (FacetValueModel) set(compDefUri, value);
    }

    public FacetValueModel set(Object value) {
        return (FacetValueModel) set(compDefUri, value);
    }

    public FacetValueModel set(ChildTopicsModel value) {
        return (FacetValueModel) set(compDefUri, value);
    }

    // ---

    public FacetValueModel setRef(long refTopicId) {
        return (FacetValueModel) setRef(compDefUri, refTopicId);
    }

    public FacetValueModel setRef(String refTopicUri) {
        return (FacetValueModel) setRef(compDefUri, refTopicUri);
    }

    // ---

    public FacetValueModel setDeletionRef(long refTopicId) {
        return (FacetValueModel) setDeletionRef(compDefUri, refTopicId);
    }

    public FacetValueModel setDeletionRef(String refTopicUri) {
        return (FacetValueModel) setDeletionRef(compDefUri, refTopicUri);
    }



    // === Multiple-valued Facets ===

    public FacetValueModel set(List<RelatedTopicModel> values) {
        return (FacetValueModel) set(compDefUri, values);
    }

    // ---

    public FacetValueModel addRef(long refTopicId) {
        return (FacetValueModel) addRef(compDefUri, refTopicId);
    }

    public FacetValueModel addRef(String refTopicUri) {
        return (FacetValueModel) addRef(compDefUri, refTopicUri);
    }

    // ---

    public FacetValueModel addDeletionRef(long refTopicId) {
        return (FacetValueModel) addDeletionRef(compDefUri, refTopicId);
    }

    public FacetValueModel addDeletionRef(String refTopicUri) {
        return (FacetValueModel) addDeletionRef(compDefUri, refTopicUri);
    }
}
