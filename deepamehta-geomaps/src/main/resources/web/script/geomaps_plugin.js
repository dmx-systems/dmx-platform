function geomaps_plugin() {

    dm4c.register_css_stylesheet("/de.deepamehta.geomaps/script/vendor/openlayers/theme/default/style.css")

    dm4c.javascript_source("/de.deepamehta.geomaps/script/topicmap_renderers/geomap_renderer.js")
    dm4c.javascript_source("/de.deepamehta.geomaps/script/vendor/openlayers/OpenLayers.js")

    // extend REST client
    //
    // Note: this is done at plugin instantiation time (*before* the webclient's init() hook is triggered)
    // because a geomap might be loaded at init() time through the topicmaps plugin.
    //
    dm4c.restc.get_geotopic = function(topic_id) {
        return this.request("GET", "/geomap/topic/" + topic_id)
    }
    dm4c.restc.get_geomap = function(geomap_id) {
        return this.request("GET", "/geomap/" + geomap_id)
    }
    dm4c.restc.add_topic_to_geomap = function(geomap_id, topic_id) {
        return this.request("PUT", "/geomap/" + geomap_id + "/topic/" + topic_id)
    }
    dm4c.restc.set_geomap_state = function(geomap_id, center, zoom) {
        return this.request("PUT", "/geomap/" + geomap_id + "/center/" + center.lon + "/" + center.lat +
            "/zoom/" + zoom)
    }



    // ***********************************************************
    // *** Topicmaps Hooks (triggered by deepamehta-topicmaps) ***
    // ***********************************************************



    this.topicmap_renderer = function() {
        return new GeoMapRenderer()
    }

    // ----------------------------------------------------------------------------------------------- Private Functions
}
