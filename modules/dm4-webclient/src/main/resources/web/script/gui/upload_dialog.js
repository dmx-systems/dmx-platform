function UploadDialog() {

    var UPLOAD_DIALOG_WIDTH = "50em"

    var upload_form = $("<form>", {
        action:  "/files",
        method:  "post",
        enctype: "multipart/form-data",
        target:  "upload-target"
    })
        .append($('<input type="file">').attr({name: "file", size: 60}))    // Note: attr() must be used here.
        .append($('<input type="submit">').attr({value: "Upload"}))         // An attr object as 2nd param doesn't work!

    dm4c.ui.dialog("upload-dialog", "Upload File", upload_form, UPLOAD_DIALOG_WIDTH)

    var upload_target = $("<iframe>", {name: "upload-target"}).hide()
    $("body").append(upload_target)

    // -------------------------------------------------------------------------------------------------- Public Methods

    /**
     * @param   command     the command (a string) send to the server along with the selected file. ### FIXME
     * @param   callback    the function that is invoked once the file has been uploaded and processed at server-side.
     *                      One argument is passed to that function: the object (deserialzed JSON) returned by the
     *                      (server-side) executeCommandHook. ### FIXDOC
     */
    this.show = function(callback) {
        // $("#upload-dialog form").attr("action", "/files/" + command)     // ### FIXME
        $("#upload-dialog").dialog("open")
        // bind handler. Note: the previous handler must be removed
        upload_target.unbind("load")
        upload_target.load(upload_complete)

        function upload_complete() {
            $("#upload-dialog").dialog("close")
            // Note: iframes must be accessed via window.frames
            var result = $("pre", window.frames["upload-target"].document).text()
            try {
                callback(JSON.parse(result))
            } catch (e) {
                alert("Invalid server response: \"" + result + "\"\n\nException=" + JSON.stringify(e))
            }
        }
    }
}
