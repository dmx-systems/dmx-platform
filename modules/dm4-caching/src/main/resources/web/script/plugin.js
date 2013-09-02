dm4c.add_plugin("de.deepamehta.caching", function() {

    var cachable_path = /^\/core\/(topic|association)\/(\d+)(\?.*)?$/

    // === Webclient Listeners ===

    dm4c.add_listener("pre_send_request", function(request) {
        if (request.method == "PUT") {
            var object_id = get_object_id(request.uri)
            if (object_id != -1) {
                // Note: we must consider the modification timestamp of the topic displayed in the page panel -- not of
                // the topic selected on the topicmap (dm4c.selected_object). Both may be different (namely when Geomap
                // renderer is active), but the PUT request always relates to the topic displayed in the page panel.
                var displayed_object = dm4c.page_panel.get_displayed_object()
                var modified = displayed_object.composite["dm4.time.modified"].value
                // ### TODO: displayed_object.get("dm4.time.modified") would be more comfortable
                // ### but is not supported for non-model values. See Topic.prototype.get()
                var modified_utc = new Date(modified).toUTCString()
                console.log("PUT object " + object_id + ", displayed object ID=" + displayed_object.id +
                    ", modified=" + modified + " (" + modified_utc + ")")
                //
                if (object_id != displayed_object.id) {
                    throw "ID mismatch when preparing conditional PUT request (" + request.uri + "): " +
                        "the page panel displays object " + displayed_object.id
                }
                //
                request.headers["If-Unmodified-Since"] = modified_utc
            }
        }
    })

    // ----------------------------------------------------------------------------------------------- Private Functions

    function get_object_id(request_uri) {
        var groups = request_uri.match(cachable_path)
        return groups ? groups[2] : -1
    }
})
