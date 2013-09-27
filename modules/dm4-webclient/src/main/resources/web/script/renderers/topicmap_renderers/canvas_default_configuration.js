function CanvasDefaultConfiguration(canvas_topics, canvas_assocs) {

    var LABEL_DIST_Y = 4        // in pixel

    this.create_topic = function(topic_viewmodel, ctx) {
        return new TopicView(topic_viewmodel, ctx)
    }

    this.draw_topic = function(ct, ctx) {
        // icon
        var icon = dm4c.get_type_icon(ct.type_uri)
        var x = ct.x - ct.width / 2
        var y = ct.y - ct.height / 2
        ctx.drawImage(icon, x, y)
        // label
        ct.label_wrapper.draw(x, y + ct.height + LABEL_DIST_Y + 16, ctx)    // 16px = 1em
        // Note: the context must be passed to every draw() call.
        // The context changes when the canvas is resized.
    }

    /**
     * Properties:
     *  id, type_uri, label
     *  x, y                    Topic position. Represents the center of the topic's icon.
     *  width, height           Icon size.
     *  label_wrapper
     *
     * @param   topic   A TopicViewmodel.
     */
    function TopicView(topic, ctx) {

        var self = this

        this.id = topic.id
        this.x = topic.x
        this.y = topic.y

        init(topic);

        // ---

        this.move_by = function(dx, dy) {
            this.x += dx
            this.y += dy
        }

        /**
         * @param   topic   A TopicViewmodel.
         */
        this.update = function(topic) {
            init(topic)
        }

        // ---

        function init(topic) {
            self.type_uri = topic.type_uri
            self.label    = topic.label
            //
            var icon = dm4c.get_type_icon(topic.type_uri)
            self.width  = icon.width
            self.height = icon.height
            //
            var label = js.truncate(self.label, dm4c.MAX_TOPIC_LABEL_CHARS)
            self.label_wrapper = new js.TextWrapper(label, dm4c.MAX_TOPIC_LABEL_WIDTH, 19, ctx)
                                                                    // line height 19px = 1.2em
        }
    }
}
