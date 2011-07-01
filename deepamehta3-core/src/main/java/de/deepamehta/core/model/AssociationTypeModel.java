package de.deepamehta.core.model;

import de.deepamehta.core.Topic;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



/**
 * Collection of the data that makes up an {@link AssociationType}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class AssociationTypeModel extends TypeModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationTypeModel(String uri, String value, String dataTypeUri) {
        super(uri, new TopicValue(value), "dm3.core.assoc_type", dataTypeUri);
    }

    public AssociationTypeModel(TopicModel model, String dataTypeUri, ViewConfigurationModel viewConfigModel) {
        super(model, dataTypeUri, viewConfigModel);
    }

    public AssociationTypeModel(JSONObject assocTypeModel) {
        super(assocTypeModel);
        try {
            this.id = -1;
            this.uri = assocTypeModel.getString("uri");
            this.value = new TopicValue(assocTypeModel.get("value"));
            this.typeUri = "dm3.core.assoc_type";
            this.composite = new Composite();
        } catch (Exception e) {
            throw new RuntimeException("Parsing AssociationTypeModel failed (JSONObject=" + assocTypeModel + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            getViewConfigModel().toJSON(o);
            //
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    @Override
    public String toString() {
        return "association type model (id=" + id + ", uri=\"" + uri + "\", value=" + value +
            ", typeUri=\"" + typeUri + "\",\nassociation type " + getViewConfigModel() + ")";
    }
}
