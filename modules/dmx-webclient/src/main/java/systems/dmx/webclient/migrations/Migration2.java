package systems.dmx.webclient.migrations;

import static systems.dmx.webclient.Constants.*;
import static systems.dmx.core.Constants.*;
import systems.dmx.core.AssocType;
import systems.dmx.core.service.Migration;



/**
 * Add view configs to Core types.
 * <p>
 * Part of DMX 5.0-beta-4
 * Runs ALWAYS
 */
public class Migration2 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        addIconToTopicType(META_TYPE,  "\uf1ce");    // fa-circle-o-notch
        addIconToTopicType(TOPIC_TYPE, "\uf10c");    // fa-circle-o
        addIconToTopicType(ASSOC_TYPE, "\uf192");    // fa-dot-circle-o
        addIconToTopicType(ROLE_TYPE,  "\uf04b");    // fa-play
        addIconToTopicType(PLUGIN,     "\uf12e");    // fa-puzzle-piece
        // colors match dmx-color-picker
        // Note: color values are not aligned by extra spaces. Cytoscape style parsing would fail.
        addColorToAssocType(ASSOCIATION,       "hsl(0, 0%, 80%)");
        addColorToAssocType(COMPOSITION,       "hsl(5, 50%, 53%)");
        addColorToAssocType(COMPOSITION_DEF,   "hsl(210, 50%, 53%)");
        addColorToAssocType(INSTANTIATION,     "hsl(180, 50%, 53%)");
        addColorToAssocType(SEQUENCE,          "hsl(60, 80%, 53%)");
        addColorToAssocType(CUSTOM_ASSOC_TYPE, "hsl(5, 50%, 53%)");
        addBackgroundColorToAssocType(ASSOCIATION,       "hsl(0, 0%, 97%)");
        addBackgroundColorToAssocType(COMPOSITION,       "hsl(5, 80%, 96%)");
        addBackgroundColorToAssocType(COMPOSITION_DEF,   "hsl(210, 80%, 96%)");
        addBackgroundColorToAssocType(INSTANTIATION,     "hsl(180, 80%, 96%)");
        addBackgroundColorToAssocType(SEQUENCE,          "hsl(60, 80%, 96%)");
        addBackgroundColorToAssocType(CUSTOM_ASSOC_TYPE, "hsl(5, 80%, 96%)");
        //
        AssocType compDef = dmx.getAssocType(COMPOSITION_DEF);
        compDef.getCompDef(CARDINALITY)
            .getViewConfig()
                .setConfigValueRef(VIEW_CONFIG, WIDGET, SELECT);
        compDef.getCompDef(ASSOC_TYPE + "#" + CUSTOM_ASSOC_TYPE)
            .getViewConfig()
                .setConfigValueRef(VIEW_CONFIG, WIDGET, SELECT)
                .setConfigValue(VIEW_CONFIG, CLEARABLE, true);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void addIconToTopicType(String topicTypeUri, String icon) {
        setTopicTypeViewConfigValue(topicTypeUri, "icon", icon);
    }

    private void addColorToAssocType(String assocTypeUri, String color) {
        setAssocTypeViewConfigValue(assocTypeUri, "color", color);
    }

    private void addBackgroundColorToAssocType(String assocTypeUri, String color) {
        setAssocTypeViewConfigValue(assocTypeUri, "color#" + BACKGROUND_COLOR, color);
    }
}
