package de.deepamehta.plugins.typeeditor;

import de.deepamehta.core.Association;
import de.deepamehta.core.service.Plugin;

import java.util.logging.Logger;



public class TypeEditorPlugin extends Plugin {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************************
    // *** Core Hooks (called from DeepaMehta 3 Core) ***
    // **************************************************



    @Override
    public void postRetypeAssociationHook(Association assoc, String oldTypeUri) {
        logger.info("### \"" + oldTypeUri + "\" -> \"" + assoc.getTypeUri() + "\"");
        if (assoc.getTypeUri().equals("dm3.core.aggregation_def") ||
            assoc.getTypeUri().equals("dm3.core.composition_def")) {
            // TODO
        }
    }
}
