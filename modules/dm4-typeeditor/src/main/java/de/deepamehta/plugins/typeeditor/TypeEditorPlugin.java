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
        Type wholeType = fetchWholeType(assoc);
        String partTypeUri = fetchPartType(assoc).getUri();
        // Note: the assoc def's ID is already known. Setting it explicitely
        // prevents the core from creating the underlying association.
        AssociationDefinitionModel assocDef = new AssociationDefinitionModel(
            assoc.getId(), assoc.getUri(), assoc.getTypeUri(),
            wholeType.getUri(), partTypeUri, "dm4.core.one", "dm4.core.one",
            null    // viewConfigModel=null
        );
        logger.info("### Adding association definition \"" + partTypeUri + "\" to type \"" + wholeType.getUri() +
            "\" (" + assocDef + ")");
        //
        wholeType.addAssocDef(assocDef);
        //
        addUpdateTypeDirective(wholeType, directives);
    }

    private void updateAssocDef(Association assoc, Directives directives) {
        Type wholeType = fetchWholeType(assoc);
        AssociationDefinitionModel assocDef = dms.getObjectFactory().fetchAssociationDefinition(assoc);
        logger.info("### Updating association definition \"" + assocDef.getPartTypeUri() + "\" of type \"" +
            wholeType.getUri() + "\" (" + assocDef + ")");
        //
        wholeType.updateAssocDef(assocDef);
        //
        addUpdateTypeDirective(wholeType, directives);
    }

    private void removeAssocDef(Association assoc, Directives directives) {
        Type wholeType = fetchWholeType(assoc);
        String partTypeUri = fetchPartType(assoc).getUri();
        logger.info("### Removing association definition \"" + partTypeUri + "\" from type \"" + wholeType.getUri() +
            "\"");
        //
        wholeType.removeAssocDef(partTypeUri);
        //
        addUpdateTypeDirective(wholeType, directives);
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
        if (assoc.getRoleModel("dm4.core.whole_type") == null ||
            assoc.getRoleModel("dm4.core.part_type") == null)  {
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
        // Note: no else here as error check already performed in fetchWholeType()
    }

    // ---

    private Type fetchWholeType(Association assoc) {
        Topic type = dms.getObjectFactory().fetchWholeType(assoc);
        String typeUri = type.getTypeUri();
        if (typeUri.equals("dm4.core.topic_type")) {
            return dms.getTopicType(type.getUri(), null);
        } else if (typeUri.equals("dm4.core.assoc_type")) {
            return dms.getAssociationType(type.getUri(), null);
        } else {
            throw new RuntimeException("Invalid association definition: the dm4.core.whole_type " +
                "player is not a type but of type \"" + typeUri + "\" (" + assoc + ")");
        }
    }

    private Topic fetchPartType(Association assoc) {
        return dms.getObjectFactory().fetchPartType(assoc);
    }
}
