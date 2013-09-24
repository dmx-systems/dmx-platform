/**
 * A geomap viewmodel that is attached to the database. There are methods for:
 *  - loading a geomap from DB
 *  - manipulating the geomap by e.g. adding/removing topics
 *
 * ### TODO: introduce common base class for GeomapViewmodel and TopicmapViewmodel (see dm4-topicmaps module)
 */
function GeomapViewmodel(topicmap_id, config) {

    var self = this

    // Viewmodel
    var info                        // The underlying Topicmap topic (a Topic object)
    var topics = {}                 // topics of this topicmap (key: topic ID, value: GeoTopicViewmodel object)
    this.center                     // map center (an OpenLayers.LonLat object in lon/lat projection)
    this.zoom                       // zoom level (integer)
    this.selected_object_id = -1    // ID of the selected topic, or -1 for no selection

    load()

    // ------------------------------------------------------------------------------------------------------ Public API

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

    /**
     * @param   geo_coordinate      a Topic object of type dm4.geomaps.geo_coordinate
     */
    this.add_topic = function(geo_coordinate) {
        if (!topics[geo_coordinate.id]) {
            // update memory and DB
            return add_geo_coordinate(geo_coordinate)
        }
    }

    // ---

    /**
     * @param   geo_coordinate      a Topic object of type dm4.geomaps.geo_coordinate (### not necessarily)
     */
    this.update_topic = function(topic) {           // Note: called from CanvasRenderer ### TODO: rethink
        var t = topics[topic.id]
        if (t) {
            // update memory
            t.update(topic)
            // Note: no DB update here. A topic update doesn't affect the view data.
            return t
        } else {
            // A topic hits the geomap through an update operation. The topic got its geo coordinate only through an
            // update operation. So it is placed in the geomap for the first time and needs to be selected.
            // Note: this is different from the standard topicmap behavoir. Its viewmodel is not affected at all if the
            // updated topic is not contained in the topicmap. A topic never hits a standard topicmap through an update
            // operation.
            //
            // update memory
            this.set_topic_selection(topic.id)
            // update memory and DB
            return add_geo_coordinate(topic)
        }
    }

    this.update_association = function(assoc) {     // Note: called from CanvasRenderer ### TODO: rethink
    }

    // ---

    this.delete_topic = function(id) {              // Note: called from CanvasRenderer ### TODO: rethink
        var topic = topics[id]
        if (topic) {
            // update memory
            topic.remove()
            // Note: no DB update here. The "Geotopic Mapcontext" association is already deleted.
        }
    }

    this.delete_association = function(id) {        // Note: called from CanvasRenderer ### TODO: rethink
    }

    // ---

    this.set_topic_selection = function(topic_id) {
        // update memory
        this.selected_object_id = topic_id
        // Note: no DB update here. The selection is not yet persisted.
    }

    this.reset_selection = function() {
        // update memory
        this.selected_object_id = -1
        // Note: no DB update here. The selection is not yet persisted.
    }

    // ---

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
                topics[topic.id] = new GeoTopicViewmodel(new Topic(topic))
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

    function add_geo_coordinate(geo_coordinate) {
        // update memory
        geo_topic = new GeoTopicViewmodel(geo_coordinate)
        topics[geo_coordinate.id] = geo_topic
        // update DB
        if (is_writable()) {
            dm4c.restc.add_topic_to_geomap(topicmap_id, geo_coordinate.id)
        }
        //
        return geo_topic
    }

    // ---

    function is_writable() {
        return config.is_writable
    }



    // ------------------------------------------------------------------------------------------------- Private Classes

    /**
     * Properties:
     *  id,
     *  lon, lat
     *
     * @param   geo_coordinate     a Topic object of type dm4.geomaps.geo_coordinate
     */
    function GeoTopicViewmodel(geo_coordinate) {

        var _self = this

        this.id = geo_coordinate.id
        init(geo_coordinate)

        /**
         * @param   geo_coordinate     a Topic object of type dm4.geomaps.geo_coordinate
         */
        this.update = function(geo_coordinate) {
            // Note: for a geo topic an update request might result in a geometry change (in case the Address changes).
            // (not so for non-geo topics).
            init(geo_coordinate)
        }

        this.remove = function() {
            delete topics[id]
            reset_selection()
        }

        // ---

        function reset_selection() {
            if (self.selected_object_id == _self.id) {
                self.reset_selection()
            }
        }

        // ---

        function init(geo_coordinate) {
            _self.lon = geo_coordinate.get("dm4.geomaps.longitude")
            _self.lat = geo_coordinate.get("dm4.geomaps.latitude")
        }
    }
}
