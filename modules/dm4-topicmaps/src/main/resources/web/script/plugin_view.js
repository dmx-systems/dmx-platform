function TopicmapsPluginView(controller) {

    // ------------------------------------------------------------------------------------------------------- Constants

    var TOPICMAP_INFO_BUTTON_HELP = "Reveal the selected topicmap on the topicmap itself.\n\n" +
        "Use this to rename/delete the topicmap."

    // --------------------------------------------------------------------------------------------------- Private State

    var topicmap_menu       // A GUIToolkit Menu object

    // ------------------------------------------------------------------------------------------------------ Public API

    this.display_topicmap = display_topicmap
    this.create_topicmap_widget = create_topicmap_widget
    this.refresh_topicmap_menu = refresh_topicmap_menu
    this.refresh_menu_item = refresh_menu_item

    // ----------------------------------------------------------------------------------------------- Private Functions

    /**
     * Displays the selected topicmap based on the model ("topicmap", "topicmap_renderer").
     * <p>
     * Prerequisite: the topicmap menu already shows the selected topicmap.
     *
     * @param   no_history_update   Optional: boolean.
     */
    function display_topicmap(no_history_update) {
        switch_topicmap_renderer()
        controller.get_topicmap_renderer().display_topicmap(controller.get_topicmap(), no_history_update)
    }

    function switch_topicmap_renderer() {
        var renderer_uri = dm4c.topicmap_renderer.get_info().uri
        var topicmap_renderer = controller.get_topicmap_renderer()
        var new_renderer_uri = topicmap_renderer.get_info().uri
        if (renderer_uri != new_renderer_uri) {
            // switch topicmap renderer
            dm4c.topicmap_renderer = topicmap_renderer
            dm4c.split_panel.set_topicmap_renderer(topicmap_renderer)
        }
    }



    // === Topicmap Widget ===

    function create_topicmap_widget() {
        var topicmap_label = $("<span>").attr("id", "topicmap-label").text("Topicmap")
        topicmap_menu = dm4c.ui.menu(do_select_topicmap)
        var topicmap_info_button = dm4c.ui.button({on_click: do_reveal_topicmap, icon: "info"})
            .attr({title: TOPICMAP_INFO_BUTTON_HELP})
        var topicmap_widget = $("<div>").attr("id", "topicmap-widget")
            .append(topicmap_label)
            .append(topicmap_menu.dom)
            .append(topicmap_info_button)
        // put in toolbar
        $("#workspace-widget").after(topicmap_widget)
        //
        refresh_topicmap_menu()

        function do_select_topicmap(menu_item) {
            var topicmap_id = menu_item.value
            if (topicmap_id == "_new") {
                open_topicmap_dialog()
            } else {
                controller.select_topicmap(topicmap_id)
            }
        }

        function do_reveal_topicmap() {
            dm4c.do_reveal_topic(controller.get_topicmap().get_id(), "show")
        }
    }

    /**
     * Refreshes the topicmap menu based on the model ("topicmap_topics", "topicmap").
     */
    function refresh_topicmap_menu() {
        var icon_src = dm4c.get_type_icon_src("dm4.topicmaps.topicmap")
        topicmap_menu.empty()
        // add topicmaps to menu
        var topicmap_topics = controller.get_topicmap_topics()
        for (var i = 0, topicmap_topic; topicmap_topic = topicmap_topics[i]; i++) {
            topicmap_menu.add_item({label: topicmap_topic.value, value: topicmap_topic.id, icon: icon_src})
        }
        // add "New..." to menu
        if (dm4c.has_create_permission_for_topic_type("dm4.topicmaps.topicmap")) {
            topicmap_menu.add_separator()
            topicmap_menu.add_item({label: "New Topicmap...", value: "_new", is_trigger: true})
        }
        //
        refresh_menu_item()
        //
        dm4c.fire_event("post_refresh_topicmap_menu", topicmap_menu)
    }

    /**
     * Selects an item from the topicmap menu based on the model ("topicmap").
     */
    function refresh_menu_item() {
        topicmap_menu.select(controller.get_topicmap().get_id())
    }



    // === Topicmap Dialog ===

    function open_topicmap_dialog() {
        var title_input = dm4c.render.input(undefined, 30)
        var type_menu = create_maptype_menu()
        var private_checkbox = dm4c.ui.checkbox(true)    // checked=true
        var dialog_content = dm4c.render.label("Title").add(title_input)
        if (type_menu.get_item_count() > 1) {
            dialog_content = dialog_content.add(dm4c.render.label("Type")).add(type_menu.dom)
        }
        dialog_content = dialog_content.add(dm4c.render.label("Private")).add(private_checkbox.dom)
        //
        dm4c.ui.dialog({
            title: "New Topicmap",
            content: dialog_content,
            button_label: "Create",
            button_handler: do_create_topicmap
        })

        function create_maptype_menu() {
            var menu = dm4c.ui.menu()
            controller.iterate_topicmap_renderers(function(renderer) {
                var info = renderer.get_info()
                menu.add_item({label: info.name, value: info.uri})
            })
            return menu
        }

        function do_create_topicmap() {
            var name = title_input.val()
            var topicmap_renderer_uri = type_menu.get_selection().value
            var private = private_checkbox.checked
            controller.create_topicmap(name, topicmap_renderer_uri, private)
        }
    }
}
// Enable debugging for dynamically loaded scripts:
//# sourceURL=topicmaps_plugin_view.js
