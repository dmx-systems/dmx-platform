/**
 * Provides the "By Text" searchmode.
 */
dm4c.add_plugin("de.deepamehta.webclient.fulltext", function() {

    var SEARCHMODE_URI = "dm4.webclient.by_text"

    dm4c.toolbar.add_searchmode(SEARCHMODE_URI, function() {

        this.label = "By Text"

        this.widget = function() {
            update_search_button_state()
            // Note: once an element is detached from document it looses its event handlers.
            // So we bind again here.
            return search_field.keyup(do_process_key)
        }

        this.search = function() {
            return dm4c.restc.search_topics_and_create_bucket(get_searchterm())
        }

        // ---

        var search_field = $('<input type="text">')

        function do_process_key(event) {
            if (event.which == 13) {
                if (get_searchterm()) {
                    dm4c.toolbar.do_search()
                }
            } else {
                update_search_button_state()
            }
        }

        function update_search_button_state() {
            dm4c.toolbar.enable_search_button(get_searchterm())
        }

        function get_searchterm() {
            return $.trim(search_field.val())
        }
    })



    // === Webclient Listeners ===

    // select initial searchmode
    dm4c.add_listener("init", function() {
        dm4c.toolbar.select_searchmode(SEARCHMODE_URI)
    })
})
