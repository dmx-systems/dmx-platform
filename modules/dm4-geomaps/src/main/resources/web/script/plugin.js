dm4c.add_plugin("de.deepamehta.geomaps", function() {

    dm4c.load_stylesheet("/de.deepamehta.geomaps/script/vendor/openlayers/theme/default/style.css")

    dm4c.load_script("/de.deepamehta.geomaps/script/topicmap_renderers/geomap_renderer.js")
    dm4c.load_script("/de.deepamehta.geomaps/script/topicmap_renderers/geomap_viewmodel.js")
    dm4c.load_script("/de.deepamehta.geomaps/script/topicmap_renderers/openlayers_view.js")
    dm4c.load_script("/de.deepamehta.geomaps/script/vendor/openlayers/OpenLayers.js")

    // === REST Client Extension ===

    // Note: this is done at plugin instantiation time (*before* the webclient's "init" event is fired)
    // because a geomap might be loaded at init() time through the topicmaps plugin.

    dm4c.restc.get_geomap = function(geomap_id) {
        return this.request("GET", "/geomap/" + geomap_id)
    }
    dm4c.restc.get_geotopic = function(topic_id) {
        return this.request("GET", "/geomap/topic/" + topic_id)
    }
    dm4c.restc.add_topic_to_geomap = function(geomap_id, topic_id) {
        return this.request("PUT", "/geomap/" + geomap_id + "/topic/" + topic_id)
    }
    dm4c.restc.set_geomap_state = function(geomap_id, center, zoom) {
        return this.request("PUT", "/geomap/" + geomap_id + "/center/" + center.lon + "/" + center.lat +
            "/zoom/" + zoom)
    }

    // === Topicmaps Listeners ===

    dm4c.add_listener("topicmap_renderer", function() {
        return new GeomapRenderer()
    })

    // ------------------------------------------------------------------------------------------------------ Public API

    /**
     * Returns the geo coordinate of the given domain topic.
     *
     * @return  A Topic object of type dm4.geomaps.geo_coordinate,
     *          or undefined if the domain topic has no geo coordinate.
     */
    this.get_geo_coordinate = function(topic) {
        return topic.find_child_topic("dm4.geomaps.geo_coordinate")
    }
})
