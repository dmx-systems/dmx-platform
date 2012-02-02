CKEDITOR.editorConfig = function(config) {
    //
    config.contentsCss   = "/style/ckeditor_contents.css"
    config.extraPlugins  = "autogrow"
    config.removePlugins = "elementspath,resize"
    config.format_tags   = "p;h1;h2;h3;h4;pre"
    config.toolbarCanCollapse = false
    config.autoGrow_onStartup = true
    //
    config.toolbar = [
        ["Format"],
        ["Bold", "Italic"],
        ["NumberedList", "BulletedList", "-", "Outdent", "Indent"],
        ["Link", "Unlink", "-", "Image", "Table"],
        ["Undo", "Redo"],
        ["Source"]
    ]
}
