// This is dead DM4 code. TODO: adapt to DM5
dm4c.add_plugin("systems.dmx.filemanager", function() {

    // === Webclient Listeners ===

    dm4c.add_listener("post_refresh_create_menu", function(type_menu) {
        // Note: the toolbar's Create menu is only refreshed when the login status changes, not when a workspace is
        // selected. (At workspace selection time the Create menu is not refreshed but shown/hidden in its entirety.)
        // So, we check the READ permission here, not the CREATE permission. (The CREATE permission involves the
        // WRITEability of the selected workspace.)
        if (!dm4c.has_read_permission_for_topic_type("dmx.files.folder")) {
            return
        }
        //
        type_menu.add_separator()
        type_menu.add_item({label: "New File Browser", handler: function() {
            dm4c.get_plugin("systems.dmx.files").create_folder_topic({path: "/"}, true, true)
        }})
    })

    // === Files Listeners ===

    dm4c.add_listener("process_files_drop", function(files) {
        dm4c.topicmap_renderer.start_grid_positioning()
        //
        var dir_count = files.get_directory_count()
        for (var i = 0; i < dir_count; i++) {
            dm4c.get_plugin("systems.dmx.files").create_file_topics(files.get_directory(i), i == 0)
        }
        for (var i = 0; i < files.get_file_count(); i++) {
            dm4c.get_plugin("systems.dmx.files").create_file_topic(files.get_file(i), !dir_count && i == 0)
        }
        //
        dm4c.topicmap_renderer.stop_grid_positioning()
    })
})
