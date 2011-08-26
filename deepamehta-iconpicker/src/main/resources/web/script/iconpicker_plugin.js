function iconpicker_plugin() {

    dm4c.register_field_renderer("/de.deepamehta.iconpicker/script/icon_field_renderer.js")



    // ***********************************************************
    // *** Webclient Hooks (triggered by deepamehta-webclient) ***
    // ***********************************************************



    this.init = function() {
        dm4c.ui.dialog("iconpicker-dialog", "Choose Icon")
    }
}
