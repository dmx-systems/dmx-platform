/**
 * The DeepaMehta standard distribution provides the following field renderers:
 *     - TextFieldRenderer      (Webclient module)
 *     - NumberFieldRenderer    (Webclient module)
 *     - BooleanFieldRenderer   (Webclient module)
 *     - HTMLFieldRenderer      (Webclient module)
 *     - SearchResultRenderer   (Webclient module)
 *     - TitleRenderer          (Webclient module) ### FIXME: drop this?
 *     - BodyTextRenderer       (Webclient module) ### FIXME: drop this?
 *     - DateFieldRenderer      (Webclient module) ### FIXME: not in use
 *     - ReferenceFieldRenderer (Webclient module) ### FIXME: not in use
 *     - FileContentRenderer    (Files module)
 *     - FolderContentRenderer  (Files module)
 *     - IconFieldRenderer      (Icon Picker module)
 */
function FieldRenderer() {

    this.render_field = function(parent_element) {

    this.render_form_element = function(parent_element) {

    /**
     * @return  a simple value or a topic reference (in REF_PREFIX notation)
     */
    this.read_form_value = function() {
}
