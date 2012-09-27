package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;



/**
 * Collection of the data that makes up an {@link AssociationType}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class AssociationTypeModel extends TypeModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationTypeModel(String uri, String value, String dataTypeUri) {
        super(uri, "dm4.core.assoc_type", new SimpleValue(value), dataTypeUri);
    }

    public AssociationTypeModel(TopicModel model) {
        super(model);
    }

    public AssociationTypeModel(JSONObject assocTypeModel) {
        super(assocTypeModel, "dm4.core.assoc_type");
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String toString() {
        return "association type (" + super.toString() + ")";
    }
}
