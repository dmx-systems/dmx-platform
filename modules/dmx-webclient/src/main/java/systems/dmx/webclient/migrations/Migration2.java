package systems.dmx.webclient.migrations;

import systems.dmx.core.AssociationType;
import systems.dmx.core.service.Migration;



/**
 * Add view configs to Core types.
 * <p>
 * Part of DMX 5.0
 * Runs ALWAYS
 */
public class Migration2 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        addIconToTopicType("dmx.core.meta_type",   "\uf10c");       // fa-circle-o
        addIconToTopicType("dmx.core.topic_type",  "\uf10c");       // fa-circle-o
        addIconToTopicType("dmx.core.assoc_type",  "\uf192");       // fa-dot-circle-o
        addIconToTopicType("dmx.core.data_type",   "\uf10c");       // fa-circle-o
        addIconToTopicType("dmx.core.cardinality", "\uf10c");       // fa-circle-o
        addIconToTopicType("dmx.core.index_mode",  "\uf10c");       // fa-circle-o
        addIconToTopicType("dmx.core.plugin",      "\uf12e");       // fa-puzzle-piece
        //
        // Note: on the canvas HSL-specified colors are rendered pale (Safari and Firefox).
        // Update: this is because HSB is not the same as HSL. The values here are actually HSB, but CSS expects HSL.
        addColorToAssociationType("dmx.core.association",     "rgb(178, 178, 178)" /*"hsl(  0,  0%, 75%)"*/);
        addColorToAssociationType("dmx.core.composition",     "rgb(231, 62, 60)"   /*"hsl(  0, 65%, 90%)"*/);
        addColorToAssociationType("dmx.core.composition_def", "rgb(184, 51, 49)"   /*"hsl(  0, 65%, 75%)"*/);
        addColorToAssociationType("dmx.core.instantiation",   "rgb(41, 194, 225)"  /*"hsl(190, 65%, 90%)"*/);
        addColorToAssociationType("dmx.core.sequence",        "rgb(228, 223, 55)"  /*"hsl( 60, 65%, 90%)"*/);
        //
        AssociationType compDef = dmx.getAssociationType("dmx.core.composition_def");
        compDef.getAssocDef("dmx.core.cardinality")
            .getViewConfig()
                .setConfigValueRef("dmx.webclient.view_config", "dmx.webclient.widget", "dmx.webclient.select");
        compDef.getAssocDef("dmx.core.assoc_type#dmx.core.custom_assoc_type")
            .getViewConfig()
                .setConfigValueRef("dmx.webclient.view_config", "dmx.webclient.widget", "dmx.webclient.select")
                .setConfigValue("dmx.webclient.view_config", "dmx.webclient.clearable", true);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void addIconToTopicType(String topicTypeUri, String icon) {
        setTopicTypeViewConfigValue(topicTypeUri, "icon", icon);
    }

    private void addColorToAssociationType(String assocTypeUri, String color) {
        setAssocTypeViewConfigValue(assocTypeUri, "color", color);
    }
}
