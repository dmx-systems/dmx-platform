/**
 * A topicmap renderer that displays a geo map in the background. The rendering is based on OpenLayers library.
 *
 * OpenLayers specifics are encapsulated. The caller must not know about OpenLayers API usage.
 */
function GeoMapRenderer() {

    // ------------------------------------------------------------------------------------------------ Constructor Code

    js.extend(this, TopicmapRenderer)

    this.dom = $("<div>", {id: "canvas"})

    var map

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

    // === TopicmapRenderer Topicmaps Extension ===

    this.load_topicmap = function(topicmap_id) {
        return new Geomap(topicmap_id)
    }

    this.prepare_topic_for_display = function(topicmap, topic) {
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
        /*for (var i = 0, ml; ml = marker_layer_info[i]; i++) {
            marker_layers[ml.name] = new MarkerLayer(ml.name, ml.icon_file, ml.type_uri)
        }*/

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
     * A topicmap model that is attached to the database.
     *
     * ### FIXME: introduce common base class for Geomap and Topicmap (see deepamehta-topicmaps module)
     */
    function Geomap(topicmap_id) {

        // Model
        var info                        // The underlying Topicmap topic (a JavaScript object)
        var selected_object_id = -1     // ID of the selected topic or association, or -1 for no selection

        load()

        // --- Public API ---

        this.get_id = function() {
            return topicmap_id
        }

        this.get_renderer_uri = function() {
            return info.composite["dm4.topicmaps.topicmap_renderer_uri"]
        }

        this.put_on_canvas = function(no_history_update) {
            dm4c.do_reset_selection(no_history_update)
        }

        this.add_topic = function(id, type_uri, label, x, y) {
        }

        this.add_association = function(id, type_uri, topic_id_1, topic_id_2) {
        }

        this.update_topic = function(topic) {
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

        // --- Private Functions ---

        function load() {
            var topicmap = dm4c.restc.get_topicmap(topicmap_id)
            info = topicmap.info
        }
    }
}
