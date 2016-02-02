package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.ViewConfigurationModel;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.List;



/**
 * Data that underlies a {@link AssociationType}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class AssociationTypeModelImpl extends TypeModelImpl implements AssociationTypeModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    AssociationTypeModelImpl(String uri, String value, String dataTypeUri) {
        super(uri, "dm4.core.assoc_type", new SimpleValue(value), dataTypeUri);
    }

    AssociationTypeModelImpl(TopicModel topic, String dataTypeUri, List<IndexMode> indexModes,
                             List<AssociationDefinitionModel> assocDefs, List<String> labelConfig,
                             ViewConfigurationModel viewConfig) {
        super(topic, dataTypeUri, indexModes, assocDefs, labelConfig, viewConfig);
    }

    AssociationTypeModelImpl(JSONObject assocType) throws JSONException {
        super(assocType.put("type_uri", "dm4.core.assoc_type"));
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public AssociationTypeModel addAssocDef(AssociationDefinitionModel assocDef) {
        return (AssociationTypeModel) super.addAssocDef(assocDef);
    }

    // ---

    @Override
    public String toString() {
        return "association type (" + super.toString() + ")";
    }
}
