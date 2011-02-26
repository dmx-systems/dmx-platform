package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONArray;

import java.util.List;


/**
 * A Topic-Relation pair.
 * <p>
 * Acts as a data transfer object.
 */
public class RelatedTopic {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Topic topic;
    private Relation relation;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public RelatedTopic() {
    }

    public RelatedTopic(Topic topic, Relation relation) {
        this.topic = topic;
        this.relation = relation;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public Topic getTopic() {
        return topic;
    }

    public Relation getRelation() {
        return relation;
    }

    // ---

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public void setRelation(Relation relation) {
        this.relation = relation;
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
