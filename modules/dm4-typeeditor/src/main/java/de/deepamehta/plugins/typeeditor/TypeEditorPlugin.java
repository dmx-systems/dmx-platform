package de.deepamehta.plugins.typeeditor;

import de.deepamehta.core.Association;
import de.deepamehta.core.Topic;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.event.PostUpdateAssociationListener;
import de.deepamehta.core.service.event.PreDeleteAssociationListener;

import java.util.logging.Logger;



public class TypeEditorPlugin extends PluginActivator implements PostUpdateAssociationListener,
                                                                 PreDeleteAssociationListener {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void postUpdateAssociation(Association assoc, AssociationModel oldModel) {
        if (isAssocDef(assoc.getModel())) {
            if (isAssocDef(oldModel)) {
                updateAssocDef(assoc);
            } else {
                createAssocDef(assoc);
            }
        } else if (isAssocDef(oldModel)) {
            removeAssocDef(assoc);
        }
    }

    // Note: we listen to the PRE event here, not the POST event. At POST time the assocdef sequence might be
    // interrupted, which would result in a corrupted sequence once rebuild. (Due to the interruption, while
    // rebuilding not all segments would be catched for deletion and recreated redundantly -> ambiguity.)
    @Override
    public void preDeleteAssociation(Association assoc) {
        if (isAssocDef(assoc.getModel())) {
            removeAssocDef(assoc);
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private void createAssocDef(Association assoc) {
        Type parentType = fetchParentType(assoc);
        String childTypeUri = fetchChildType(assoc).getUri();
        // Note: the assoc def's ID is already known. Setting it explicitely
        // prevents the core from creating the underlying association.
        AssociationDefinitionModel assocDef = new AssociationDefinitionModel(
            assoc.getId(), assoc.getUri(), assoc.getTypeUri(), null,
            parentType.getUri(), childTypeUri, "dm4.core.one", "dm4.core.one",
            null    // customAssocTypeUri=null, viewConfigModel=null
        );
        logger.info("### Adding association definition \"" + childTypeUri + "\" to type \"" + parentType.getUri() +
            "\" (" + assocDef + ")");
        //
        parentType.addAssocDef(assocDef);
        //
        addUpdateTypeDirective(parentType);
    }

    private void updateAssocDef(Association assoc) {
        Type parentType = fetchParentType(assoc);
        AssociationDefinitionModel assocDef = dms.getTypeStorage().fetchAssociationDefinition(assoc);
        logger.info("### Updating association definition \"" + assocDef.getChildTypeUri() + "\" of type \"" +
            parentType.getUri() + "\" (" + assocDef + ")");
        //
        parentType.updateAssocDef(assocDef);
        //
        addUpdateTypeDirective(parentType);
    }

    private void removeAssocDef(Association assoc) {
        Type parentType = fetchParentType(assoc);
        String childTypeUri = fetchChildType(assoc).getUri();
        logger.info("### Removing association definition \"" + childTypeUri + "\" from type \"" + parentType.getUri() +
            "\"");
        //
        dms.getTypeStorage().removeAssociationDefinitionFromMemoryAndRebuildSequence(parentType, childTypeUri);
        //
        addUpdateTypeDirective(parentType);
    }



    // === Helper ===

    private boolean isAssocDef(AssociationModel assoc) {
        String typeUri = assoc.getTypeUri();
        if (!typeUri.equals("dm4.core.aggregation_def") &&
            !typeUri.equals("dm4.core.composition_def")) {
            return false;
        }
        //
        if (assoc.hasSameRoleTypeUris()) {
            return false;
        }
        //
        if (assoc.getRoleModel("dm4.core.parent_type") == null ||
            assoc.getRoleModel("dm4.core.child_type") == null)  {
            return false;
        }
        //
        return true;
    }

    // ### TODO: adding the UPDATE directive should be the responsibility of a type. The Type interface's
    // ### addAssocDef(), updateAssocDef(), and removeAssocDef() methods should have a "directives" parameter.
    private void addUpdateTypeDirective(Type type) {
        if (type.getTypeUri().equals("dm4.core.topic_type")) {
            Directives.get().add(Directive.UPDATE_TOPIC_TYPE, type);
        } else if (type.getTypeUri().equals("dm4.core.assoc_type")) {
            Directives.get().add(Directive.UPDATE_ASSOCIATION_TYPE, type);
        }
        // Note: no else here as error check already performed in fetchParentType()
    }

    // ---

    private Type fetchParentType(Association assoc) {
        Topic type = dms.getTypeStorage().fetchParentType(assoc);
        String typeUri = type.getTypeUri();
        if (typeUri.equals("dm4.core.topic_type")) {
            return dms.getTopicType(type.getUri());
        } else if (typeUri.equals("dm4.core.assoc_type")) {
            return dms.getAssociationType(type.getUri());
        } else {
            throw new RuntimeException("Invalid association definition: the dm4.core.parent_type " +
                "player is not a type but of type \"" + typeUri + "\" (" + assoc + ")");
        }
    }

    private Topic fetchChildType(Association assoc) {
        return dms.getTypeStorage().fetchChildType(assoc);
    }
}
