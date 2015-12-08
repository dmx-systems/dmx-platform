/**
 * ### FIXDOC
 * Note: this is inactive code. It is here only for documentation purpose.
 *
 * The DeepaMehta standard distribution provides the following simple renderers:
 *     - TextRenderer           (Webclient module)
 *     - NumberRenderer         (Webclient module)
 *     - BooleanRenderer        (Webclient module)
 *     - HTMLRenderer           (Webclient module)
 *     - SearchResultRenderer   (Webclient module)
 *     - TitleRenderer          (Webclient module)
 *     - BodyTextRenderer       (Webclient module)
 *     - DateFieldRenderer      (Webclient module) ### FIXME: not in use
 *     - ReferenceFieldRenderer (Webclient module) ### FIXME: not in use
 *     - FileContentRenderer    (Files module)
 *     - FolderContentRenderer  (Files module)
 *     - IconRenderer           (Icon Picker module)
 */
function SimpleRenderer() {

    this.render_info = function(parent_element) {}

    /**
     * @return  The form reading function: called to read out the form element's value.
     *          This function is expected to return a simple value or a topic reference (in REF_ID_PREFIX notation).
     */
    this.render_form = function(parent_element) {}
}
