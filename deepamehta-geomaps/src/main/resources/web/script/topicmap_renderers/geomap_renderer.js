/**
 * A topicmap renderer that displays a geo map in the background. The rendering is based on OpenLayers library.
 *
 * OpenLayers specifics are encapsulated. The caller must not know about OpenLayers API usage.
 */
function GeoMapRenderer() {

    // ------------------------------------------------------------------------------------------------ Constructor Code

    var LOG_GEOMAPS = false

    js.extend(this, TopicmapRenderer)

    this.dom = $("<div>", {id: "canvas"})

    var map                     // OpenLayers.Map object
    var marker_layers = {}      // Key: layer name, value: MarkerLayer object

    // ------------------------------------------------------------------------------------------------------ Public API

    // === TopicmapRenderer Implementation ===

    this.get_info = function() {
        return {
            uri: "dm4.geomaps.geomap_renderer",
            name: "Geomap"
        }
    }

    this.init = function() {
        this.resize(dm4c.split_panel.get_left_panel_size())     // we must already in DOM
        init()                                                  // we must already in DOM
    }

    this.resize = function(size) {
        if (dm4c.LOG_GUI) dm4c.log("Resizing geomap canvas to " + size.width + "x" + size.height)
        this.dom.width(size.width).height(size.height)
    }

    this.add_topic = function(topic, do_select) {
        if (topic.x != undefined && topic.y != undefined) {
            if (LOG_GEOMAPS) dm4c.log("GeoMapRenderer.add_topic(): displaying marker at x=" +
                topic.x + ", y=" + topic.y + "\n..... Topic=" + JSON.stringify(topic))
            marker_layers["markers"].add_marker({lon: topic.x, lat: topic.y}, topic)
        } else {
            if (LOG_GEOMAPS) dm4c.log("GeoMapRenderer.add_topic(): displaying marker ABORTED -- " +
                "topic has no coordinates\n..... Topic=" + JSON.stringify(topic))
        }
    }

    this.select_topic = function(topic_id) {
        var topic = dm4c.fetch_topic(topic_id)
        // ### set_highlight_object(topic_id)
        return {select: topic, display: topic}
    }

    // === TopicmapRenderer Topicmaps Extension ===

    this.load_topicmap = function(topicmap_id) {
        return new Geomap(topicmap_id)
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    function init() {
        OpenLayers.ImgPath = "/de.deepamehta.geomaps/script/vendor/openlayers/img/"
        //
        map = new OpenLayers.Map("canvas", {
            controls: []
        })
        map.addLayers([
            new OpenLayers.Layer.OSM("OpenSteetMap")/*,
            new OpenLayers.Layer.Google("Google Maps")*/
        ])
        map.addControl(new OpenLayers.Control.Navigation({'zoomWheelEnabled': false}))
        map.addControl(new OpenLayers.Control.ZoomPanel())
        // map.addControl(new OpenLayers.Control.LayerSwitcher())
        map.setCenter(transform(11, 51), 6)
        //
        // for (var i = 0, ml; ml = marker_layer_info[i]; i++) {
        marker_layers["markers"] = new MarkerLayer("markers", "marker.png")
        // }

        // === Public API ===

        /*this.geocode = function() {
            var geocoder = new google.maps.Geocoder()
            return function(address, callback) {
                geocoder.geocode({address: address}, callback)
            }
        }()

        this.set_center = function(pos) {
            map.setCenter(transform(pos.lon, pos.lat))
        }*/
    }

    /**
     * Transforms lon/lat coordinates according to this map's projection.
     */
    var transform = function() {
        var projection = new OpenLayers.Projection("EPSG:4326")     // EPSG:4326 is *lon/lat* projection
        return function(lon, lat) {
            return new OpenLayers.LonLat(lon, lat).transform(
                projection, map.getProjectionObject()
            )
        }
    }()

    // ------------------------------------------------------------------------------------------------- Private Classes

    /**
     * Wraps an OpenLayers markers layer and binds markers to topics. Provides two methods:
     *     - add_marker(pos, topic)
     *     - remove_marker(topic_id)
     */
    function MarkerLayer(layer_name, icon_file) {
        var self = this
        var markers = {}    // holds the OpenLayers.Marker objects, keyed by topic ID
        var markers_layer = new OpenLayers.Layer.Markers(layer_name)
        map.addLayer(markers_layer)

        // === Public API ===

        this.add_marker = function() {
            var size = new OpenLayers.Size(21, 25)
            var offset = new OpenLayers.Pixel(-size.w / 2, -size.h)
            var icon = new OpenLayers.Icon('/de.deepamehta.geomaps/script/vendor/openlayers/img/' +
                icon_file, size, offset)

            return function(pos, topic) {
                // if the marker is already on the map, remove it
                if (markers[topic.id]) {
                    markers_layer.removeMarker(markers[topic.id])
                }
                // Note: you should not share icons between markers. Clone them instead.
                var marker = new OpenLayers.Marker(transform(pos.lon, pos.lat), icon.clone())
                marker.events.register("click", topic, marker_clicked)
                markers[topic.id] = marker
                markers_layer.addMarker(marker)

                function marker_clicked() {
                    dm4c.do_select_topic(this.id)
                }
            }
        }()

        this.remove_marker = function(topic_id) {
            markers_layer.removeMarker(markers[topic_id])
        }
    }

    /**
     * A topicmap model that is attached to the database.
     *
     * ### FIXME: introduce common base class for Geomap and Topicmap (see deepamehta-topicmaps module)
     */
    function Geomap(topicmap_id) {

        // Model
        var info                        // The underlying Topicmap topic (a Topic object)
        var topics = {}                 // topics of this topicmap (key: topic ID, value: GeomapTopic object)
        var selected_object_id = -1     // ID of the selected topic or association, or -1 for no selection

        load()

        // --- Public API ---

        // === Topicmap Implementation ===

        this.get_id = function() {
            return topicmap_id
        }

        this.get_renderer_uri = function() {
            return info.get("dm4.topicmaps.topicmap_renderer_uri")
        }

        this.put_on_canvas = function(no_history_update) {
            display_topics()
            restore_selection()

            function display_topics() {
                for (var id in topics) {
                    dm4c.canvas.add_topic(topics[id])
                }
            }

            function restore_selection() {
                dm4c.do_reset_selection(no_history_update)
            }
        }

        this.add_topic = function(id, type_uri, value, x, y) {
            if (x != undefined && y != undefined) {
                if (LOG_GEOMAPS) dm4c.log("Geomap.add_topic(): adding topic to model\n..... id=" + id +
                    ", type_uri=\"" + type_uri + "\", x=" + x + ", y=" + y)
                // update DB
                dm4c.restc.add_topic_to_geomap(topicmap_id, id)     // ### FIXME: add only once
                // update memory
                topics[id] = new GeomapTopic(id, type_uri, value, x, y)
            } else {
                if (LOG_GEOMAPS) dm4c.log("Geomap.add_topic(): adding topic to model ABORTED -- topic has " +
                    "no coordinates\n..... id=" + id + ", type_uri=\"" + type_uri + "\", value=\"" + value + "\"")
            }
        }

        this.add_association = function(id, type_uri, topic_id_1, topic_id_2) {
        }

        this.update_topic = function(topic) {
            // ### Compare to prepare_topic_for_display()
            // ### FIXME: can we use dm4c.show_topic() here?
            if (LOG_GEOMAPS) dm4c.log("Geomap.update_topic(): topic=" + JSON.stringify(topic))
            var address = topic.find_child_topic("dm4.contacts.address")
            if (address) {
                var geo_facet = get_geo_facet(address)
                if (geo_facet) {
                    // update model
                    this.add_topic(geo_facet.id, geo_facet.type_uri, "", geo_facet.x, geo_facet.y)
                    // update view
                    dm4c.canvas.add_topic(geo_facet)
                }
            }
        }

        this.delete_topic = function(id) {
        }

        this.delete_association = function(id) {
        }

        this.set_topic_selection = function(topic) {
            selected_object_id = topic.id
            is_topic_selected = true
        }

        this.set_association_selection = function(assoc) {
            selected_object_id = assoc.id
            is_topic_selected = false
        }

        this.reset_selection = function() {
            selected_object_id = -1
        }

        this.prepare_topic_for_display = function(topic) {
            var address = topic.find_child_topic("dm4.contacts.address")
            if (address) {
                var geo_facet = get_geo_facet(address)
                if (geo_facet) {
                    if (LOG_GEOMAPS) dm4c.log("Geomap.prepare_topic_for_display(): setting up proxy topic " +
                        "at x=" + geo_facet.x + ", y=" + geo_facet.y + "\n..... Original address topic=" +
                        JSON.stringify(address))
                    return geo_facet
                } else {
                    if (LOG_GEOMAPS) dm4c.log("Geomap.prepare_topic_for_display(): setting up proxy topic " +
                        "ABORTED -- address has no geo facet\n..... Address topic=" + JSON.stringify(address))
                }
            } else {
                if (LOG_GEOMAPS) dm4c.log("Geomap.prepare_topic_for_display(): setting up proxy topic " +
                    "ABORTED -- topic has no address child\n..... Topic=" + JSON.stringify(topic))
            }
        }

        // ===

        // --- Private Functions ---

        function load() {
            var topicmap = dm4c.restc.get_geomap(topicmap_id)
            info = new Topic(topicmap.info)
            //
            load_topics()

            function load_topics() {
                for (var i = 0, topic; topic = topicmap.topics[i]; i++) {
                    var pos = position(topic)
                    topics[topic.id] = new GeomapTopic(topic.id, topic.type_uri, topic.value, pos.x, pos.y)
                }
            }
        }

        /**
         * Returns the geo facet of an address.
         *
         * @param   address     An "Address" topic (a JavaScript object).
         *
         * @return  A "Geo Coordinate" topic extended with "x" and "y" properties (a Topic object).
         */
        function get_geo_facet(address) {
            var geo_facet = address.composite["dm4.geomaps.geo_coordinate"]
            if (geo_facet) {
                var pos = position(geo_facet)
                geo_facet = new Topic(geo_facet)
                geo_facet.x = pos.x
                geo_facet.y = pos.y
                return geo_facet
            }
        }

        function position(geo_facet) {
            return {
                x: geo_facet.composite["dm4.geomaps.longitude"].value,
                y: geo_facet.composite["dm4.geomaps.latitude"].value
            }
        }

        // --- Private Classes ---

        function GeomapTopic(id, type_uri, value, x, y) {
            this.id = id
            this.type_uri = type_uri
            this.value = value
            this.x = x
            this.y = y
        }
    }
}
