function HTMLFieldRenderer(doc, field, rel_topics) {

    tinymce_options = {
        theme: "advanced",
        content_css: "style/tinymce.css",
        plugins: "autoresize",
        width: "98%",
        extended_valid_elements: "iframe[align<bottom?left?middle?right?top|class|frameborder|height|id|" +
            "longdesc|marginheight|marginwidth|name|scrolling<auto?no?yes|src|style|title|width]",
        // Theme options
        theme_advanced_buttons1: "formatselect,|,bullist,numlist,|,bold,italic,underline,|,link,unlink,anchor,|," +
            "image,code,|,undo,redo",
        theme_advanced_buttons2: "fontselect,fontsizeselect,forecolor,backcolor",
        theme_advanced_buttons3: "",
        theme_advanced_blockformats: "h1,h2,h3,p",
        theme_advanced_toolbar_location: "top",
        theme_advanced_toolbar_align: "left"
    }

    this.render_field = function() {
        // render field label
        dm3c.render.field_label(field)
        // render field value
        return dm3c.get_value(doc, field.uri)
    }

    this.render_form_element = function() {
        var lines = field.lines || DEFAULT_AREA_HEIGHT
        var textarea = $("<textarea>")
        textarea.attr({id: "field_" + field.uri, rows: lines})
        textarea.text(dm3c.get_value(doc, field.uri))
        return textarea
    }

    this.post_render_form_element = function() {
        tinymce_options.window = window
        tinymce_options.element_id = "field_" + field.uri
        if (!tinyMCE.execCommand("mceAddFrameControl", false, tinymce_options)) {
            alert("mceAddFrameControl not executed")
        }
    }

    this.read_form_value = function() {
        return tinyMCE.get("field_" + field.uri).getContent()
    }
}
