package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



/**
 * A topic type. Part of the meta-model (like a class).
 * <p>
 * A topic type is an ordered collection of {@link DataField}s.
 * A topic type itself is a {@link Topic}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class TopicType extends Topic {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected List<DataField> dataFields;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicType(Properties properties, List dataFields) {
        // id and label remain uninitialized
        super(-1, "de/deepamehta/core/topictype/TopicType", null, properties);  // label=null
        this.dataFields = dataFields;
    }

    public TopicType(JSONObject type) {
        // id, label, and properties remain uninitialized
        super(-1, "de/deepamehta/core/topictype/TopicType", null, null);
        try {
            // initialize properties
            setProperty("de/deepamehta/core/property/TypeURI", type.getString("uri"));
            setProperty("de/deepamehta/core/property/TypeLabel", type.getString("label"));
            if (type.has("icon_src")) {
                setProperty("icon_src", type.getString("icon_src"));
            }
            if (type.has("topic_label_field_uri")) {
                setProperty("topic_label_field_uri", type.getString("topic_label_field_uri"));
            }
            setProperty("js_renderer_class", type.getString("js_renderer_class"));
            // initialize data fields
            dataFields = new ArrayList();
            JSONArray fieldDefs = type.getJSONArray("fields");
            for (int i = 0; i < fieldDefs.length(); i++) {
                addDataField(new DataField(fieldDefs.getJSONObject(i)));
            }
        } catch (Throwable e) {
            e.printStackTrace();    // FIXME: to be dropped
            throw new RuntimeException("Error while parsing topic type \"" +
                getProperty("de/deepamehta/core/property/TypeURI") + "\"", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);                // "derived" from Topic
            o.put("type_uri", typeUri);     // "derived" from Topic
            o.put("label", getProperty("de/deepamehta/core/property/TypeLabel"));
            o.put("uri",   getProperty("de/deepamehta/core/property/TypeURI"));
            o.put("icon_src", getProperty("icon_src", null));                                //  optional
            o.put("topic_label_field_uri", getProperty("topic_label_field_uri", null));      //  optional
            o.put("js_renderer_class", getProperty("js_renderer_class"));
            //
            JSONArray fields = new JSONArray();
            for (DataField dataField : dataFields) {
                fields.put(dataField.toJSON());
            }
            o.put("fields", fields);
            //
            serializeEnrichment(o);
            //
            return o;
        } catch (JSONException e) {
            throw new RuntimeException("Error while serializing " + this, e);
        }
    }

    @Override
    public String toString() {
        return "topic type " + getProperty("de/deepamehta/core/property/TypeURI") + " (ID " + id +  ")";
    }

    // ---

    public void setTypeUri(String typeUri) {
        setProperty("de/deepamehta/core/property/TypeURI", typeUri);
    }

    public void setLabel(String label) {
        setProperty("de/deepamehta/core/property/TypeLabel", label);
    }

    // ---

    public List<DataField> getDataFields() {
        return dataFields;
    }

    public DataField getDataField(int index) {
        return dataFields.get(index);
    }

    public DataField getDataField(String uri) {
        for (DataField dataField : dataFields) {
            if (dataField.getUri().equals(uri)) {
                return dataField;
            }
        }
        throw new RuntimeException("Topic type \"" + getProperty("de/deepamehta/core/property/TypeLabel") +
            "\" has no data field \"" + uri + "\"");
    }

    public boolean hasDataField(String uri) {
        for (DataField dataField : dataFields) {
            if (dataField.getUri().equals(uri)) {
                return true;
            }
        }
        return false;
    }

    // ---

    public void addDataField(DataField dataField) {
        dataFields.add(dataField);
    }

    public void removeDataField(String uri) {
        try {
            boolean removed = dataFields.remove(getDataField(uri));
            if (!removed) {
                throw new RuntimeException("List.remove() returned false");
            }
        } catch (Throwable e) {
            throw new RuntimeException("Data field \"" + uri + "\" can't be removed", e);
        }
    }

    public void setDataFieldOrder(List<String> uris) {
        logger.info("### uris=" + uris);
        //
        if (uris.size() != dataFields.size()) {
            throw new RuntimeException("There are " + dataFields.size() + " data fields " +
                "to order but ordering description has " + uris.size());
        }
        //
        List reorderedDataFields = new ArrayList();
        for (String uri : uris) {
            reorderedDataFields.add(getDataField(uri));
        }
        //
        dataFields = reorderedDataFields;
    }
}
