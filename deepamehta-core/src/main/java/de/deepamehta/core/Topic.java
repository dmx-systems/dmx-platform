package de.deepamehta.core;

import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ChangeReport;
import de.deepamehta.core.service.ClientContext;
import de.deepamehta.core.service.Directives;

import java.util.Set;



/**
 * Specification of a topic -- DeepaMehta's central data object.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface Topic extends DeepaMehtaObject {

    TopicModel getModel();

    ChangeReport update(TopicModel model, ClientContext clientContext, Directives directives);

    // === Traversal ===

    // --- Association Retrieval ---

    // ### TODO: move this methods to DeepaMehtaObject.

    RelatedAssociation getRelatedAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                     String othersAssocTypeUri, boolean fetchComposite, boolean fetchRelatingComposite);

    Set<RelatedAssociation> getRelatedAssociations(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                     String othersAssocTypeUri, boolean fetchComposite, boolean fetchRelatingComposite);
}
