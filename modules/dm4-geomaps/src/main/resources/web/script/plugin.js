dm4c.add_plugin("de.deepamehta.geomaps", function() {

    dm4c.load_stylesheet("/de.deepamehta.geomaps/script/vendor/openlayers/theme/default/style.css")

    dm4c.load_script("/de.deepamehta.geomaps/script/topicmap_renderers/geomap_renderer.js")
    dm4c.load_script("/de.deepamehta.geomaps/script/model/geomap.js")
    dm4c.load_script("/de.deepamehta.geomaps/script/view/openlayers_view.js")
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
     * Returns the geo facet of the given topic.
     *
     * @return  A "Geo Coordinate" topic extended with "x" and "y" properties (a Topic object).
     */
    this.get_geo_facet = function(topic) {
        var address = topic.find_child_topic("dm4.contacts.address")
        if (address) {
            var geo_facet = address.get("dm4.geomaps.geo_coordinate")
            if (geo_facet) {
                var pos = GeomapRenderer.position(geo_facet)
                geo_facet.x = pos.x
                geo_facet.y = pos.y
                return geo_facet
            }
        }
    }
})
