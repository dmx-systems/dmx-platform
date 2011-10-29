function geomaps_plugin() {

    dm4c.register_css_stylesheet("/de.deepamehta.geomaps/script/vendor/openlayers/theme/default/style.css")

    dm4c.javascript_source("/de.deepamehta.geomaps/script/topicmap_renderers/geomap_renderer.js")
    dm4c.javascript_source("/de.deepamehta.geomaps/script/vendor/openlayers/OpenLayers.js")



    // ***********************************************************
    // *** Topicmaps Hooks (triggered by deepamehta-topicmaps) ***
    // ***********************************************************



    this.topicmap_renderer = function() {
        return new GeoMapRenderer()
    }

    this.process_directive = function(directive) {
        if (directive.type == "ADD_MARKER") {
            alert("Geomaps module: process ADD_MARKER directive\n\n" + JSON.stringify(directive.arg))
        }
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

}
