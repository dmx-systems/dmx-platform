dm4c.add_plugin("de.deepamehta.time", function() {

    // === REST Client Extension ===

    dm4c.restc.get_creation_time = function(object_id) {
        return this.request("GET", "/time/object/" + object_id + "/created")
    }
    dm4c.restc.get_modification_time = function(object_id) {
        return this.request("GET", "/time/object/" + object_id + "/modified")
    }
})
