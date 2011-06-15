package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;



public abstract class TypeModel extends TopicModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private ViewConfigurationModel viewConfigModel;         // is never null

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TypeModel(String uri, TopicValue value, String topicTypeUri) {
        super(uri, value, topicTypeUri);
        this.viewConfigModel = new ViewConfigurationModel();
    }

    public TypeModel(TopicModel model, ViewConfigurationModel viewConfigModel) {
        super(model);
        this.viewConfigModel = viewConfigModel;
    }

    public TypeModel(TypeModel model) {
        super(model);
        this.viewConfigModel = model.getViewConfigModel();
    }

    public TypeModel(JSONObject typeModel) {
        // Note: the super constructor is not called here.
        // The entire object is initialized by the subclass constructors.
        this.viewConfigModel = new ViewConfigurationModel(typeModel);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public ViewConfigurationModel getViewConfigModel() {
        return viewConfigModel;
    }

    // FIXME: server-side operations on the view config settings possibly suggest they are not acually
    // view config settings but part of the topic type model. Possibly this method should be dropped.
    public Object getViewConfig(String typeUri, String settingUri) {
        return viewConfigModel.getSetting(typeUri, settingUri);
    }
}
