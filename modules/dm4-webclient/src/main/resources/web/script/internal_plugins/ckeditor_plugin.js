dm4c.add_plugin("de.deepamehta.webclient.ckeditor", function() {

    // === Webclient Listeners ===

    dm4c.add_listener("post_destroy_form", function() {
        // ### Note: we destroy *all* editor instances. Alternatively we could consult the page model.
        for (var editor_name in CKEDITOR.instances) {
            CKEDITOR.instances[editor_name].destroy(true)   // noUpdate=true (textarea DOM element is already removed)
        }
    })
})
