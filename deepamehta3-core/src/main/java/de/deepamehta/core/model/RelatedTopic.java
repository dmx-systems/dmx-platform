package de.deepamehta.core.model;



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
}
