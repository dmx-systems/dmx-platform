/**
 * A topicmap renderer that displays a geo map in the background. The rendering is based on OpenLayers library.
 *
 * OpenLayers specifics are encapsulated. The caller must not know about OpenLayers API usage.
 */
function GeomapRenderer() {

    // ------------------------------------------------------------------------------------------------ Constructor Code

    js.extend(this, TopicmapRenderer)

    this.dom = $("<div>").attr("id", "geomap-renderer")

    // View (OpenLayers based)
    var ol_view = new OpenLayersView({move_handler: on_move})

    // Viewmodel
    var geomap      // the geomap currently rendered (a GeomapViewmodel).
                    // Initialized by display_topicmap().

    // ------------------------------------------------------------------------------------------------------ Public API



    // === TopicmapRenderer Implementation ===

    this.get_info = function() {
        return {
            uri: "dm4.geomaps.geomap_renderer",
            name: "Geomap"
        }
    }

    // ---

    this.load_topicmap = function(topicmap_id, config) {
        return new GeomapViewmodel(topicmap_id, config)
    }

    this.display_topicmap = function(topicmap, no_history_update) {
        geomap = topicmap
        //
        ol_view.remove_all_features()
        ol_view.set_center(geomap.center, geomap.zoom)
        display_topics()
        restore_selection()

        function display_topics() {
            geomap.iterate_topics(function(topic) {
                ol_view.add_feature(topic)
            })
        }

        function restore_selection() {
            var id = geomap.selected_object_id
            if (id != -1) {
                dm4c.do_select_topic(id, no_history_update)
            } else {
                dm4c.do_reset_selection(no_history_update)
            }
        }
    }

    // ---

    this.show_topic = function(topic, do_select) {
        var geo_coordinate = dm4c.get_plugin("de.deepamehta.geomaps").get_geo_coordinate(topic)
        if (geo_coordinate) {
            // update viewmodel
            var geo_topic = geomap.add_topic(geo_coordinate)
            if (do_select) {
                geomap.set_topic_selection(geo_coordinate.id)
            }
            // update view
            if (geo_topic) {
                ol_view.add_feature(geo_topic, do_select)
            }
            //
            return geo_coordinate
        }
    }

    this.update_topic = function(topic) {
        var geo_coordinate = dm4c.get_plugin("de.deepamehta.geomaps").get_geo_coordinate(topic)
        if (geo_coordinate) {
            // update viewmodel
            var geo_topic = geomap.update_topic(geo_coordinate)
            // update view
            ol_view.add_feature(geo_topic, true)    // do_select=true
        }
    }

    this.select_topic = function(topic_id) {
        // fetch from DB
        var topic_select = dm4c.fetch_topic(topic_id)
        var topic_display = new Topic(dm4c.restc.get_geotopic(topic_id))
        // update viewmodel
        geomap.set_topic_selection(topic_id)
        // update view
        ol_view.select_feature(topic_id)
        //
        return {
            select: topic_select,
            display: topic_display
        }
    }



    // === Left SplitPanel Component Implementation ===

    this.init = function() {
        ol_view.render("geomap-renderer")
    }

    this.resize = function(size) {
        this.dom.width(size.width).height(size.height)
    }

    this.resize_end = function() {
        ol_view.update_size()
    }



    // ----------------------------------------------------------------------------------------------- Private Functions

    // === Event Handler ===

    function on_move(center, zoom) {
        geomap.set_state(center, zoom)
    }
}
