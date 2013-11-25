dm4c.add_plugin("de.deepamehta.caching", function() {

    var CACHABLE_PATH = /^\/core\/(topic|association)\/(\d+)(\?.*)?$/
    var PROP_URI_MODIFIED = "dm4.time.modified"

    // === Webclient Listeners ===

    dm4c.add_listener("pre_submit_form", function(object, new_object_model) {
        // Note: the update model is not required to have a "composite" property at all
        if (!new_object_model.composite) {
            new_object_model.composite = {}
        }
        // Note: the pre_send_request handler below expects a time modified *topic* object, not just a plain value.
        // So we construct the most rudimentary topic object here (with a sole "value" property).
        new_object_model.composite[PROP_URI_MODIFIED] = {value: object.composite[PROP_URI_MODIFIED].value}
    })

    dm4c.add_listener("pre_send_request", function(request) {
        if (request.method == "PUT") {
            var object_id = get_object_id(request.uri)
            if (object_id != -1) {
                var comp = request.data.composite
                var modified = comp && comp[PROP_URI_MODIFIED] && comp[PROP_URI_MODIFIED].value
                if (!modified) {
                    throw "CachingError: modification timestamp missing while preparing conditional PUT request " +
                        "(uri=\"" + request.uri + "\", data=" + JSON.stringify(request.data) + ")"
                }
                var modified_utc = new Date(modified).toUTCString()
                console.log("PUT object " + object_id + ", modified=" + modified + " (" + modified_utc + ")")
                //
                request.headers["If-Unmodified-Since"] = modified_utc
            }
        }
    })

    // ----------------------------------------------------------------------------------------------- Private Functions

    function get_object_id(request_uri) {
        var groups = request_uri.match(CACHABLE_PATH)
        return groups ? groups[2] : -1
    }
})
