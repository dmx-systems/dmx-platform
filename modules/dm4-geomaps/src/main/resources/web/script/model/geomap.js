/**
 * A geomap model that is attached to the database. There are methods for:
 *  - loading a geomap from DB
 *  - manipulating the geomap by e.g. adding/removing topics
 *
 * ### TODO: introduce common base class for Geomap and Topicmap (see deepamehta-topicmaps module)
 */
function Geomap(topicmap_id, config) {

    var LOG_GEOMAPS = false
    var self = this

    // Model
    var info                        // The underlying Topicmap topic (a Topic object)
    var topics = {}                 // topics of this topicmap (key: topic ID, value: GeomapTopic object)
    this.center                     // map center (an OpenLayers.LonLat object in lon/lat projection)
    this.zoom                       // zoom level (integer)
    this.selected_object_id = -1    // ID of the selected topic, or -1 for no selection

    load()



    // ------------------------------------------------------------------------------------------------------ Public API

    // === Topicmap Implementation ===

    this.get_id = function() {
        return topicmap_id
    }

    this.get_name = function() {
        return info.get("dm4.topicmaps.name")
    }

    this.get_renderer_uri = function() {
        return info.get("dm4.topicmaps.topicmap_renderer_uri")
    }

    this.iterate_topics = function(visitor_func) {
        for (var id in topics) {
            visitor_func(topics[id])
        }
    }

    this.add_topic = function(id, type_uri, value, x, y) {
        if (x == undefined || y == undefined) {
            throw "GeomapError: no coordinates provided while calling add_topic (topic_id=" + id + ")"
        }
        //
        if (!topics[id]) {
            if (LOG_GEOMAPS) dm4c.log("Adding topic " + id + " (type_uri=\"" + type_uri + "\", x=" + x + ", y=" + y +
                ") to geomap " + topicmap_id)
            // update DB
            if (is_writable()) {
                dm4c.restc.add_topic_to_geomap(topicmap_id, id)
            }
            // update memory
            topics[id] = new GeomapTopic(id, x, y)
        }
    }

    this.add_association = function(id, type_uri, topic_id_1, topic_id_2) {
    }

    this.update_topic = function(topic) {
        var t = topics[topic.id]
        if (t) {
            if (LOG_GEOMAPS) dm4c.log("..... Updating topic " + t.id + " (x=" + t.x + ", y=" + t.y + ") on geomap " +
                topicmap_id)
            t.update(topic)
            // ### FIXME: check is_writable()
        }
    }

    this.update_association = function(assoc) {
    }

    this.delete_topic = function(id) {
        var topic = topics[id]
        if (topic) {
            if (LOG_GEOMAPS) dm4c.log("..... Deleting topic " + id + " from geomap " + topicmap_id)
            topic.remove()
            // ### FIXME: check is_writable()
        }
    }

    this.delete_association = function(id) {
    }

    this.set_topic_selection = function(topic) {
        this.selected_object_id = topic.id
    }

    this.set_association_selection = function(assoc) {
    }

    this.reset_selection = function() {
        this.selected_object_id = -1
    }

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
                topics[topic.id] = new GeomapTopic(topic.id, pos.x, pos.y)
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

    function GeomapTopic(id, x, y) {

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
