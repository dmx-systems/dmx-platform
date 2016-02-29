package de.deepamehta.plugins.typeeditor;

import de.deepamehta.core.Association;
import de.deepamehta.core.Topic;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
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
    public void postUpdateAssociation(Association assoc, AssociationModel newModel, AssociationModel oldModel) {
        AssociationModel _newModel = assoc.getModel();  // ### TODO: can we rely on newModel instead? I don't think so.
        if (isAssocDef(_newModel)) {
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
        logger.info("##### Adding association definition \"" + assoc.getId() + "\" to type \"" +
            parentType.getUri() + "\"");
        //
        parentType._addAssocDef(assoc);
        //
        addUpdateTypeDirective(parentType);
    }

    private void updateAssocDef(Association assoc) {
        Type parentType = fetchParentType(assoc);
        logger.info("##### Updating association definition " + assoc.getId() + " of type \"" +
            parentType.getUri() + "\"");
        //
        parentType._updateAssocDef(assoc);
        //
        addUpdateTypeDirective(parentType);
    }

    private void removeAssocDef(Association assoc) {
        Type parentType = fetchParentType(assoc);
        logger.info("##### Removing association definition \"" + assoc.getId() + "\" from type \"" +
            parentType.getUri() + "\"");
        //
        parentType._removeAssocDefFromMemoryAndRebuildSequence(assoc);
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
        if (assoc.getRoleModel("dm4.core.parent_type") == null ||
            assoc.getRoleModel("dm4.core.child_type") == null)  {
            return false;
        }
        //
        return true;
    }

    // ### TODO: adding the UPDATE directive should be the responsibility of a type. The Type interface's
    // ### addAssocDef(), updateAssocDef(), and removeAssocDef() methods should have a "directives" parameter ### FIXDOC
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
        TopicModel type = dms.getTypeStorage().fetchParentType(assoc);
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
}
