package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;



public class TopicModel extends DeepaMehtaObjectModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicModel(String typeUri) {
        super(typeUri);
    }

    public TopicModel(String typeUri, Composite composite) {
        super(typeUri, composite);
    }

    public TopicModel(String uri, TopicValue value, String typeUri) {
        super(uri, value, typeUri);
    }

    public TopicModel(String uri, TopicValue value, String typeUri, Composite composite) {
        super(uri, value, typeUri, composite);
    }

    /**
     * @param   uri         If <code>null</code> an empty string is set. This is OK.
     * @param   value       If <code>null</code> an empty string value is set. This is OK.
     * @param   typeUri     Mandatory. Note: only the internal meta type topic (ID 0) has no type URI (null).
     * @param   composite   If <code>null</code> an empty composite is set. This is OK.
     */
    public TopicModel(long id, String uri, TopicValue value, String typeUri, Composite composite) {
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

    // ---

    // Called from the TypeModel's JSON constructor
    protected TopicModel() {
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // === Java API ===

    @Override
    public String toString() {
        return "topic model (id=" + id + ", uri=\"" + uri + "\", value=" + value +
            ", typeUri=\"" + typeUri + "\", composite=" + composite + ")";
    }
}
