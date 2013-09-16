/**
 * A geomap view model that is attached to the database. There are methods for:
 *  - loading a geomap from DB
 *  - manipulating the geomap by e.g. adding/removing topics
 *
 * ### TODO: introduce common base class for GeomapViewmodel and TopicmapViewmodel (see dm4-topicmaps module)
 */
function GeomapViewmodel(topicmap_id, config) {

    var LOG_GEOMAPS = false
    var self = this

    // Model
    var info                        // The underlying Topicmap topic (a Topic object)
    var topics = {}                 // topics of this topicmap (key: topic ID, value: GeoTopicViewmodel object)
    this.center                     // map center (an OpenLayers.LonLat object in lon/lat projection)
    this.zoom                       // zoom level (integer)
    this.selected_object_id = -1    // ID of the selected topic, or -1 for no selection

    load()

    // ------------------------------------------------------------------------------------------------------ Public API



    // === TopicmapViewmodel Implementation ===

    this.get_id = function() {
        return topicmap_id
    }

    this.get_name = function() {
        return info.get("dm4.topicmaps.name")
    }

    this.get_renderer_uri = function() {
        return info.get("dm4.topicmaps.topicmap_renderer_uri")
    }

    // ---

    this.iterate_topics = function(visitor_func) {
        for (var id in topics) {
            visitor_func(topics[id])
        }
    }

    // ---

    this.add_topic = function(id, type_uri, value, x, y) {
        if (x == undefined || y == undefined) {
            throw "GeomapError: no coordinates provided while calling add_topic (topic_id=" + id + ")"
        }
        //
        if (!topics[id]) {
            if (LOG_GEOMAPS) dm4c.log("Adding topic " + id + " (type_uri=\"" + type_uri + "\", x=" + x + ", y=" + y +
                ") to geomap " + topicmap_id)
            // update memory
            topics[id] = new GeoTopicViewmodel(id, x, y)
            // update DB
            if (is_writable()) {
                dm4c.restc.add_topic_to_geomap(topicmap_id, id)
            }
        }
    }

    this.add_association = function(id, type_uri, topic_id_1, topic_id_2) {
    }

    // ---

    this.update_topic = function(topic) {
        var t = topics[topic.id]
        if (t) {
            if (LOG_GEOMAPS) dm4c.log("..... Updating topic " + t.id + " (x=" + t.x + ", y=" + t.y + ") on geomap " +
                topicmap_id)
            // update memory
            t.update(topic)
            // Note: no DB update here. A topic update doesn't affect the persisted view.
        }
    }

    this.update_association = function(assoc) {
    }

    // ---

    this.delete_topic = function(id) {
        var topic = topics[id]
        if (topic) {
            if (LOG_GEOMAPS) dm4c.log("..... Deleting topic " + id + " from geomap " + topicmap_id)
            // update memory
            topic.remove()
            // Note: no DB update here. The "Geotopic Mapcontext" association is already deleted.
        }
    }

    this.delete_association = function(id) {
    }

    // ---

    this.set_topic_selection = function(topic) {
        // update memory
        this.selected_object_id = topic.id
        // Note: no DB update here. The selection is not yet persisted.
    }

    this.set_association_selection = function(assoc) {
    }

    this.reset_selection = function() {
        // update memory
        this.selected_object_id = -1
        // Note: no DB update here. The selection is not yet persisted.
    }

    // ---

    this.prepare_topic_for_display = function(topic) {
    }

    // ===

    /**
     * @param   center      an OpenLayers.LonLat object in lon/lat projection
     */
    this.set_state = function(center, zoom) {
        // update memory
        this.center = center
        this.zoom = zoom
        // update DB
        if (is_writable()) {
            dm4c.restc.set_geomap_state(topicmap_id, center, zoom)
        }
    }



    // ----------------------------------------------------------------------------------------------- Private Functions

    function load() {
        var topicmap = dm4c.restc.get_geomap(topicmap_id)
        info = new Topic(topicmap.info)
        //
        init_topics()
        init_state()

        function init_topics() {
            for (var i = 0, topic; topic = topicmap.topics[i]; i++) {
                var pos = GeomapRenderer.position(new Topic(topic))
                topics[topic.id] = new GeoTopicViewmodel(topic.id, pos.x, pos.y)
            }
        }

        function init_state() {
            var state = info.get("dm4.topicmaps.state")
            var trans = state.get("dm4.topicmaps.translation")
            var lon = trans.get("dm4.topicmaps.translation_x")
            var lat = trans.get("dm4.topicmaps.translation_y")
            self.center = new OpenLayers.LonLat(lon, lat)
            self.zoom = state.get("dm4.topicmaps.zoom_level")
        }
    }

    // ---

    function is_writable() {
        return config.is_writable
    }



    // ------------------------------------------------------------------------------------------------- Private Classes

    function GeoTopicViewmodel(id, x, y) {

        this.id = id
        this.x = x
        this.y = y

        /**
         * @param   topic   a Topic object of type dm4.geomaps.geo_coordinate
         */
        this.update = function(topic) {
            // Note: for a geo topic an update request might result in a geometry change (in case the Address changes).
            // (not so for non-geo topics).
            var p = GeomapRenderer.position(topic)
            this.x = p.x
            this.y = p.y
        }

        this.remove = function() {
            delete topics[id]
            reset_selection()
        }

        // ---

        function reset_selection() {
            if (self.selected_object_id == id) {
                self.selected_object_id = -1
            }
        }
    }
}
