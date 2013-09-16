function OpenLayersView(config) {

    // style preferences
    var default_style = {
        fillColor: "#ff00ff",
        fillOpacity: 0.5,
        strokeColor: "#000000",
        strokeOpacity: 0.5,
        strokeWidth: 2,
        pointRadius: 8
    }
    var select_style = {
        fillOpacity: 1,
        strokeOpacity: 0.3,
        strokeWidth: 10,
        pointRadius: 12
    }

    var map                     // OpenLayers.Map object
    var feature_layers = {}     // Key: layer name, value: FeatureLayer object

    var map_projection          // OpenStreetMap projection is EPSG:900913
    var lonlat_projection = new OpenLayers.Projection("EPSG:4326")  // EPSG:4326 is lon/lat projection

    init()

    // ------------------------------------------------------------------------------------------------------ Public API

    // === The Map ===

    this.render = function(container) {
        map.render(container)
    }

    this.set_center = function(center, zoom) {
        map.setCenter(transform_to_map(center.lon, center.lat), zoom)
    }

    this.update_size = function() {
        map.updateSize()
    }

    // === Features ===

    this.add_feature = function(geo_facet, do_select) {
        feature_layers["features"].add_feature({lon: geo_facet.x, lat: geo_facet.y}, geo_facet, do_select)
    }

    this.select_feature = function(geo_facet_id) {
        feature_layers["features"].select_feature(geo_facet_id)
    }

    this.remove_all_features = function() {
        feature_layers["features"].remove_all_features()
    }



    // ----------------------------------------------------------------------------------------------- Private Functions

    function init() {
        OpenLayers.ImgPath = "/de.deepamehta.geomaps/script/vendor/openlayers/img/"
        //
        map = new OpenLayers.Map({
            controls: []
        })
        map.addLayers([
            new OpenLayers.Layer.OSM("OpenSteetMap")
        ])
        map.addControl(new OpenLayers.Control.Navigation({'zoomWheelEnabled': false}))
        map.addControl(new OpenLayers.Control.ZoomPanel())
        map.events.register("moveend", undefined, on_move)
        map_projection = map.getProjectionObject()
        feature_layers["features"] = new FeatureLayer("features")

        function on_move() {
            var center = map.getCenter()
            config.move_handler(transform_to_lonlat(center.lon, center.lat), map.getZoom())
        }
    }

    // ---

    /**
     * Transforms lon/lat coordinates according to this map's projection.
     *
     * @param   lon     (float)
     * @param   lat     (float)
     *
     * @return  an OpenLayers.LonLat object
     */
    function transform_to_map(lon, lat) {
        return new OpenLayers.LonLat(lon, lat).transform(lonlat_projection, map_projection)
    }

    function transform_to_lonlat(lon, lat) {
        return new OpenLayers.LonLat(lon, lat).transform(map_projection, lonlat_projection)
    }



    // ------------------------------------------------------------------------------------------------- Private Classes

    /**
     * Wraps an OpenLayers vector layer and binds features to topics. Provides two methods:
     *     - add_feature(pos, topic)
     *     - remove_feature(topic_id)
     */
    function FeatureLayer(layer_name) {
        var features = {}   // holds the OpenLayers.Feature.Vector objects, keyed by topic ID
        var style_map = new OpenLayers.StyleMap({
            default: default_style,
            select: select_style
        })
        // create vector layer
        var vector_layer = new OpenLayers.Layer.Vector(layer_name, {
            styleMap: style_map
        })
        map.addLayer(vector_layer)
        bind_select_handler()
        // add SelectFeature control
        var select_control = new OpenLayers.Control.SelectFeature(vector_layer, {
            clickout: false
        })
        map.addControl(select_control)
        select_control.activate()

        // === Public API ===

        this.add_feature = function(pos, topic, do_select) {
            // remove feature if already on the map
            if (features[topic.id]) {
                vector_layer.removeFeatures([features[topic.id]])
            }
            // create feature
            var p = transform_to_map(pos.lon, pos.lat)
            var geometry = new OpenLayers.Geometry.Point(p.lon, p.lat)
            var feature = new OpenLayers.Feature.Vector(geometry, {topic_id: topic.id})
            features[topic.id] = feature
            vector_layer.addFeatures([feature])
            //
            if (do_select) {
                this.select_feature(topic.id)
            }
        }

        this.select_feature = function(topic_id) {
            var feature = features[topic_id]
            if (feature) {
                unbind_select_handler()
                //
                select_control.clickFeature(feature)
                //
                bind_select_handler()
                scroll_to_center(feature)
            } else {
                // ### alert("FeatureLayer: there is no feature for topic " + topic_id)
            }
        }

        this.remove_feature = function(topic_id) {
            vector_layer.removeFeatures([features[topic_id]])
            // ### TODO: delete from features object
        }

        this.remove_all_features = function() {
            vector_layer.removeAllFeatures()
        }

        // ===

        function bind_select_handler() {
            vector_layer.events.on({"featureselected": do_select_feature})
        }

        function unbind_select_handler() {
            vector_layer.events.un({"featureselected": do_select_feature})
        }

        // ---

        function do_select_feature(event) {
            dm4c.do_select_topic(event.feature.attributes.topic_id)
        }

        function scroll_to_center(feature) {
            var p = new OpenLayers.LonLat(feature.geometry.x, feature.geometry.y)
            if (!map.getExtent().containsLonLat(p)) {
                map.setCenter(p)
            }
        }
    }
}
