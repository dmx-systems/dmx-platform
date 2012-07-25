package de.deepamehta.core;

import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ChangeReport;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;

import java.util.Set;



/**
 * Specification of a topic -- DeepaMehta's central data object.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface Topic extends DeepaMehtaObject {

    TopicModel getModel();

    ChangeReport update(TopicModel model, ClientState clientState, Directives directives);

    // === Traversal ===

    // --- Association Retrieval ---

    // ### TODO: move this methods to DeepaMehtaObject.

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersAssocTypeUri  may be null
     */
    RelatedAssociation getRelatedAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                     String othersAssocTypeUri, boolean fetchComposite, boolean fetchRelatingComposite);

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersAssocTypeUri  may be null
     */
    Set<RelatedAssociation> getRelatedAssociations(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                     String othersAssocTypeUri, boolean fetchComposite, boolean fetchRelatingComposite);
}
