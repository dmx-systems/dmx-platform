package de.deepamehta.plugins.typeeditor;

import de.deepamehta.core.Association;
import de.deepamehta.core.Topic;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.event.PostDeleteAssociationListener;
import de.deepamehta.core.service.event.PostUpdateAssociationListener;

import java.util.logging.Logger;



public class TypeEditorPlugin extends PluginActivator implements PostUpdateAssociationListener,
                                                                 PostDeleteAssociationListener {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void postUpdateAssociation(Association assoc, AssociationModel oldModel, ClientState clientState,
                                                                                    Directives directives) {
        if (isAssocDef(assoc.getModel())) {
            if (isAssocDef(oldModel)) {
                updateAssocDef(assoc, directives);
            } else {
                createAssocDef(assoc, directives);
            }
        } else if (isAssocDef(oldModel)) {
            removeAssocDef(assoc, directives);
        }
    }

    @Override
    public void postDeleteAssociation(Association assoc, Directives directives) {
        if (isAssocDef(assoc.getModel())) {
            removeAssocDef(assoc, directives);
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private void createAssocDef(Association assoc, Directives directives) {
        Type parentType = fetchParentType(assoc);
        String childTypeUri = fetchChildType(assoc).getUri();
        // Note: the assoc def's ID is already known. Setting it explicitely
        // prevents the core from creating the underlying association.
        AssociationDefinitionModel assocDef = new AssociationDefinitionModel(
            assoc.getId(), assoc.getUri(), assoc.getTypeUri(),
            parentType.getUri(), childTypeUri, "dm4.core.one", "dm4.core.one",
            null    // viewConfigModel=null
        );
        logger.info("### Adding association definition \"" + childTypeUri + "\" to type \"" + parentType.getUri() +
            "\" (" + assocDef + ")");
        //
        parentType.addAssocDef(assocDef);
        //
        addUpdateTypeDirective(parentType, directives);
    }

    private void updateAssocDef(Association assoc, Directives directives) {
        Type parentType = fetchParentType(assoc);
        AssociationDefinitionModel assocDef = dms.getTypeStorage().fetchAssociationDefinition(assoc);
        logger.info("### Updating association definition \"" + assocDef.getChildTypeUri() + "\" of type \"" +
            parentType.getUri() + "\" (" + assocDef + ")");
        //
        parentType.updateAssocDef(assocDef);
        //
        addUpdateTypeDirective(parentType, directives);
    }

    private void removeAssocDef(Association assoc, Directives directives) {
        Type parentType = fetchParentType(assoc);
        String childTypeUri = fetchChildType(assoc).getUri();
        logger.info("### Removing association definition \"" + childTypeUri + "\" from type \"" + parentType.getUri() +
            "\"");
        //
        parentType.removeAssocDef(childTypeUri);
        //
        addUpdateTypeDirective(parentType, directives);
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

    private void addUpdateTypeDirective(Type type, Directives directives) {
        if (type.getTypeUri().equals("dm4.core.topic_type")) {
            directives.add(Directive.UPDATE_TOPIC_TYPE, type);
        } else if (type.getTypeUri().equals("dm4.core.assoc_type")) {
            directives.add(Directive.UPDATE_ASSOCIATION_TYPE, type);
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
