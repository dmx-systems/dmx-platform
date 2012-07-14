/**
 * ### FIXDOC
 * Note: this is inactive code. It is here only for documentation purpose.
 *
 * The DeepaMehta standard distribution provides the following field renderers:
 *     - TextFieldRenderer      (Webclient module)
 *     - NumberFieldRenderer    (Webclient module)
 *     - BooleanFieldRenderer   (Webclient module)
 *     - HTMLFieldRenderer      (Webclient module)
 *     - SearchResultRenderer   (Webclient module)
 *     - TitleRenderer          (Webclient module)
 *     - BodyTextRenderer       (Webclient module)
 *     - DateFieldRenderer      (Webclient module) ### FIXME: not in use
 *     - ReferenceFieldRenderer (Webclient module) ### FIXME: not in use
 *     - FileContentRenderer    (Files module)
 *     - FolderContentRenderer  (Files module)
 *     - IconFieldRenderer      (Icon Picker module)
 */
function FieldRenderer() {

    this.render_field = function(parent_element) {

    /**
     * @return  The form reading function: called to read out the form element's value.
     *          This function is expected to return a simple value or a topic reference (in REF_PREFIX notation).
     */
    this.render_form_element = function(parent_element) {
}
