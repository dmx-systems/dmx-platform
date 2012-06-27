dm4c.add_plugin("proxy_plugin", function() {

    // === REST Client Extension ===

    /**
     * @param   uri     Must not be URI-encoded!
     */
    dm4c.restc.get_resource = function(uri, type, size) {
        var params = this.createRequestParameter({type: type, size: size})
        return this.request("GET", "/proxy/" + encodeURIComponent(uri) + "?" + params.to_query_string())
    }

    /**
     * @param   uri     Must not be URI-encoded!
     */
    dm4c.restc.get_resource_info = function(uri) {
        return this.request("GET", "/proxy/" + encodeURIComponent(uri) + "/info")
    }
})
