/**
 * Renders a topic as "icon + label".
 * The clickable area is the icon.
 * The label is truncated and line wrapped.
 */
function CanvasDefaultConfiguration(canvas_topics, canvas_assocs) {

    var LABEL_DIST_Y = 4        // in pixel

    /**
     * Adds "width" and "height" properties to the topic view. The CanvasView relies on these for click detection.
     * Adds "label_wrapper" proprietary property.
     * Adds "icon_pos", "label_pos_y" proprietary properties. Updated on topic move.
     *
     * @param   tv      A TopicView object (defined in CanvasView),
     *                  has "id", "type_uri", "label", "x", "y" properties.
     */
    this.update_topic = function(tv, ctx) {
        init_icon_and_label(tv, ctx)
        init_pos(tv)
    }

    this.move_topic = function(tv) {
        init_pos(tv)
    }

    this.draw_topic = function(tv, ctx) {
        // 1) render icon
        // Note: the icon object is not hold in the topic view, but looked up every time. This saves us
        // from touching all topic view objects once a topic type's icon changes (via view configuration).
        // Icon lookup is supposed to be a cheap operation.
        var icon = dm4c.get_type_icon(tv.type_uri)
        ctx.drawImage(icon, tv.icon_pos.x, tv.icon_pos.y)
        // 2) render label
        tv.label_wrapper.draw(tv.icon_pos.x, tv.label_pos_y, ctx)
        // Note: the context must be passed to every draw() call.
        // The context changes when the canvas is resized.
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    function init_icon_and_label(tv, ctx) {
        var icon = dm4c.get_type_icon(tv.type_uri)
        tv.width  = icon.width
        tv.height = icon.height
        //
        var label = js.truncate(tv.label, dm4c.MAX_TOPIC_LABEL_CHARS)
        tv.label_wrapper = new js.TextWrapper(label, dm4c.MAX_TOPIC_LABEL_WIDTH, 19, ctx)
        //                                                        // line height 19px = 1.2em
    }

    function init_pos(tv) {
        tv.icon_pos = {
            x: tv.x - tv.width / 2,
            y: tv.y - tv.height / 2
        }
        tv.label_pos_y = tv.icon_pos.y + tv.height + LABEL_DIST_Y + 16    // 16px = 1em
    }
}
