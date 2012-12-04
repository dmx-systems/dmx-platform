/**
 * A topicmap renderer that displays a geo map in the background. The rendering is based on OpenLayers library.
 *
 * OpenLayers specifics are encapsulated. The caller must not know about OpenLayers API usage.
 */
function GeomapRenderer() {

    // ------------------------------------------------------------------------------------------------ Constructor Code

    js.extend(this, TopicmapRenderer)

    this.dom = $("<div>", {id: "canvas"})

    var ol_view = new OpenLayersView({move_handler: on_move})

    var LOG_GEOMAPS = false

    // ------------------------------------------------------------------------------------------------------ Public API

    // === TopicmapRenderer Implementation ===

    this.get_info = function() {
        return {
            uri: "dm4.geomaps.geomap_renderer",
            name: "Geomap"
        }
    }

    this.add_topic = function(topic, do_select) {
        var topic_shown = undefined
        //
        var geo_facet = dm4c.get_plugin("de.deepamehta.geomaps").get_geo_facet(topic)
        if (geo_facet) {
            // update view
            ol_view.add_feature(geo_facet, do_select)
            //
            topic_shown = geo_facet
        }
        //
        return topic_shown
    }

    this.update_topic = function(topic, refresh_canvas) {
        // ### Compare to add_topic() above. Can we call it from here?
        // ### FIXME: or can we call dm4c.show_topic() here?
        var geo_facet = dm4c.get_plugin("de.deepamehta.geomaps").get_geo_facet(topic)
        if (geo_facet) {
            // update model
            get_geomap().add_topic(geo_facet.id, geo_facet.type_uri, "", geo_facet.x, geo_facet.y)
            // update view
            ol_view.add_feature(geo_facet, true)    // do_select=true
        }
    }

    this.clear = function() {
        ol_view.remove_all_features()
    }

    this.select_topic = function(topic_id) {
        // fetch from DB
        var topic_select = dm4c.fetch_topic(topic_id)
        var topic_display = new Topic(dm4c.restc.get_geotopic(topic_id))
        // update view
        ol_view.select_feature(topic_id)
        //
        return {
            select: topic_select,
            display: topic_display
        }
    }

    // === TopicmapRenderer Topicmaps Extension ===

    this.load_topicmap = function(topicmap_id, config) {
        return new Geomap(topicmap_id, config)
    }

    this.display_topicmap = function(topicmap, no_history_update) {
        dm4c.canvas.clear()
        ol_view.set_center(topicmap.center, topicmap.zoom)
        display_topics()
        restore_selection()

        function display_topics() {
            topicmap.iterate_topics(function(topic) {
                ol_view.add_feature(topic)
            })
        }

        function restore_selection() {
            var id = topicmap.selected_object_id
            if (id != -1) {
                dm4c.do_select_topic(id, no_history_update)
            } else {
                dm4c.do_reset_selection(no_history_update)
            }
        }
    }

    // === Left SplitPanel Component Implementation ===

    this.init = function() {
        ol_view.render("canvas")
    }

    this.resize = function(size) {
        if (dm4c.LOG_GUI) dm4c.log("Resizing geomap to " + size.width + "x" + size.height)
        this.dom.width(size.width).height(size.height)
    }

    this.resize_end = function() {
        ol_view.update_size()
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    function get_geomap() {
        return dm4c.get_plugin("de.deepamehta.topicmaps").get_topicmap()
    }

    // === Event Handler ===

    function on_move(center, zoom) {
        get_geomap().set_state(center, zoom)
    }
}

// ------------------------------------------------------------------------------------------------------ Static Methods

// ### FIXME: revise the Geomap model class and make this local functions

GeomapRenderer.position = function(geo_facet) {
    return {
        x: geo_facet.get("dm4.geomaps.longitude"),
        y: geo_facet.get("dm4.geomaps.latitude")
    }
}
