/**
 * Provides the "By Type" search mode.
 */
function typesearch_plugin() {

    var type_menu



    // ***********************************************************
    // *** Webclient Hooks (triggered by deepamehta-webclient) ***
    // ***********************************************************



    this.init = function() {
        dm4c.toolbar.searchmode_menu.add_item({label: "By Type", value: "by-type"})
    }

    this.searchmode_widget = function(searchmode) {
        if (searchmode == "by-type") {
            // enable search button
            dm4c.toolbar.search_button.button("enable")
            // create type menu
            type_menu = dm4c.ui.menu(do_search_by_type)     // ### FIXME: had ID "search-type-menu", needed?
            refresh_type_menu()
            //
            return type_menu.dom
        }
    }

    this.search = function(searchmode) {
        if (searchmode == "by-type") {
            return dm4c.restc.get_topics_and_create_bucket(get_type_uri())
        }
    }

    /**
     * Once a "Topic Type" topic is updated we refresh the type menu.
     */
    this.post_update_topic = function(topic, old_topic) {
        if (topic.type_uri == "dm4.core.topic_type") {
            refresh_type_menu()
        }
    }

    /**
     * Once a "Topic Type" topic is deleted we refresh the type menu.
     */
    this.post_delete_topic = function(topic) {
        if (topic.type_uri == "dm4.core.topic_type") {
            refresh_type_menu()
        }
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    // === Event Handler ===

    function do_search_by_type() {
        dm4c.do_search("by-type")
    }

    // === Helper ===

    function refresh_type_menu() {
        // Note: refreshing the type menu is only required if the "By Type" searchmode is selected
        if (is_searchmode_selected()) {
            dm4c.refresh_type_menu(type_menu, function(topic_type) {
                return true     // all types are added
            })
        }
    }

    function is_searchmode_selected() {
        return dm4c.toolbar.searchmode_menu.get_selection().value == "by-type"
    }

    function get_type_uri() {
        return type_menu.get_selection().value
    }
}
