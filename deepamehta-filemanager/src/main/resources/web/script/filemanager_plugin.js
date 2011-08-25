function filemanager_plugin() {



    // ***********************************************************
    // *** Webclient Hooks (triggered by deepamehta-webclient) ***
    // ***********************************************************



    this.process_files_drop = function(files) {
        dm4c.canvas.start_grid_positioning()
        //
        var dir_count = files.get_directory_count()
        for (var i = 0; i < dir_count; i++) {
            dm4c.get_plugin("files_plugin").create_file_topics(files.get_directory(i), i == 0)
        }
        for (var i = 0; i < files.get_file_count(); i++) {
            dm4c.get_plugin("files_plugin").create_file_topic(files.get_file(i), !dir_count && i == 0)
        }
        //
        dm4c.canvas.stop_grid_positioning()
    }
}
