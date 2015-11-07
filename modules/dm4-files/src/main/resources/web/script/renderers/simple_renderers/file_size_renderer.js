dm4c.add_simple_renderer("dm4.files.file_size_renderer", {

    render_info: function(page_model, parent_element) {
        dm4c.render.field_label(page_model, parent_element)
        var size = page_model.value
        parent_element
            .append(js.format_file_size(size))
            .attr("title", size + " bytes")
    }
})
