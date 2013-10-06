package de.deepamehta.plugins.topicmaps.model;

import de.deepamehta.core.model.AssociationModel;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;



/**
 * An association viewmodel as contained in a topicmap viewmodel.
 *
 * ### TODO: could be renamed to "AssociationViewmodel"
 */
public class TopicmapAssociation extends AssociationModel {

    // ---------------------------------------------------------------------------------------------------- Constructors

    TopicmapAssociation(AssociationModel assoc) {
        super(assoc);
    }
}
