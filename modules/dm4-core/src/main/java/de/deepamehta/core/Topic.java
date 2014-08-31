package de.deepamehta.core;

import de.deepamehta.core.model.TopicModel;

import java.util.List;



/**
 * Specification of a topic -- DeepaMehta's central data object.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface Topic extends DeepaMehtaObject {



    // === Model ===

    TopicModel getModel();



    // === Updating ===

    void update(TopicModel model);



    // === Traversal ===

    // --- Association Retrieval ---

    // ### TODO: move to DeepaMehtaObject
    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersAssocTypeUri  may be null
     */
    RelatedAssociation getRelatedAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                             String othersAssocTypeUri);

    // ### TODO: move to DeepaMehtaObject
    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersAssocTypeUri  may be null
     */
    List<RelatedAssociation> getRelatedAssociations(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                    String othersAssocTypeUri);
}
