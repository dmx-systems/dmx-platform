function UploadDialog() {

    var UPLOAD_DIALOG_WIDTH = "50em"

    var upload_form = $("<form>").attr({method: "post", enctype: "multipart/form-data", target: "upload-target"})
        .append($('<input type="file">').attr({name: "file", size: 60}))
        .append($('<input type="submit">').attr({value: "Upload"}))

    dm4c.ui.dialog("upload-dialog", "Upload File", upload_form, UPLOAD_DIALOG_WIDTH)

    $("body").append($("<iframe>", {id: "upload-target", name: "upload-target"}).hide())

    // -------------------------------------------------------------------------------------------------- Public Methods

    /**
     * @param   command     the command (a string) send to the server along with the selected file.
     * @param   callback    the function that is invoked once the file has been uploaded and processed at server-side.
     *                      One argument is passed to that function: the object (deserialzed JSON) returned by the
     *                      (server-side) executeCommandHook. ### FIXDOC
     */
    this.show = function(command, callback) {
        $("#upload-dialog form").attr("action", "/core/command/" + command)
        $("#upload-dialog").dialog("open")
        // bind callback function, using artifact ID as event namespace
        $("#upload-target").unbind("load.deepamehta-webclient")
        $("#upload-target").bind("load.deepamehta-webclient", upload_complete(callback))

        function upload_complete(callback) {
            return function() {
                $("#upload-dialog").dialog("close")
                // Note: iframes (the upload target) must be DOM manipulated as frames
                var result = $("pre", window.frames["upload-target"].document).text()
                try {
                    callback(JSON.parse(result))
                } catch (e) {
                    alert("No valid server response: \"" + result + "\"\n\nException=" + JSON.stringify(e))
                }
            }
        }
    }
}
