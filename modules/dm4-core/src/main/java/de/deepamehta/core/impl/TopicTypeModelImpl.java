package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.model.ViewConfigurationModel;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.List;



/**
 * Data that underlies a {@link TopicType}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
class TopicTypeModelImpl extends TypeModelImpl implements TopicTypeModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicTypeModelImpl(String uri, String value, String dataTypeUri) {
        super(uri, "dm4.core.topic_type", new SimpleValue(value), dataTypeUri);
    }

    TopicTypeModelImpl(TopicModel topic, String dataTypeUri, List<IndexMode> indexModes,
                       List<AssociationDefinitionModel> assocDefs, List<String> labelConfig,
                       ViewConfigurationModel viewConfig) {
        super(topic, dataTypeUri, indexModes, assocDefs, labelConfig, viewConfig);
    }

    TopicTypeModelImpl(JSONObject topicType) throws JSONException {
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
