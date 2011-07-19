function files_plugin() {

    // ------------------------------------------------------------------------------------------------ Overriding Hooks

    this.init = function() {
        extend_rest_client()
    }

    this.process_drop = function(data_transfer) {
        if (js.contains(data_transfer.types, "Files")) {
            if (typeof netscape != "undefined") {
                var files = process_file_drop_firefox(data_transfer)
            } else if (js.contains(data_transfer.types, "text/uri-list")) {
                var files = process_file_drop_safari(data_transfer)
            } else {
                alert("WARNING: drag'n'drop operation is ignored.\n\nDropping files/folders is not yet supported by " +
                    "DeepaMehta for this browser/operating system.\n" + js.inspect(navigator) + js.inspect($.browser))
            }
            // Note: if an error occurred "files" is not initialized
            if (files) {
                dm4c.trigger_hook("process_files_drop", files)
            }
        } else if (js.contains(data_transfer.types, "text/plain")) {
            alert("WARNING: drag'n'drop operation is ignored.\n\nType: text/plain " +
                "(not yet implemented by DeepaMehta)\n\nText: \"" + data_transfer.getData("text/plain") + "\"")
        } else {
            alert("WARNING: drag'n'drop operation is ignored.\n\nUnexpected type " +
                "(not yet implemented by DeepaMehta)\n" + js.inspect(data_transfer))
        }

        function process_file_drop_firefox(data_transfer) {
            try {
                var files = new FilesDataTransfer()
                for (var i = 0, file; file = data_transfer.files[i]; i++) {
                    // Firefox note: a DOM File's "mozFullPath" attribute contains the file's path.
                    // Requires the UniversalFileRead privilege to read.
                    netscape.security.PrivilegeManager.enablePrivilege("UniversalFileRead")
                    var path = file.mozFullPath
                    if (is_directory(file)) {
                        var dropped_dir = dm4c.restc.get_resource("file:" + path)
                        files.add_directory(dropped_dir)
                        continue
                    }
                    files.add_file(new File(file.name, path, file.type, file.size))
                }
                return files
            } catch (e) {
                alert("Local file/folder \"" + file.name + "\" can't be accessed.\n\n" + e)
            }

            function is_directory(file) {
                // Note: Firefox does *not* end the path of a File object which represents a directory with "/".
                // So we can't detect directories safely by relying on the path.
                // Instead: if the item's MIME type is known we know it is not a directory.
                if (file.type) {
                    return false
                }
                // Otherwise we involve the server to get information about the item
                var info = dm4c.restc.get_resource_info("file:" + file.mozFullPath)
                return info.kind == "directory"
            }
        }

        function process_file_drop_safari(data_transfer) {
            var files = new FilesDataTransfer()
            // Note: Safari provides a "text/uri-list" data flavor which holds the URIs of the files dropped
            var uri_list = data_transfer.getData("text/uri-list").split("\n")
            for (var i = 0, file; file = data_transfer.files[i]; i++) {
                var path = uri_to_path(uri_list[i])
                if (is_directory(path)) {
                    var dropped_dir = dm4c.restc.get_resource("file:" + path)
                    files.add_directory(dropped_dir)
                    continue
                }
                files.add_file(new File(file.name, path, file.type, file.size))
            }
            return files

            function uri_to_path(uri) {
                // Note: local file URIs provided by Safari begin with "file://localhost" which must be cut off
                if (uri.match(/^file:\/\/localhost(.*)/)) {
                    uri = RegExp.$1
                }
                // Note: local file URIs provided by Safari are encoded
                return decodeURIComponent(uri)
            }

            function is_directory(path) {
                // Note: Safari does end the URI of a File object which represents a directory with "/".
                return path.match(/.$/)[0] == "/"
            }
        }
    }

    /**
     * @param   topic   a CanvasTopic object
     */
    this.topic_doubleclicked = function(topic) {
        if (topic.type == "de/deepamehta/core/topictype/File" ||
            topic.type == "de/deepamehta/core/topictype/Folder") {
            dm4c.restc.execute_command("deepamehta-files.open-file", {topic_id: topic.id})
        }
    }

    // ------------------------------------------------------------------------------------------------------ Public API

    /**
     * Creates a File topic for the given file and shows the topic on the canvas.
     *
     * @param   file        A File object (with "name", "path", "type", and "size" attributes).
     * @param   do_select   Optional: if evaluates to true the File topic is selected on the canvas.
     */
    this.create_file_topic = function(file, do_select) {
        var file_topic = dm4c.restc.execute_command("deepamehta-files.create-file-topic", {path: file.path})
        dm4c.add_topic_to_canvas(file_topic, do_select ? "show" : "none")
    }

    /**
     * Creates a Folder topic for the given directory and shows the topic on the canvas.
     *
     * @param   dir         A Directory object (with "name", "path", and "items" attributes).
     * @param   do_select   Optional: if evaluates to true the Folder topic is selected on the canvas.
     */
    this.create_folder_topic = function(dir, do_select) {
        var folder_topic = dm4c.restc.execute_command("deepamehta-files.create-folder-topic", {path: dir.path})
        dm4c.add_topic_to_canvas(folder_topic, do_select ? "show" : "none")
    }

    /**
     * Creates respective File and Folder topics for all items contained in the given directory
     * and shows the topics on the canvas.
     *
     * @param   dir                 A Directory object (with "name", "path", and "items" attributes).
     * @param   select_first_topic  Optional: if evaluates to true the first created topic is selected on the canvas.
     */
    this.create_file_topics = function(dir, select_first_topic) {
        for (var i = 0, item; item = dir.items[i]; i++) {
            var do_select = select_first_topic && i == 0
            if (item.kind == "file") {
                this.create_file_topic(item, do_select)
            } else if (item.kind == "directory") {
                this.create_folder_topic(item, do_select)
            } else {
                alert("WARNING (create_file_topics):\n\nItem \"" + item.name + "\" of directory \"" +
                    dir.name + "\" is of unexpected kind: \"" + item.kind + "\".")
            }
        }
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    function extend_rest_client() {

        /**
         * @param   uri     Must not be URI-encoded!
         */
        dm4c.restc.get_resource = function(uri, type, size) {
            var params = this.createRequestParameter({type: type, size: size})
            return this.request("GET", "/resource/" + encodeURIComponent(uri) + "?" + params.to_query_string())
        }

        /**
         * @param   uri     Must not be URI-encoded!
         */
        dm4c.restc.get_resource_info = function(uri) {
            return this.request("GET", "/resource/" + encodeURIComponent(uri) + "/info")
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Classes

    function FilesDataTransfer() {

        var files = []
        var directories = []

        // ---

        this.add_file = function(file) {
            files.push(file)
        }

        this.add_directory = function(directory) {
            directories.push(directory)
        }

        // ---

        this.get_file_count = function() {
            return files.length
        }

        this.get_directory_count = function() {
            return directories.length
        }

        // ---

        this.get_file = function(index) {
            return files[index]
        }

        this.get_directory = function(index) {
            return directories[index]
        }
    }

    function File(name, path, type, size) {
        this.kind = "file"
        this.name = name
        this.path = path
        this.type = type
        this.size = size
    }

    function Directory(name, path, items) {
        this.kind = "directory"
        this.name = name
        this.path = path
        this.items = items  // array of File and Directory objects
    }
}
