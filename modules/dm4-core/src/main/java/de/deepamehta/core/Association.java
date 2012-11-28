package de.deepamehta.core;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;

import java.util.Set;



/**
 * ### FIXDOC: Specification of an association -- A n-ary connection between topics and other associations.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface Association extends DeepaMehtaObject {



    // === Model ===

    Role getRole1();

    Role getRole2();

    // ---

    Role getRole(RoleModel model);

    // ---

    AssociationModel getModel();



    // === Updating ===

    void update(AssociationModel model, ClientState clientState, Directives directives);



    // === Traversal ===

    Topic getTopic(String roleTypeUri);

    Set<Topic> getTopics(String roleTypeUri);

    // ---

    RelatedAssociation getRelatedAssociation(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri);
}
