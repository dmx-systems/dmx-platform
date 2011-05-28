function TopictypeRenderer() {

    var uri_input   // a jQuery <input> element

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************************
    // *** Page Renderer Implementation ***
    // ************************************



    this.render_page = function(topic) {
        get_topic_renderer().render_page(topic)
        //
        dm3c.render.field_label("URI")
        dm3c.render.field_value(topic.uri)
    }

    this.render_form = function(topic) {
        get_topic_renderer().render_form(topic)
        //
        uri_input = dm3c.render.input(topic.uri)
        dm3c.render.field_label("URI")
        dm3c.render.field_value(uri_input)
    }

    this.process_form = function(topic) {
        // 1) update DB and memory
        var topic_type_model = build_topic_type_model()
        // alert("topic type model to update: " + JSON.stringify(topic_type_model))
        var topic_type = dm3c.update_topic_type(topic, topic_type_model)
        dm3c.trigger_plugin_hook("post_submit_form", topic)
        // 2) update GUI
        dm3c.canvas.update_topic(topic_type)    // Note: a topic type is passed as topic. Topic types *are* topics.
        dm3c.canvas.refresh()
        dm3c.page_panel.display(topic_type)

        /**
         * Reads out values from GUI elements and builds a topic type model object from it.
         *
         * @return  a topic type model object
         */
        function build_topic_type_model() {
            var page_model = get_topic_renderer().get_page_model()
            // error check
            if (!(page_model instanceof TopicRenderer.Field)) {
                throw "InvalidPageModel: " + JSON.stringify(page_model)
            }
            //
            var topic_type_model = {
                id: topic.id,
                uri: $.trim(uri_input.val()),
                value: page_model.read_form_value(),
                data_type_uri: "dm3.core.text"
            }
            return topic_type_model
        }
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    // Note: through this function the topic renderer is accessed lazily at rendering time.
    // The topic renderer can't be accessed at instantiation time of this page renderer.
    // Page renderers are loaded asynchronously.
    function get_topic_renderer() {
        return dm3c.get_page_renderer("TopicRenderer")
    }
}
