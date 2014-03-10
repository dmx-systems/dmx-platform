package de.deepamehta.plugins.facets.model;

import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.TopicModel;

import org.codehaus.jettison.json.JSONObject;

import java.util.List;



public class FacetValues extends CompositeValueModel {

    private String childTypeUri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public FacetValues(String childTypeUri) {
        this.childTypeUri = childTypeUri;
    }

    public FacetValues(JSONObject obj) {
        super(obj);
        try {
            if (size() != 1) {
                throw new RuntimeException("There are " + size() + " child type entries (expected is 1)");
            }
            //
            this.childTypeUri = keys().iterator().next();
        } catch (Exception e) {
            throw new RuntimeException("Parsing FacetValues failed (JSONObject=" + obj + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public List<TopicModel> getValues() {
        return getTopics(childTypeUri);
    }

    // ---

    public FacetValues addRef(long refTopicId) {
        return (FacetValues) addRef(childTypeUri, refTopicId);
    }

    public FacetValues addRef(String refTopicUri) {
        return (FacetValues) addRef(childTypeUri, refTopicUri);
    }
}
