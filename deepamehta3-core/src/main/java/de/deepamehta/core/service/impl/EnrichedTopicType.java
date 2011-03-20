package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicTypeData;
import de.deepamehta.core.model.TopicValue;

import org.codehaus.jettison.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



class EnrichedTopicType extends AttachedTopicType {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Map<String, Object> enrichment;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    EnrichedTopicType(AttachedTopicType topicType) {
        super(topicType);
        this.enrichment = new HashMap();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    void setEnrichment(String key, Object value) {
        enrichment.put(key, value);
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            // enrichment
            if (enrichment.size() > 0) {
                JSONObject e = new JSONObject();
                for (String key : enrichment.keySet()) {
                    Object value = enrichment.get(key);
                    e.put(key, new JSONObject((Map) value));
                }
                o.put("enrichment", e);
            }
            //
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serializing " + this + " failed", e);
        }
    }
}
