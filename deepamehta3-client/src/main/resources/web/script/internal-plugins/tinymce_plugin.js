function tinymce_plugin() {

    // ------------------------------------------------------------------------------------------------ Overriding Hooks

    this.post_submit_form = function(doc) {
        for (var i = 0, field; field = dm3c.type_cache.get(doc.type_uri).fields[i]; i++) {
            if (field.data_type == "html") {
                if (!tinyMCE.execCommand("mceRemoveControl", false, "field_" + field.uri)) {
                    alert("mceRemoveControl not executed")
                } else {
                    // alert("TinyMCE instance removed")
                }
            }
        }
    }
}
