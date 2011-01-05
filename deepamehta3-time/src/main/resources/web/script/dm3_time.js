function dm3_time() {

    dm3c.register_field_renderer("/de.deepamehta.3-time/script/timestamp_field_renderer.js")
    dm3c.css_stylesheet("/de.deepamehta.3-time/style/dm3-time.css")



    /**************************************************************************************************/
    /**************************************** Overriding Hooks ****************************************/
    /**************************************************************************************************/



    /*** Provide "By Time" search mode ***/

    this.init = function() {
        $("#searchmode-select").append($("<option>").text("By Time"))
    }

    this.search_widget = function(searchmode) {
        if (searchmode == "By Time") {
            return dm3c.ui.menu("time_select", undefined, [
                {label: "Last week",  value:  7},
                {label: "Last month", value: 30}
            ]).dom
        }
    }

    this.search = function(searchmode) {
        if (searchmode == "By Time") {
            // perform time search
            var days = dm3c.ui.menu_item("time_select").value
            var upper_date = new Date().getTime()
            var lower_date = upper_date - days * 24 * 60 * 60 * 1000
            var query = "[" + lower_date + " TO " + upper_date + "]"
            return dm3c.restc.search_topics_and_create_bucket(query, "de/deepamehta/core/property/DateModified", true)
        }
    }

    /*** Customizing search result view ***/

    this.render_topic_list_item = function(topic, list_item) {
        var timestamp = dm3c.get_value(topic, "de/deepamehta/core/property/DateModified")
        var time_div = $("<div>").addClass("result-item-time").append(js.format_timestamp(timestamp))
        return list_item.append(time_div)
    }
} 
