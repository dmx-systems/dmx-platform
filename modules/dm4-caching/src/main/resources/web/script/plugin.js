dm4c.add_plugin("de.deepamehta.caching", function() {

    var CACHABLE_PATH = /^\/core\/(topic|association)\/(\d+)(\?.*)?$/
    var PROP_URI_MODIFIED = "dm4.time.modified"

    // === Webclient Listeners ===

    dm4c.add_listener("pre_send_request", function(request) {
        if (request.method == "PUT") {
            var object_id = request_object_id()
            if (object_id != -1) {
                var modified = request_object_timestamp()
                if (!modified) {
                    // possible fallback: get the timestamp from the object displayed in the page panel
                    //
                    // Note: we must consider the page panel's object -- not the object selected on the topicmap
                    // (dm4c.selected_object). Both may be different (namely when Geomap renderer is active),
                    // but the PUT request always relates to the object displayed in the page panel.
                    modified = page_panel_object_timestamp()
                    //
                    if (!modified) {
                        throw "CachingError: modification timestamp missing while preparing conditional PUT request " +
                            "(uri=\"" + request.uri + "\", data=" + JSON.stringify(request.data) +
                            ", page panel object=" + dm4c.page_panel.get_displayed_object().id + ")"
                    }
                }
                //
                var modified_utc = new Date(modified).toUTCString()
                console.log("PUT object " + object_id + ", modified=" + modified + " (" + modified_utc + ")")
                //
                request.headers["If-Unmodified-Since"] = modified_utc
            }
        }

        function request_object_id() {
            var groups = request.uri.match(CACHABLE_PATH)
            return groups ? groups[2] : -1
        }

        function request_object_timestamp() {
            var comp = request.data.composite
            return comp && comp[PROP_URI_MODIFIED] && comp[PROP_URI_MODIFIED].value
            // ### TODO: request.data.get(PROP_URI_MODIFIED) would be more comfortable
            // ### but is not supported for non-model values. See Topic.prototype.get()
        }

        function page_panel_object_timestamp() {
            var displayed_object = dm4c.page_panel.get_displayed_object()
            if (object_id == displayed_object.id) {
                return displayed_object.composite[PROP_URI_MODIFIED].value
            }
        }
    })
})
