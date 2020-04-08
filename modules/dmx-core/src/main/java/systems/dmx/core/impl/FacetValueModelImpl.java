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

    private String childTypeUri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    FacetValueModelImpl(String childTypeUri, ModelFactory mf) {
        super(new HashMap(), mf);
        this.childTypeUri = childTypeUri;
    }

    FacetValueModelImpl(ChildTopicsModelImpl childTopics) {
        super(childTopics);
        this.childTypeUri = iterator().next();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Single-valued Facets ===

    public RelatedTopicModel getTopic() {
        return getTopic(childTypeUri);
    }

    public List<? extends RelatedTopicModel> getTopics() {
        return getTopics(childTypeUri);
    }

    // ---

    public FacetValueModel set(RelatedTopicModel value) {
        return (FacetValueModel) set(childTypeUri, value);
    }

    public FacetValueModel set(TopicModel value) {
        return (FacetValueModel) set(childTypeUri, value);
    }

    public FacetValueModel set(Object value) {
        return (FacetValueModel) set(childTypeUri, value);
    }

    public FacetValueModel set(ChildTopicsModel value) {
        return (FacetValueModel) set(childTypeUri, value);
    }

    // ---

    public FacetValueModel setRef(long refTopicId) {
        return (FacetValueModel) setRef(childTypeUri, refTopicId);
    }

    public FacetValueModel setRef(String refTopicUri) {
        return (FacetValueModel) setRef(childTypeUri, refTopicUri);
    }

    // ---

    public FacetValueModel setDeletionRef(long refTopicId) {
        return (FacetValueModel) setDeletionRef(childTypeUri, refTopicId);
    }

    public FacetValueModel setDeletionRef(String refTopicUri) {
        return (FacetValueModel) setDeletionRef(childTypeUri, refTopicUri);
    }



    // === Multiple-valued Facets ===

    public FacetValueModel set(List<RelatedTopicModel> values) {
        return (FacetValueModel) set(childTypeUri, values);
    }

    // ---

    public FacetValueModel addRef(long refTopicId) {
        return (FacetValueModel) addRef(childTypeUri, refTopicId);
    }

    public FacetValueModel addRef(String refTopicUri) {
        return (FacetValueModel) addRef(childTypeUri, refTopicUri);
    }

    // ---

    public FacetValueModel addDeletionRef(long refTopicId) {
        return (FacetValueModel) addDeletionRef(childTypeUri, refTopicId);
    }

    public FacetValueModel addDeletionRef(String refTopicUri) {
        return (FacetValueModel) addDeletionRef(childTypeUri, refTopicUri);
    }
}
