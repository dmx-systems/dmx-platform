dm4c.add_plugin("de.deepamehta.caching", function() {

    var cachable_path = /^\/core\/(topic|association)\/(\d+)(\?.*)?$/

    // === Webclient Listeners ===

    dm4c.add_listener("pre_send_request", function(request) {
        if (request.method == "PUT") {
            var groups = request.uri.match(cachable_path)
            if (groups) {
                var object_id = groups[2]
                var modified = dm4c.selected_object.composite["dm4.time.modified"].value
                // ### TODO: dm4c.selected_object.get("dm4.time.modified") would be more comfortable
                // ### but is not supported for non-model values. See Topic.prototype.get()
                console.log("PUT object " + object_id + ", ID=" + dm4c.selected_object.id +
                    ", modified=" + modified + " (" + new Date(modified).toUTCString() + ")")
                request.headers["If-Unmodified-Since"] = new Date(modified).toUTCString()
            }
        }
    })
})
