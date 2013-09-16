dm4c.add_plugin("de.deepamehta.filemanager", function() {

    // === Webclient Listeners ===

    dm4c.add_listener("post_refresh_create_menu", function(type_menu) {
        if (!dm4c.has_create_permission("dm4.files.folder")) {
            return
        }
        //
        type_menu.add_separator()
        type_menu.add_item({label: "New File Browser", handler: function() {
            dm4c.get_plugin("de.deepamehta.files").create_folder_topic({path: "/"}, true, true)
        }})
    })

    // === Files Listeners ===

    dm4c.add_listener("process_files_drop", function(files) {
        dm4c.topicmap_renderer.start_grid_positioning()
        //
        var dir_count = files.get_directory_count()
        for (var i = 0; i < dir_count; i++) {
            dm4c.get_plugin("de.deepamehta.files").create_file_topics(files.get_directory(i), i == 0)
        }
        for (var i = 0; i < files.get_file_count(); i++) {
            dm4c.get_plugin("de.deepamehta.files").create_file_topic(files.get_file(i), !dir_count && i == 0)
        }
        //
        dm4c.topicmap_renderer.stop_grid_positioning()
    })
})
