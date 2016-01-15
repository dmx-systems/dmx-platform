package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.List;



/**
 * Data that underlies a {@link TopicType}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class TopicTypeModel extends TypeModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicTypeModel(String uri, String value, String dataTypeUri) {
        super(uri, "dm4.core.topic_type", new SimpleValue(value), dataTypeUri);
    }

    public TopicTypeModel(TopicModel topic, String dataTypeUri, List<IndexMode> indexModes,
                          List<AssociationDefinitionModel> assocDefs, List<String> labelConfig,
                          ViewConfigurationModel viewConfig) {
        super(topic, dataTypeUri, indexModes, assocDefs, labelConfig, viewConfig);
    }

    public TopicTypeModel(JSONObject topicType) throws JSONException {
        super(topicType.put("type_uri", "dm4.core.topic_type"));
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public TopicTypeModel addAssocDef(AssociationDefinitionModel assocDef) {
        return (TopicTypeModel) super.addAssocDef(assocDef);
    }

    // ---

    @Override
    public String toString() {
        return "topic type (" + super.toString() + ")";
    }
}
