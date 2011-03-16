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
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class TopicTypeDefinition extends TopicType {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private List<AssociationDefinition> assocDefs;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicTypeDefinition(TopicType topicType) {
        super(topicType);
        this.assocDefs = new ArrayList();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // FIXME: abstraction. Adding should be the factory's resposibility
    public void addAssociationDefinition(AssociationDefinition assocDef) {
        assocDefs.add(assocDef);
    }

    // ---

    @Override
    public String toString() {
        return "topic type definition " + id + " \"" + value + "\" (uri=\"" + uri + "\", typeUri=\"" + typeUri +
            "\", dataTypeUri=\"" + dataTypeUri + "\", assocDefs=" + assocDefs + ")";
    }
}
