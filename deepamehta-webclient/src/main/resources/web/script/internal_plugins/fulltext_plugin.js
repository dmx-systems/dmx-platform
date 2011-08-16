/**
 * Provides the "By Text" search mode.
 */
function fulltext_plugin() {

    var SEARCH_FIELD_WIDTH = 20    // in chars
    var search_field



    // ***********************************************************
    // *** Webclient Hooks (triggered by deepamehta-webclient) ***
    // ***********************************************************



    this.init = function() {
        dm4c.toolbar.searchmode_menu.add_item({label: "By Text", value: "by-text"})
        dm4c.toolbar.search_button.button("disable")
        search_field = $('<input type="text">').attr("size", SEARCH_FIELD_WIDTH)
        // select initial searchmode
        dm4c.toolbar.select_searchmode("by-text")
    }

    this.searchmode_widget = function(searchmode) {
        if (searchmode == "by-text") {
            return search_field.keyup(do_process_key)
        }
    }

    this.search = function(searchmode) {
        if (searchmode == "by-text") {
            return dm4c.restc.search_topics_and_create_bucket(get_searchterm())
        }
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    // === Event Handler ===

    function do_process_key(event) {
        var searchterm = get_searchterm()
        if (event.which == 13 && searchterm) {
            dm4c.do_search("by-text")
        } else {
            dm4c.toolbar.search_button.button("option", "disabled", !searchterm)
        }
    }

    // === Helper ===

    function get_searchterm() {
        return $.trim(search_field.val())
    }
}
