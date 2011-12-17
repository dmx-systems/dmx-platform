/**
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
 *     - TopicRenderer: the default renderer for topics (provided by the Webclient module)
 *     - AssociationRenderer: the default renderer for associations (provided by the Webclient module)
 *     - TopictypeRenderer (provided by the Type Editor module)
 *     - WebpageRenderer (provided by the Webbrowser module)
 */
function PageRenderer() {

    this.render_page = function(topic_or_association) {}

    this.render_form = function(topic_or_association) {}

    this.process_form = function(topic_or_association) {}

    // ---

    this.page_css = function() {
        return {overflow: "auto"}
    }
}
