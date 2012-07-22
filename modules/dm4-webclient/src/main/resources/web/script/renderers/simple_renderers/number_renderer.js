dm4c.add_simple_renderer("dm4.webclient.number_renderer", {

    render_info: function(page_model, parent_element) {
        dm4c.render.field_label(page_model, parent_element)
        parent_element.append(page_model.value)
    },

    render_form: function(page_model, parent_element) {
        var input = dm4c.render.input(page_model)
        parent_element.append(input)
        //
        return function() {
            var val = $.trim(input.val())
            var value = Number(val)
            if (isNaN(value)) {
                alert("WARNING: \"" + val + "\" is not a number.\n" +
                    "(field \"" + page_model.label + "\")\n\n" +
                    "The old value is restored.")
                return null     // prevent this field from being updated
            }
            return value
        }
    }
})
