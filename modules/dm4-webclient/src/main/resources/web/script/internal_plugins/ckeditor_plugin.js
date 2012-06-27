dm4c.add_plugin("ckeditor_plugin", function() {

    // === Webclient Listeners ===

    dm4c.register_listener("post_destroy_form", function() {
        // ### Note: we destroy *all* editor instances. Alternatively we could consult the page model.
        for (var editor_name in CKEDITOR.instances) {
            CKEDITOR.instances[editor_name].destroy(true)   // noUpdate=true (textarea DOM element is already removed)
        }
    })
})
