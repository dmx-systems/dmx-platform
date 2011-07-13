package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;



public class TopicModel extends DeepaMehtaObjectModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicModel(String typeUri) {
        super(typeUri);
    }

    public TopicModel(String typeUri, CompositeValue composite) {
        super(typeUri, composite);
    }

    public TopicModel(String uri, SimpleValue value, String typeUri) {
        super(uri, value, typeUri);
    }

    public TopicModel(String uri, SimpleValue value, String typeUri, CompositeValue composite) {
        super(uri, value, typeUri, composite);
    }

    /**
     * @param   uri         If <code>null</code> an empty string is set. This is OK.
     * @param   value       If <code>null</code> an empty string value is set. This is OK.
     * @param   typeUri     Mandatory. Note: only the internal meta type topic (ID 0) has no type URI (null).
     * @param   composite   If <code>null</code> an empty composite is set. This is OK.
     */
    public TopicModel(long id, String uri, SimpleValue value, String typeUri, CompositeValue composite) {
        super(id, uri, value, typeUri, composite);
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
    public String toString() {
        return "topic (" + super.toString() + ")";
    }
}
