/**
 * Provides the "By Type" searchmode.
 */
dm4c.add_plugin("systems.dmx.typesearch", function() {

    var SEARCHMODE_URI = "dm4.typesearch.by_type"

    var type_menu

    dm4c.toolbar.add_searchmode(SEARCHMODE_URI, function() {

        this.label = "By Type"

        this.widget = function() {
            dm4c.toolbar.enable_search_button(true)
            // Note: once an element is detached from document it looses its event handlers.
            // So we recreate the menu here.
            type_menu = dm4c.ui.menu(dm4c.toolbar.do_search)
            refresh_type_menu()
            //
            return type_menu.dom
        }

        this.search = function() {
            var type_uri = type_menu.get_selection().value
            return dm4c.restc.get_topics_and_create_bucket(type_uri)
        }
    })



    // === Webclient Listeners ===

    /**
     * Refresh the type menu once a topic type is created.
     */
    dm4c.add_listener("post_create_topic", function(topic) {
        if (topic.type_uri == "dm4.core.topic_type") {
            refresh_type_menu()
        }
    })

    /**
     * Refresh the type menu once a topic type is updated.
     */
    dm4c.add_listener("post_update_topic", function(topic) {
        if (topic.type_uri == "dm4.core.topic_type") {
            refresh_type_menu()
        }
    })

    /**
     * Refresh the type menu once a topic type is deleted.
     */
    dm4c.add_listener("post_delete_topic_type", function(type_uri) {
        refresh_type_menu()
    })

    // ----------------------------------------------------------------------------------------------- Private Functions

    function refresh_type_menu() {
        // Note: refreshing the type menu is only required if our searchmode is selected
        if (dm4c.toolbar.get_searchmode_uri() == SEARCHMODE_URI) {
            dm4c.refresh_type_menu(type_menu)
        }
    }
})
