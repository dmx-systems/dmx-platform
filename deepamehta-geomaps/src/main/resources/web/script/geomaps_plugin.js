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
            var topicmap = dm4c.get_plugin("topicmaps_plugin").get_topicmap()
            if (topicmap.get_renderer_uri() == "dm4.geomaps.geomap_renderer") {
                // ### alert("geomaps_plugin(): process ADD_MARKER directive\n\n" + JSON.stringify(directive.arg))
                var topic = directive.arg
                topic.x = topic.composite["dm4.geomaps.longitude"]
                topic.y = topic.composite["dm4.geomaps.latitude"]
                // update model
                topicmap.add_topic(topic.id, topic.type_uri, "", topic.x, topic.y)
                // update view
                dm4c.canvas.add_topic(topic)
            }
        }
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

}
