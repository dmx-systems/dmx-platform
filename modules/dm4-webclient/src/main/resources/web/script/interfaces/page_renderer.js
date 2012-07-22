/**
 * ### FIXDOC
 * Note: this is inactive code. It is here only for documentation purpose.
 *
 * An abstraction of the view component that occupies the right part of the DeepaMehta window (the "page panel").
 * The abstraction comprises:
 *     - a strategy to render a topic/association info page
 *     - a strategy to render a topic/association form
 *     - a strategy to process the form input
 *
 * The Webclient module is coded to this interface (in particular the PagePanel view class and the default_plugin).
 *
 * PageRenderer instances are singletons. They are created and cached by the Webclient main class.
 *
 * The DeepaMehta standard distribution provides the following page renderers:
 *     - dm4.webclient.topic_renderer:       the default renderer for topics       (Webclient module)
 *     - dm4.webclient.association_renderer: the default renderer for associations (Webclient module)
 *     - dm4.typeeditor.topictype_renderer   (Type Editor module)
 *     - dm4.webbrowser.webpage_renderer     (Webbrowser module)
 */
function PageRenderer() {

    this.render_page = function(topic_or_association) {}

    /**
     * @return  The form processing function: called to "submit" the form.
     */
    this.render_form = function(topic_or_association) {}

    // ---

    /**
     * Optional: the page CSS (JavaScript object). If not specified {overflow: "auto"} is used.
     */
    this.page_css = {overflow: "auto"}
}
