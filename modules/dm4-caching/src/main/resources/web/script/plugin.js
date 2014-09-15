dm4c.add_plugin("de.deepamehta.caching", function() {

    var CACHABLE_PATH = /^\/core\/(topic|association)\/(\d+)(\?.*)?$/
    var PROP_URI_MODIFIED = "dm4.time.modified"

    // === Webclient Listeners ===

    dm4c.add_listener("pre_send_request", function(request) {
        if (request.method == "PUT") {
            var object_id = request_object_id()
            if (object_id != -1) {
                var modified = request_object_timestamp()
                // Note: 0 is a perfect timestamp. It is send by the server as the default timestamp for objects which
                // don't have a timestamp. This happens e.g. when upgrading from a DM 4.1 installation. Timestamps were
                // introduced only in DM 4.1.1. So we must compare to undefined here. !modified would not work.
                if (modified == undefined) {
                    // possible fallback: get the timestamp from the object displayed in the page panel
                    //
                    // Note: we must consider the page panel's object -- not the object selected on the topicmap
                    // (dm4c.selected_object). Both may be different (namely when Geomap renderer is active),
                    // but the PUT request always relates to the object displayed in the page panel.
                    modified = page_panel_object_timestamp()
                    //
                    if (modified == undefined) {
                        var displayed_object = dm4c.page_panel.get_displayed_object()
                        throw "CachingError: modification timestamp missing while preparing conditional PUT request " +
                            "(uri=\"" + request.uri + "\", data=" + JSON.stringify(request.data) +
                            ", page panel object=" + (displayed_object && displayed_object.id) + ")"
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
            var comp = request.data.childs
            return comp && comp[PROP_URI_MODIFIED] && comp[PROP_URI_MODIFIED].value
            // ### TODO: request.data.get(PROP_URI_MODIFIED) would be more comfortable
            // ### but is not supported for non-model values. See Topic.prototype.get()
        }

        function page_panel_object_timestamp() {
            var displayed_object = dm4c.page_panel.get_displayed_object()
            if (displayed_object && displayed_object.id == object_id) {
                return displayed_object.childs[PROP_URI_MODIFIED].value
            }
        }
    })
})
