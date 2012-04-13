function DateFieldRenderer(field) {

    this.render_field = function() {
        // field label
        dm4c.render.field_label(field_model)
        // field value
        return js.format_date(field_model.value)
    }

    this.render_form_element = function() {
        var input = $("<input>").attr({type: "hidden", "field-uri": field_model.uri, value: field_model.value})
        input.change(function() {
            $("span", $(this).parent()).text(js.format_date(this.value))
        })
        var date_div = $("<div>")
        date_div.append($("<span>").css("margin-right", "1em").text(js.format_date(field_model.value)))
        date_div.append(input)
        input.datepicker({firstDay: 1, showAnim: "fadeIn", showOtherMonths: true, showOn: "button",
            buttonImage: "images/calendar.gif", buttonImageOnly: true, buttonText: "Choose Date"})
        return date_div
    }

    this.read_form_value = function() {
        return $("[field-uri=" + field_model.uri + "]").val()
    }
}
