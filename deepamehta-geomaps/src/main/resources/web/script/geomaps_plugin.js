function geomaps_plugin() {

    dm4c.javascript_source("/de.deepamehta.geomaps/script/geomap_renderer.js")



    // ***********************************************************
    // *** Topicmaps Hooks (triggered by deepamehta-topicmaps) ***
    // ***********************************************************



    this.topicmap_renderer = function() {
        return new GeoMapRenderer()
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

}
