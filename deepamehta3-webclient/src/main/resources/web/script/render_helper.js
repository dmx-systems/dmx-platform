function RenderHelper() {

    /**
     * @param   field   a field object or a string.
     */
    this.field_label = function(field, suffix) {
        var name
        if (typeof(field) == "string") {
            name = field
        } else {
            name = field.label
            if (suffix) {
                name += suffix
            }
        }
        $("#detail-panel").append($("<div>").addClass("field-name").text(name))
    }

    this.input = function(field) {
        return $("<input>").attr({type: "text", value: field.value})
    }
}
