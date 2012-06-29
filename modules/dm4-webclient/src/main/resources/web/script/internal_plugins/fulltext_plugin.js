/**
 * Provides the "By Text" search mode.
 */
dm4c.add_plugin("de.deepamehta.webclient.fulltext", function() {

    var SEARCH_FIELD_WIDTH = 20    // in chars
    var search_field

    // === Webclient Listeners ===

    dm4c.add_listener("init", function() {
        dm4c.toolbar.searchmode_menu.add_item({label: "By Text", value: "by-text"})
        search_field = $('<input type="text">').attr("size", SEARCH_FIELD_WIDTH)
        // select initial searchmode
        dm4c.toolbar.searchmode_menu.select("by-text")
        dm4c.toolbar.select_searchmode("by-text")
    })

    dm4c.add_listener("searchmode_widget", function(searchmode) {
        if (searchmode == "by-text") {
            update_search_button_state()
            return search_field.keyup(do_process_key)
        }
    })

    dm4c.add_listener("search", function(searchmode) {
        if (searchmode == "by-text") {
            return dm4c.restc.search_topics_and_create_bucket(get_searchterm())
        }
    })

    // ----------------------------------------------------------------------------------------------- Private Functions

    // === Event Handler ===

    function do_process_key(event) {
        if (event.which == 13 && get_searchterm()) {
            dm4c.do_search("by-text")
        } else {
            update_search_button_state()
        }
    }

    // === Helper ===

    function update_search_button_state() {
        dm4c.toolbar.search_button.button("option", "disabled", !get_searchterm())
    }

    function get_searchterm() {
        return $.trim(search_field.val())
    }
})
