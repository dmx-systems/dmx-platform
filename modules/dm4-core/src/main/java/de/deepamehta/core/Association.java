package de.deepamehta.core;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.service.ChangeReport;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;

import java.util.Set;



/**
 * Specification of an association -- A n-ary connection between topics and other associations.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface Association extends DeepaMehtaObject {

    Role getRole1();

    Role getRole2();

    // ---

    Role getRole(long objectId);

    // ---

    AssociationModel getModel();

    ChangeReport update(AssociationModel model, ClientState clientState, Directives directives);

    // === Traversal ===

    Topic getTopic(String roleTypeUri);

    Set<Topic> getTopics(String roleTypeUri);

    // ---

    RelatedAssociation getRelatedAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri);
}
