package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;



public class TopicModel extends DeepaMehtaObjectModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicModel(String typeUri) {
        super(typeUri);
    }

    public TopicModel(String typeUri, SimpleValue value) {
        super(typeUri, value);
    }

    public TopicModel(String typeUri, CompositeValue composite) {
        super(typeUri, composite);
    }

    public TopicModel(String uri, String typeUri, SimpleValue value) {
        super(uri, typeUri, value);
    }

    public TopicModel(String uri, String typeUri, CompositeValue composite) {
        super(uri, typeUri, composite);
    }

    public TopicModel(long id, CompositeValue composite) {
        super(id, composite);
    }

    /**
     * @param   uri         If <code>null</code> an empty string is set. This is OK.
     * @param   typeUri     Mandatory. Note: only the internal meta type topic (ID 0) has no type URI (null).
     * @param   value       If <code>null</code> an empty string value is set. This is OK.
     * @param   composite   If <code>null</code> an empty composite is set. This is OK.
     */
    public TopicModel(long id, String uri, String typeUri, SimpleValue value, CompositeValue composite) {
        super(id, uri, typeUri, value, composite);
    }

    public TopicModel(TopicModel model) {
        super(model);
    }

    public TopicModel(JSONObject model) {
        super(model);
    }

    public TopicModel(JSONObject typeModel, String typeUri) {
        super(typeModel, typeUri);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // === Java API ===

    @Override
    public TopicModel clone() {
        try {
            return (TopicModel) super.clone();
        } catch (Exception e) {
            throw new RuntimeException("Cloning a TopicModel failed", e);
        }
    }

    @Override
    public String toString() {
        return "topic (" + super.toString() + ")";
    }
}
