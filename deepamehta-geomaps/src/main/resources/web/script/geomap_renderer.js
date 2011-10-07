/**
 * A topicmap renderer that displays a geo map in the background. The rendering is based on OpenLayers library.
 *
 * OpenLayers specifics are encapsulated. The caller must not know about OpenLayers API usage.
 */
function GeoMapRenderer() {

    // ------------------------------------------------------------------------------------------------ Constructor Code

    this.superclass = TopicmapRenderer
    this.superclass()

    // ------------------------------------------------------------------------------------------------------ Public API

    // === Overriding TopicmapRenderer Adapter Methods ===

    this.get_info = function() {
        return {
            uri: "dm4.topicmap_renderer.geomap",
            name: "Geomap"
        }
    }
}
