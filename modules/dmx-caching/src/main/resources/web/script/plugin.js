dm4c.add_plugin("systems.dmx.caching", function() {

    var CACHABLE_PATH = /^\/core\/(topic|association)\/(\d+)(\?.*)?$/
    var PROP_MODIFIED = "dmx.time.modified"

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
            var childs = request.data.childs
            return childs && childs[PROP_MODIFIED] && childs[PROP_MODIFIED].value
            // ### TODO: request.data.get(PROP_MODIFIED) would be more comfortable
            // ### but is not supported for non-model values. See Topic.prototype.get()
        }

        function page_panel_object_timestamp() {
            var displayed_object = dm4c.page_panel.get_displayed_object()
            if (!displayed_object) {
                throw "CachingError: modification timestamp missing while preparing conditional PUT request " +
                    "(nothing displayed in page panel)"
            }
            if (displayed_object.id != object_id) {
                throw "CachingError: modification timestamp missing while preparing conditional PUT request " +
                    "(uri=\"" + request.uri + "\" while page panel displays object " + displayed_object.id + ")"
            }
            var modified = displayed_object.childs[PROP_MODIFIED]
            if (!modified) {
                throw "CachingError: can't prepare conditional PUT request (object " + object_id +
                    " has no modification timestamp)"
            }
            return modified.value
        }
    })
})
