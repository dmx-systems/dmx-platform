function UploadDialog() {

    var upload_form = $("<form>", {
        method:  "post",
        enctype: "multipart/form-data",
        target:  "upload-target"
    })
        .append($('<input type="file">').attr({name: "file", size: 60}))    // Note: attr() must be used here.
        .append($('<input type="submit">').attr({value: "Upload"}))         // An attr object as 2nd param doesn't work!

    var upload_dialog = dm4c.ui.dialog({title: "Upload File", content: upload_form})

    var upload_target = $("<iframe>", {name: "upload-target"}).hide()
    $("body").append(upload_target)

    // -------------------------------------------------------------------------------------------------- Public Methods

    /**
     * @param   path        the file repository path (a string) to upload the selected file to. Must begin with "/".
     * @param   callback    the function that is invoked once the file has been uploaded and processed at server-side.
     *                      One argument is passed to that function: the object (deserialzed JSON)
     *                      returned by the (server-side) executeCommandHook. ### FIXDOC
     */
    this.open = function(path, callback) {
        upload_form.attr("action", "/files/" + path)
        upload_dialog.open()
        // bind handler
        upload_target.unbind("load")    // Note: the previous handler must be removed
        upload_target.load(upload_complete)

        function upload_complete() {
            upload_dialog.close()
            // Note: iframes must be accessed via window.frames
            var response = $("pre", window.frames["upload-target"].document).text()
            try {
                callback(JSON.parse(response))
            } catch (e) {
                alert("Upload failed: \"" + response + "\"\n\nException=" + JSON.stringify(e))
            }
        }
    }
}
