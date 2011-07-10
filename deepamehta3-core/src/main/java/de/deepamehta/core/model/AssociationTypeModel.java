package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;

import java.util.Set;
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

    public AssociationTypeModel(TopicModel model) {
        super(model);
    }

    public AssociationTypeModel(JSONObject assocTypeModel) {
        super(assocTypeModel, "dm3.core.assoc_type");
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String toString() {
        return "association type (" + super.toString() + ")";
    }
}
