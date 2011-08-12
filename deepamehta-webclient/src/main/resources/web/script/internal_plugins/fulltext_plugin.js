function fulltext_plugin() {

    dm4c.register_css_stylesheet("style/search_result.css")

    // ------------------------------------------------------------------------------------------------ Overriding Hooks

    /*** Provide "By Text" search mode ***/

    this.init = function() {
        dm4c.toolbar.searchmode_menu.add_item({label: "By Text", value: "by-text"})
    }

    this.search_widget = function(searchmode) {
        if (searchmode == "by-text") {
            return $("<input>").attr({id: "search-field", type: "text", size: dm4c.SEARCH_FIELD_WIDTH})
        }
    }

    this.search = function(searchmode) {
        if (searchmode == "by-text") {
            var searchterm = $.trim($("#search-field").val())
            return dm4c.restc.search_topics_and_create_bucket(searchterm)
        }
    }
}
