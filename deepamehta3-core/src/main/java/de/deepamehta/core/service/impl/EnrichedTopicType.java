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

    private Map<String, Map<String, Object>> enrichment;    // key: plugin ID

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    EnrichedTopicType(AttachedTopicType topicType) {
        super(topicType);
        this.enrichment = new HashMap();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    void setEnrichment(String pluginId, Map<String, Object> values) {
        if (values.size() > 0) {
            enrichment.put(pluginId, values);
        }
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            // enrichment
            if (enrichment.size() > 0) {
                JSONObject e = new JSONObject();
                for (String pluginId : enrichment.keySet()) {
                    Map<String, Object> values = enrichment.get(pluginId);
                    e.put(pluginId, new JSONObject(values));
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
