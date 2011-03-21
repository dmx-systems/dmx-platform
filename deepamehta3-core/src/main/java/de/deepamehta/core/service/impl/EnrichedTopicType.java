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

    void addEnrichment(Map<String, Object> values) {
        for (String key : values.keySet()) {
            Object value = values.get(key);
            Object previousValue = enrichment.put(key, value);
            if (previousValue != null) {
                throw new RuntimeException("Key clash in enrichment for topic type \"" + getUri() + "\" (key=\"" +
                    key + "\", value=\"" + value + "\", previousValue=\"" + previousValue + "\")");
            }
        }
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            // enrichment
            for (String key : enrichment.keySet()) {
                Object value = enrichment.get(key);
                Object previousValue = o.opt(key);
                if (previousValue != null) {
                    throw new RuntimeException("Key clash in enrichment for topic type \"" + getUri() + "\" (key=\"" +
                        key + "\", value=\"" + value + "\", previousValue=\"" + previousValue + "\")");
                }
                o.put(key, value);
            }
            //
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }
}
