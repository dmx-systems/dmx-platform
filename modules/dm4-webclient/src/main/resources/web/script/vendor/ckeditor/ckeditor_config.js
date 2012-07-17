CKEDITOR.editorConfig = function(config) {
    //
    config.language      = "en"
    config.skin          = "kama_dm,/de.deepamehta.webclient/script/vendor/ckeditor/skins/kama_dm/"
    config.contentsCss   = "/de.deepamehta.webclient/script/vendor/ckeditor/css/ckeditor_contents.css"
    config.extraPlugins  = "autogrow"
    config.removePlugins = "elementspath,resize"
    config.format_tags   = "p;h1;h2;h3;h4;pre"
    config.toolbarCanCollapse = false
    config.autoGrow_onStartup = true
    //
    config.toolbar = [
        ["Format"],
        ["NumberedList", "BulletedList", "-", "Outdent", "Indent"],
        ["Bold", "Italic"],
        ["Link", "Unlink", "-", "Image", "Table"],
        ["Source"]
    ]
}
