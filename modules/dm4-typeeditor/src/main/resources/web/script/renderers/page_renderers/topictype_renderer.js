(function() {
    function TopicTypeRenderer() {

        // === Page Renderer Implementation ===

        this.render_page = function(topic) {
            var topic_type = dm4c.get_topic_type(topic.uri)
            this.render_type_page(topic_type)
        }

        this.render_form = function(topic) {
            var topic_type = dm4c.get_topic_type(topic.uri)
            var type_model_func = this.render_type_form(topic_type)
            return function() {
                dm4c.do_update_topic_type(type_model_func())
            }
        }
    }

    TopicTypeRenderer.prototype = new TypeRenderer()

    dm4c.add_page_renderer("dm4.typeeditor.topictype_renderer", new TopicTypeRenderer())
})()
