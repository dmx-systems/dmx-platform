package de.deepamehta.webclient.migrations;

import de.deepamehta.core.service.Migration;



/**
 * Add view configs to Core types.
 * Runs ALWAYS.
 * <p>
 * Part of DM 4.0
 */
public class Migration2 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        addIconToTopicType("dm4.core.meta_type",   "\uf111");
        addIconToTopicType("dm4.core.topic_type",  "\uf111");
        addIconToTopicType("dm4.core.assoc_type",  "\uf111");
        addIconToTopicType("dm4.core.data_type",   "\uf111");
        addIconToTopicType("dm4.core.cardinality", "\uf111");
        addIconToTopicType("dm4.core.index_mode",  "\uf111");
        addIconToTopicType("dm4.core.plugin",      "\uf12e");
        //
        // Note: on the canvas HSL-specified colors are rendered pale (Safari and Firefox).
        // Update: this is because HSB is not the same as HSL. The values here are actually HSB, but CSS expects HSL.
        addColorToAssociationType("dm4.core.association",     "rgb(178, 178, 178)" /*"hsl(  0,  0%, 75%)"*/);
        addColorToAssociationType("dm4.core.aggregation",     "rgb(53, 223, 59)"   /*"hsl(120, 65%, 90%)"*/);
        addColorToAssociationType("dm4.core.composition",     "rgb(231, 62, 60)"   /*"hsl(  0, 65%, 90%)"*/);
        addColorToAssociationType("dm4.core.aggregation_def", "rgb(44, 178, 48)"   /*"hsl(120, 65%, 75%)"*/);
        addColorToAssociationType("dm4.core.composition_def", "rgb(184, 51, 49)"   /*"hsl(  0, 65%, 75%)"*/);
        addColorToAssociationType("dm4.core.instantiation",   "rgb(41, 194, 225)"  /*"hsl(190, 65%, 90%)"*/);
        addColorToAssociationType("dm4.core.sequence",        "rgb(228, 223, 55)"  /*"hsl( 60, 65%, 90%)"*/);
        //
        setSelectWidget("dm4.core.composition_def");
        setSelectWidget("dm4.core.aggregation_def");
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void addIconToTopicType(String topicTypeUri, String icon) {
        setTopicTypeViewConfigValue(topicTypeUri, "icon", icon);
    }

    private void addColorToAssociationType(String assocTypeUri, String color) {
        setAssocTypeViewConfigValue(assocTypeUri, "color", color);
    }

    // ---

    private void setSelectWidget(String assocTypeUri) {
        dm4.getAssociationType(assocTypeUri).getAssocDef("dm4.core.assoc_type#dm4.core.custom_assoc_type")
            .getViewConfig()
                .setConfigValueRef("dm4.webclient.view_config", "dm4.webclient.widget", "dm4.webclient.select")
                .setConfigValue("dm4.webclient.view_config", "dm4.webclient.clearable", true);
    }
}
