/**
 * A topicmap renderer that displays a geo map in the background. The rendering is based on OpenLayers library.
 *
 * OpenLayers specifics are encapsulated. The caller must not know about OpenLayers API usage.
 */
function GeoMapRenderer() {

    // ------------------------------------------------------------------------------------------------ Constructor Code

    this.superclass = TopicmapRenderer
    this.superclass()

    this.dom = $("<div>", {id: "canvas"})

    var map

    // ------------------------------------------------------------------------------------------------------ Public API

    // === Overriding TopicmapRenderer Adapter Methods ===

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

    this.load_topicmap = function(topicmap_id) {
        return new Geomap(topicmap_id)
    }

    this.resize = function(size) {
        if (dm4c.LOG_GUI) dm4c.log("Resizing geomap canvas to " + size.width + "x" + size.height)
        this.dom.width(size.width).height(size.height)
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
     * An in-memory representation (model) of a persistent topicmap. There are methods for:
     *  - building the in-memory representation by loading a topicmap from DB.
     *  - manipulating the topicmap by e.g. adding/removing topics and associations,
     *    while synchronizing the DB accordingly.
     *  - putting the topicmap on the canvas.
     *
     * ### FIXME: introduce common base class for Topicmap and Geomap
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
