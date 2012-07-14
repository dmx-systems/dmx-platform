dm4c.add_field_renderer("dm4.webclient.number_renderer", {

    render_field: function(field_model, parent_element) {
        dm4c.render.field_label(field_model, parent_element)
        parent_element.append(field_model.value)
    },

    render_form_element: function(field_model, parent_element) {
        var input = dm4c.render.input(field_model)
        parent_element.append(input)
        //
        return function() {
            var val = $.trim(input.val())
            var value = Number(val)
            if (isNaN(value)) {
                alert("WARNING: \"" + val + "\" is not a number.\n" +
                    "(field \"" + field_model.label + "\")\n\n" +
                    "The old value is restored.")
                return null     // prevent this field from being updated
            }
            return value
        }
    }
})
