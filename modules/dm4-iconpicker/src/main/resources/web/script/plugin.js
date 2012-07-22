dm4c.add_plugin("de.deepamehta.iconpicker", function() {

    // === Webclient Listeners ===

    dm4c.add_listener("init", function() {
        dm4c.ui.dialog("iconpicker-dialog", "Choose Icon")
    })
})
