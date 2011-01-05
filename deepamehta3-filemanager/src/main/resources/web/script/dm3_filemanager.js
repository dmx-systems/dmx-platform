function dm3_filemanager() {

    // ------------------------------------------------------------------------------------------------ Overriding Hooks

    this.process_files_drop = function(files) {
        dm3c.canvas.start_grid_positioning()
        //
        var dir_count = files.get_directory_count()
        for (var i = 0; i < dir_count; i++) {
            dm3c.get_plugin("dm3_files").create_file_topics(files.get_directory(i), i == 0)
        }
        for (var i = 0; i < files.get_file_count(); i++) {
            dm3c.get_plugin("dm3_files").create_file_topic(files.get_file(i), !dir_count && i == 0)
        }
        //
        dm3c.canvas.stop_grid_positioning()
    }
}
