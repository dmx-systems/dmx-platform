function dm4_iconpicker() {

    dm4c.register_field_renderer("/de.deepamehta.iconpicker/script/icon_field_renderer.js")

    var plugin = this



    /**************************************************************************************************/
    /**************************************** Overriding Hooks ****************************************/
    /**************************************************************************************************/



    this.init = function() {
        create_icon_dialog()
    }



    /************************************************************************************************/
    /**************************************** Custom Methods ****************************************/
    /************************************************************************************************/



    function create_icon_dialog() {
        $("body").append($("<div>").attr("id", "icon_dialog"))
        $("#icon_dialog").dialog({
            title: "Choose Icon", modal: true, autoOpen: false,
            draggable: false, resizable: false, width: 350
        })
    }

    this.open_icon_dialog = function() {
        // query icon topics
        var icon_topics = dm4c.restc.get_topics("de/deepamehta/core/topictype/Icon")
        // fill dialog with icons
        $("#icon_dialog").empty()
        for (var i = 0, icon_topic; icon_topic = icon_topics[i]; i++) {
            // Note: we're a click handler.
            // "this" is the <a> DOM element that invoked the dialog.
            var a = $("<a>")
                .attr({href: "#", title: icon_topic.label})
                .click(icon_selected(icon_topic, this))
            $("#icon_dialog").append(a.append(plugin.render_icon(icon_topic)))
        }
        // open dialog
        $("#icon_dialog").dialog("open")
        //
        return false
    }

    function icon_selected(icon_topic, target) {
        return function() {
            $("#icon_dialog").dialog("close")
            $(target).empty().append(plugin.render_icon(icon_topic))
            return false
        }
    }

    //

    /**
     * @param   icon_topic    a topic of type "Icon"
     */
    this.render_icon = function(icon_topic) {
        var icon_src = icon_topic.properties["de/deepamehta/core/property/IconSource"]
        return $("<img>")
            .attr({"icon-topic-id": icon_topic.id, src: icon_src})
            .addClass("type-icon")
    }
}
