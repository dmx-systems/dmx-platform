function fulltext_plugin() {

    dm4c.register_css_stylesheet("style/search_result.css")

    // ------------------------------------------------------------------------------------------------ Overriding Hooks

    /*** Provide "By Text" search mode ***/

    this.init = function() {
        $("#searchmode-select").append($("<option>").text("By Text"))
    }

    this.search_widget = function(searchmode) {
        if (searchmode == "By Text") {
            return $("<input>").attr({id: "search_field", type: "text", size: dm4c.SEARCH_FIELD_WIDTH})
        }
    }

    this.search = function(searchmode) {
        if (searchmode == "By Text") {
            var searchterm = $.trim($("#search_field").val())
            return dm4c.restc.search_topics_and_create_bucket(searchterm)
        }
    }
}
