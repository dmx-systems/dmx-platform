package de.deepamehta.plugins.webclient.migrations;

import de.deepamehta.core.service.Migration;



public class Migration2 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        addIconToTopicType("dm4.core.meta_type",   "box-grey.png");
        addIconToTopicType("dm4.core.topic_type",  "box-blue.png");
        addIconToTopicType("dm4.core.assoc_type",  "box-red.png");
        addIconToTopicType("dm4.core.data_type",   "box-green.png");
        addIconToTopicType("dm4.core.cardinality", "box-yellow.png");
        addIconToTopicType("dm4.core.index_mode",  "box-orange.png");
        addIconToTopicType("dm4.core.plugin",      "puzzle.png");
        //
        // Note: on the canvas HSL-specified colors are rendered pale (Safari and Firefox)
        addColorToAssociationType("dm4.core.association",     "rgb(178, 178, 178)" /*"hsl(  0,  0%, 75%)"*/);
        addColorToAssociationType("dm4.core.aggregation",     "rgb( 53, 223,  59)" /*"hsl(120, 65%, 90%)"*/);
        addColorToAssociationType("dm4.core.composition",     "rgb(231,  62,  60)" /*"hsl(  0, 65%, 90%)"*/);
        addColorToAssociationType("dm4.core.aggregation_def", "rgb( 44, 178,  48)" /*"hsl(120, 65%, 75%)"*/);
        addColorToAssociationType("dm4.core.composition_def", "rgb(184,  51,  49)" /*"hsl(  0, 65%, 75%)"*/);
        addColorToAssociationType("dm4.core.instantiation",   "rgb( 41, 194, 225)" /*"hsl(190, 65%, 90%)"*/);
        addColorToAssociationType("dm4.core.sequence",        "rgb(228, 223,  55)" /*"hsl( 60, 65%, 90%)"*/);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void addIconToTopicType(String topicTypeUri, String iconfile) {
        addTopicTypeSetting(topicTypeUri, "icon_src", "/images/" + iconfile);
    }

    private void addColorToAssociationType(String assocTypeUri, String color) {
        addAssociationTypeSetting(assocTypeUri, "color", color);
    }
}
