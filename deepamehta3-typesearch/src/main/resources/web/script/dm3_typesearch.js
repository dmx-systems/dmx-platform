function dm3_typesearch() {

    // ------------------------------------------------------------------------------------------------------ Public API



    // *******************************
    // *** Overriding Plugin Hooks ***
    // *******************************



    /*** Provide "By Type" search mode ***/

    this.init = function() {
        $("#searchmode-select").append($("<option>").text("By Type"))
    }

    this.search_widget = function(searchmode) {
        if (searchmode == "By Type") {
            return dm3c.create_type_menu("search-type-menu").dom
        }
    }

    this.search = function(searchmode) {
        if (searchmode == "By Type") {
            // perform type search
            var type_uri = dm3c.ui.menu_item("search-type-menu").value
            return dm3c.restc.get_topics_and_create_bucket(type_uri)
        }
    }

    /***  Working together with the DM3 Type Editor plugin ***/

    /**
     * Once a "Topic Type" topic is updated we rebuild the type menu.
     */
    this.post_update_topic = function(topic, old_properties) {
        if (topic.type_uri == "de/deepamehta/core/topictype/TopicType") {
            update_type_menu()
        }
    }

    /**
     * Once a "Topic Type" topic is deleted we rebuild the type menu.
     */
    this.post_delete_topic = function(topic) {
        if (topic.type_uri == "de/deepamehta/core/topictype/TopicType") {
            update_type_menu()
        }
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    function update_type_menu() {
        // Rebuilding the type menu is only required if the "By Type" searchmode is active.
        if (dm3c.ui.menu_item("searchmode-select").label == "By Type") {
            dm3c.recreate_type_menu("search-type-menu")
        }
    }
}
