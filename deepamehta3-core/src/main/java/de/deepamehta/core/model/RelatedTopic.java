package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONArray;

import java.util.List;


/**
 * A Topic-Association pair.
 * <p>
 * Acts as a data transfer object.
 */
public class RelatedTopic {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Topic topic;
    private Association association;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public RelatedTopic() {
    }

    public RelatedTopic(Topic topic, Association association) {
        this.topic = topic;
        this.association = association;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public Topic getTopic() {
        return topic;
    }

    public Association getRelation() {
        return association;
    }

    // ---

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public void setRelation(Association association) {
        this.association = association;
    }

    // ---

    public static JSONArray relatedTopicsToJson(List<RelatedTopic> relTopics) {
        JSONArray array = new JSONArray();
        for (RelatedTopic relTopic : relTopics) {
            // FIXME: for the moment it is sufficient to serialize the topics only.
            // The respective relations are omitted.
            array.put(relTopic.getTopic().toJSON());
        }
        return array;
    }
}
