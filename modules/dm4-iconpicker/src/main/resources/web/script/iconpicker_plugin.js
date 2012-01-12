function iconpicker_plugin() {

    dm4c.register_field_renderer("/de.deepamehta.iconpicker/script/field_renderers/icon_field_renderer.js")
    dm4c.register_css_stylesheet("/de.deepamehta.iconpicker/style/iconpicker.css")

    // === Webclient Listeners ===

    dm4c.register_listener("init", function() {
        dm4c.ui.dialog("iconpicker-dialog", "Choose Icon")
    })
}
