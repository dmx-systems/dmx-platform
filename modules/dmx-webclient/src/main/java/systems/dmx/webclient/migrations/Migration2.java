package systems.dmx.webclient.migrations;

import systems.dmx.core.AssociationType;
import systems.dmx.core.service.Migration;



/**
 * Add view configs to Core types.
 * <p>
 * Part of DMX 5.0-beta-3
 * Runs ALWAYS
 */
public class Migration2 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        addIconToTopicType("dmx.core.meta_type",  "\uf1ce");    // fa-circle-o-notch
        addIconToTopicType("dmx.core.topic_type", "\uf10c");    // fa-circle-o
        addIconToTopicType("dmx.core.assoc_type", "\uf192");    // fa-dot-circle-o
        addIconToTopicType("dmx.core.role_type",  "\uf04b");    // fa-play
        addIconToTopicType("dmx.core.plugin",     "\uf12e");    // fa-puzzle-piece
        // colors match dm5-color-picker
        // Note: color values are not aligned by extra spaces. Cytoscape style parsing would fail.
        addColorToAssociationType("dmx.core.association",     "hsl(0, 0%, 80%)");
        addColorToAssociationType("dmx.core.composition",     "hsl(5, 50%, 53%)");
        addColorToAssociationType("dmx.core.composition_def", "hsl(210, 50%, 53%)");
        addColorToAssociationType("dmx.core.instantiation",   "hsl(180, 50%, 53%)");
        addColorToAssociationType("dmx.core.sequence",        "hsl(60, 80%, 53%)");
        addBackgroundColorToAssociationType("dmx.core.association",     "hsl(0, 0%, 97%)");
        addBackgroundColorToAssociationType("dmx.core.composition",     "hsl(5, 80%, 96%)");
        addBackgroundColorToAssociationType("dmx.core.composition_def", "hsl(210, 80%, 96%)");
        addBackgroundColorToAssociationType("dmx.core.instantiation",   "hsl(180, 80%, 96%)");
        addBackgroundColorToAssociationType("dmx.core.sequence",        "hsl(60, 80%, 96%)");
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

    private void addBackgroundColorToAssociationType(String assocTypeUri, String color) {
        setAssocTypeViewConfigValue(assocTypeUri, "color#dmx.webclient.background_color", color);
    }
}
