/**
 * A topicmap renderer that displays a geo map in the background. The rendering is based on OpenLayers library.
 *
 * OpenLayers specifics are encapsulated. The caller must not know about OpenLayers API usage.
 */
function GeoMapRenderer() {

    // ------------------------------------------------------------------------------------------------ Constructor Code

    this.superclass = TopicmapRenderer
    this.superclass()

    this.dom = $("<div>", {id: "canvas"}).text("Hello Geomap!")
    // this.resize(dm4c.split_panel.get_left_panel_size())

    // ------------------------------------------------------------------------------------------------------ Public API

    // === Overriding TopicmapRenderer Adapter Methods ===

    this.get_info = function() {
        return {
            uri: "dm4.topicmap_renderer.geomap",
            name: "Geomap"
        }
    }

    this.resize = function(size) {
        if (dm4c.LOG_GUI) dm4c.log("Resizing geomap canvas to " + size.width + "x" + size.height)
        this.dom.width(size.width).height(size.height)
    }
}
