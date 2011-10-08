function Canvas(width, height) {

    // ------------------------------------------------------------------------------------------------ Constructor Code

    this.superclass = TopicmapRenderer
    this.superclass()
    var self = this

    // Settings
    this.DEFAULT_ASSOC_COLOR = "#b2b2b2"
    var ASSOC_WIDTH = 4
    var ASSOC_CLICK_TOLERANCE = 0.3
    var HIGHLIGHT_COLOR = "#0000ff"
    var HIGHLIGHT_BLUR = 16
    var ANIMATION_STEPS = 30
    var ANIMATION_DELAY = 10
    var LABEL_FONT = "1em 'Lucida Grande', Verdana, Arial, Helvetica, sans-serif"   // copied from webclient.css
    var LABEL_COLOR = "black"
    var LABEL_DIST_Y = 4            // in pixel
    var LABEL_MAX_WIDTH = 200       // in pixel

    // Model
    var canvas_topics               // topics displayed on canvas (Object, key: topic ID, value: CanvasTopic)
    var canvas_assocs               // associations displayed on canvas (Object, key: assoc ID, value: CanvasAssoc)
    var trans_x, trans_y            // canvas translation (in pixel)
    var highlight_object_id         // ID of the highlighted topic or association, or -1 for no highlighting
    var grid_positioning            // while grid positioning is in progress: a GridPositioning object, null otherwise

    // View (Canvas)
    var ctx                         // the canvas drawing context

    // Short-term Interaction State (model)
    var topic_move_in_progress      // true while topic move is in progress (boolean)
    var canvas_move_in_progress     // true while canvas translation is in progress (boolean)
    var association_in_progress     // true while new association is pulled (boolean)
    var action_topic                // the topic being moved/associated (CanvasTopic)
    var action_assoc                // the association being clicked (CanvasAssoc)
    var tmp_x, tmp_y                // coordinates while action is in progress

    // build the canvas
    init_model()
    create_canvas_element()
    $("#canvas-panel").dblclick(dblclick)
    $("#canvas-panel").mousemove(mousemove)
    $("#canvas-panel").mouseleave(mouseleave)

    // ------------------------------------------------------------------------------------------------------ Public API

    // === Overriding TopicmapRenderer Adapter Methods ===

    this.get_info = function() {
        return {
            uri: "dm4.topicmap_renderer.canvas",
            name: "Topicmap"
        }
    }

    /**
     * Adds a topic to the canvas. If the topic is already on the canvas it is not added again.
     *
     * @param   topic               A Topic object
     *                              ### FIXDOC: a plain JavaScript object with "id", "type_uri", and "value" 
     *                              ### properties is sufficient, plus optional "x" and "y" properties
     * @param   refresh_canvas      Optional: if true, the canvas is refreshed.
     */
    this.add_topic = function(topic, refresh_canvas) {
        if (!topic_exists(topic.id)) {
            // update model
            init_position()
            add_topic(new CanvasTopic(topic))
        }
        // refresh GUI
        if (refresh_canvas) {
            this.refresh()
        }

        function init_position() {
            if (topic.x == undefined && topic.y == undefined) {
                if (grid_positioning) {
                    var pos = grid_positioning.next_position()
                    topic.x = pos.x
                    topic.y = pos.y
                } else {
                    topic.x = width  * Math.random() - trans_x
                    topic.y = height * Math.random() - trans_y
                }
            }
            topic.x = Math.floor(topic.x)
            topic.y = Math.floor(topic.y)
        }
    }

    /**
     * @param   assoc               An Associatin object
     *                              ### FIXDOC: a plain JavaScript object with "id", "type_uri", "role_1", and "role_2"
     *                              ### properties is sufficient
     * @param   refresh_canvas      Optional: if true, the canvas is refreshed.
     */
    this.add_association = function(assoc, refresh_canvas) {
        if (!association_exists(assoc.id)) {
            // update model
            add_association(new CanvasAssoc(assoc))
        }
        // refresh GUI
        if (refresh_canvas) {
            this.refresh()
        }
    }

    // ---

    this.update_topic = function(topic, refresh_canvas) {
        get_topic(topic.id).update(topic)
        // refresh GUI
        if (refresh_canvas) {
            this.refresh()
        }
    }

    this.update_association = function(assoc, refresh_canvas) {
        // update model
        get_association(assoc.id).update(assoc)
        // refresh GUI
        if (refresh_canvas) {
            this.refresh()
        }
    }

    // ---

    /**
     * Removes a topic from the canvas (model) and optionally refreshes the canvas (view).
     * If the topic is not present on the canvas nothing is performed.
     *
     * @param   refresh_canvas  Optional - if true, the canvas is refreshed.
     */
    this.remove_topic = function(id, refresh_canvas) {
        // 1) update model
        var ct = remove_topic(id)
        if (!ct) {
            return
        }
        reset_highlighting_conditionally(id)
        // 2) refresh GUI
        if (refresh_canvas) {
            this.refresh()
        }
    }

    /**
     * Removes an association from the canvas (model) and optionally refreshes the canvas (view).
     * If the association is not present on the canvas nothing is performed.
     *
     * @param   refresh_canvas  Optional - if true, the canvas is refreshed.
     */
    this.remove_association = function(id, refresh_canvas) {
        // 1) update model
        var ca = remove_association(id)
        if (!ca) {
            return
        }
        reset_highlighting_conditionally(id)
        // 2) refresh GUI
        if (refresh_canvas) {
            this.refresh()
        }
    }

    // ---

    // ### FIXME: not in interface
    this.set_highlight_object = function(object_id, refresh_canvas) {
        // update model
        highlight_object_id = object_id
        // refresh GUI
        if (refresh_canvas) {
            this.refresh()
        }
    }

    // ### FIXME: not in interface
    this.reset_highlighting = function(refresh_canvas) {
        // update model
        reset_highlighting()
        // refresh GUI
        if (refresh_canvas) {
            this.refresh()
        }
    }

    // ---

    this.scroll_topic_to_center = function(topic_id) {
        var ct = get_topic(topic_id)
        scroll_to_center(ct.x + trans_x, ct.y + trans_y)
    }

    this.begin_association = function(topic_id, x, y) {
        association_in_progress = true
        action_topic = get_topic(topic_id)
        //
        tmp_x = x
        tmp_y = y
        draw()
    }

    // ### FIXME: not in interface
    this.get_associations = function(topic_id) {
        var assocs = []
        iterate_associations(function(ca) {
            if (ca.get_topic1_id() == topic_id || ca.get_topic2_id() == topic_id) {
                assocs.push(ca)
            }
        })
        return assocs
    }

    this.refresh = function() {
        draw()
    }

    this.clear = function() {
        // refresh GUI
        translate(-trans_x, -trans_y)                       // reset translation
        // update model
        init_model()
    }

    this.resize = function(size) {
        resize_canvas(size)
    }

    this.close_context_menu = function() {
        close_context_menu()
    }

    // --- Grid Positioning ---

    this.start_grid_positioning = function() {
        grid_positioning = new GridPositioning()
    }

    this.stop_grid_positioning = function() {
        grid_positioning = null
    }

    // ----------------------------------------------------------------------------------------------- Private Functions



    /***************/
    /*** Drawing ***/
    /***************/



    function draw() {
        ctx.clearRect(-trans_x, -trans_y, width, height)
        // trigger hook
        dm4c.trigger_plugin_hook("pre_draw_canvas", ctx)
        //
        draw_associations()
        //
        if (association_in_progress) {
            draw_line(action_topic.x, action_topic.y, tmp_x - trans_x, tmp_y - trans_y,
                ASSOC_WIDTH, self.DEFAULT_ASSOC_COLOR)
        }
        //
        draw_topics()
    }

    function draw_topics() {
        // set label style
        ctx.fillStyle = LABEL_COLOR
        //
        iterate_topics(function(ct) {
            // hightlight
            var is_highlight = highlight_object_id == ct.id
            set_highlight_style(is_highlight)
            //
            ct.draw()
            //
            reset_highlight_style(is_highlight)
        })
    }

    function draw_associations() {
        iterate_associations(function(ca) {
            var ct1 = get_topic(ca.get_topic1_id())
            var ct2 = get_topic(ca.get_topic2_id())
            // error check
            if (!ct1 || !ct2) {
                // TODO: deleted associations must be removed from all topicmaps.
                alert("ERROR in draw_associations: association " + ca.id + " is missing a topic")
                // ### delete canvas_assocs[i]
                return
            }
            // hightlight
            var is_highlight = highlight_object_id == ca.id
            set_highlight_style(is_highlight)
            //
            draw_line(ct1.x, ct1.y, ct2.x, ct2.y, ASSOC_WIDTH, ca.color)
            //
            reset_highlight_style(is_highlight)
        })
    }

    function draw_line(x1, y1, x2, y2, width, color) {
        ctx.lineWidth = width
        ctx.strokeStyle = color
        ctx.beginPath()
        ctx.moveTo(x1, y1)
        ctx.lineTo(x2, y2)
        ctx.stroke()
    }

    function set_highlight_style(is_highlight) {
        if (is_highlight) {
            ctx.shadowColor = HIGHLIGHT_COLOR
            ctx.shadowBlur  = HIGHLIGHT_BLUR
        }
    }

    function reset_highlight_style(is_highlight) {
        if (is_highlight) {
            // Note: according to the HTML5 spec setting the blur to 0 should be sufficient to switch the shadow off.
            // Works so in Safari 5 but not in Firefox 3.6. Workaround: set the shadow color to fully-transparent.
            // http://www.whatwg.org/specs/web-apps/current-work/multipage/the-canvas-element.html#shadows
            ctx.shadowColor = "rgba(0, 0, 0, 0)"
            ctx.shadowBlur = 0
        }
    }



    /**********************/
    /*** Event Handling ***/
    /**********************/



    // === Mouse Events ===

    function mousedown(event) {
        if (dm4c.LOG_GUI) dm4c.log("Mouse down!")
        //
        if (event.which == 1) {
            tmp_x = cx(event)
            tmp_y = cy(event)
            //
            var ct = find_topic(event)
            if (ct) {
                action_topic = ct
            } else {
                var ca = find_association(event)
                if (ca) {
                    action_assoc = ca
                } else if (!association_in_progress) {
                    canvas_move_in_progress = true
                }
            }
        }
    }

    function mousemove(event) {
        if (action_topic || canvas_move_in_progress) {
            if (association_in_progress) {
                tmp_x = cx(event)
                tmp_y = cy(event)
            } else if (canvas_move_in_progress) {
                var x = cx(event)
                var y = cy(event)
                translate(x - tmp_x, y - tmp_y)
                tmp_x = x
                tmp_y = y
            } else {
                topic_move_in_progress = true
                var x = cx(event)
                var y = cy(event)
                action_topic.move_by(x - tmp_x, y - tmp_y)
                tmp_x = x
                tmp_y = y
            }
            draw()
        }
    }

    function mouseleave(event) {
        if (dm4c.LOG_GUI) dm4c.log("Mouse leave!")
        //
        if (association_in_progress) {
            end_association_in_progress()
            draw()
        } else if (topic_move_in_progress) {
            end_topic_move()
        } else if (canvas_move_in_progress) {
            end_canvas_move()
        }
        end_interaction()
    }

    function mouseup(event) {
        if (dm4c.LOG_GUI) dm4c.log("Mouse up!")
        //
        close_context_menu()
        //
        if (association_in_progress) {
            end_association_in_progress()
            //
            var ct = find_topic(event)
            if (ct) {
                dm4c.do_create_association("dm4.core.association", ct)
            } else {
                draw()
            }
        } else if (topic_move_in_progress) {
            end_topic_move()
        } else if (canvas_move_in_progress) {
            end_canvas_move()
        } else {
            var ct = find_topic(event)   // ### FIXME: use actionTopic instead of searching again?
            if (ct) {
                dm4c.do_select_topic(ct.id)
            } else if (action_assoc) {
                dm4c.do_select_association(action_assoc.id)
            }
        }
        end_interaction()
    }

    function dblclick(event) {
        var ct = find_topic(event)
        if (ct) {
            dm4c.trigger_plugin_hook("topic_doubleclicked", ct)
        }
    }

    // ---

    function find_topic(event) {
        var x = cx(event, true)
        var y = cy(event, true)
        return iterate_topics(function(ct) {
            if (x >= ct.x - ct.width / 2 && x < ct.x + ct.width / 2 &&
                y >= ct.y - ct.height / 2 && y < ct.y + ct.height / 2) {
                //
                return ct
            }
        })
    }

    function find_association(event) {
        var x = cx(event, true)
        var y = cy(event, true)
        return iterate_associations(function(ca) {
            var ct1 = get_topic(ca.get_topic1_id())
            var ct2 = get_topic(ca.get_topic2_id())
            // bounding rectangle
            var aw2 = ASSOC_WIDTH / 2   // buffer to make orthogonal associations selectable
            var bx1 = Math.min(ct1.x, ct2.x) - aw2
            var bx2 = Math.max(ct1.x, ct2.x) + aw2
            var by1 = Math.min(ct1.y, ct2.y) - aw2
            var by2 = Math.max(ct1.y, ct2.y) + aw2
            var in_bounding = x > bx1 && x < bx2 && y > by1 && y < by2
            if (!in_bounding) {
                return
            }
            // gradient
            var dx1 = x - ct1.x
            var dx2 = x - ct2.x
            var dy1 = y - ct1.y
            var dy2 = y - ct2.y
            if (bx2 - bx1 > by2 - by1) {
                var g1 = dy1 / dx1
                var g2 = dy2 / dx2
            } else {
                var g1 = dx1 / dy1
                var g2 = dx2 / dy2
            }
            // dm4c.log(g1 + " " + g2 + " -> " + Math.abs(g1 - g2))
            //
            if (Math.abs(g1 - g2) < ASSOC_CLICK_TOLERANCE) {
                return ca
            }
        })
    }

    // ---

    function end_topic_move() {
        topic_move_in_progress = false
        // trigger hook
        dm4c.trigger_plugin_hook("post_move_topic", action_topic)
    }

    function end_canvas_move() {
        canvas_move_in_progress = false
    }

    function end_association_in_progress() {
        association_in_progress = false
    }

    function end_interaction() {
        // remove topic activation
        action_topic = null
        action_assoc = null
    }



    // === Context Menu Events ===

    function do_contextmenu(event) {
        if (dm4c.LOG_GUI) dm4c.log("Contextmenu!")
        //
        close_context_menu()
        // 1) assemble commands
        var ct, ca
        if (ct = find_topic(event)) {
            dm4c.do_select_topic(ct.id)
            // Note: only dm4c.selected_object has the auxiliary attributes (the canvas topic has not)
            var commands = dm4c.get_topic_commands(dm4c.selected_object, "context-menu")
        } else if (ca = find_association(event)) {
            dm4c.do_select_association(ca.id)
            // ### FIXME: use dm4c.selected assoc?
            var commands = dm4c.get_association_commands(ca, "context-menu")
        } else {
            var x = cx(event, true)
            var y = cy(event, true)
            var commands = dm4c.get_canvas_commands(x, y, "context-menu")
        }
        // 2) show menu
        open_context_menu(commands, event)
        //
        return false
    }

    /**
     * Builds a context menu from a set of commands and shows it.
     *
     * @param   commands    Array of commands. May be empty. Must not be null/undefined.
     * @param   event       The mouse event that triggered the context menu.
     */
    function open_context_menu(commands, event) {
        if (!commands.length) {
            return
        }
        var cm_x = cx(event)
        var cm_y = cy(event)
        var contextmenu = $("<div>").addClass("menu").css({
            top:  cm_y + "px",
            left: cm_x + "px"
        })
        for (var i = 0, cmd; cmd = commands[i]; i++) {
            if (cmd.is_separator) {
                contextmenu.append("<hr>")
            } else {
                var handler = context_menu_handler(cmd.handler)
                var item_dom = $("<a>").attr("href", "#").click(handler).text(cmd.label)
                add_hovering()
                contextmenu.append(item_dom)
            }
        }
        $("#canvas-panel").append(contextmenu)
        contextmenu.show()

        function context_menu_handler(handler) {
            return function(event) {
                // pass the coordinates of the command selecting mouse click to the command handler
                handler(cx(event) + cm_x, cy(event) + cm_y)
                dm4c.canvas.close_context_menu()
                return false
            }
        }

        // ### FIXME: copy in GUIToolkit
        // ### TODO: refactor canvas context menu to make use of GUIToolkit Menu class
        function add_hovering() {
            item_dom.hover(
                function() {$(this).addClass("hover")},
                function() {$(this).removeClass("hover")}
            )
        }
    }

    function close_context_menu() {
        // remove context menu
        $("#canvas-panel .menu").remove()
    }



    // === Drag and Drop Events ===

    // Required. Otherwise we don't receive a drop.
    function dragover () {
        // Return false is Required. Otherwise we don't receive a drop.
        return false
    }

    function drop(e) {
        // e.preventDefault();  // Useful for debugging when exception is thrown before false is returned.
        dm4c.trigger_plugin_hook("process_drop", e.dataTransfer)
        return false
    }



    /**********************/
    /*** Helper Methods ***/
    /**********************/



    // === Model Helper ===

    function get_topic(id) {
        return canvas_topics[id]
    }

    function add_topic(ct) {
        canvas_topics[ct.id] = ct
    }

    function remove_topic(id) {
        var ct = get_topic(id)
        delete canvas_topics[id]
        return ct
    }

    function topic_exists(id) {
        return get_topic(id) != undefined
    }

    function iterate_topics(func) {
        for (var id in canvas_topics) {
            var ret = func(get_topic(id))
            if (ret) {
                return ret
            }
        }
    }

    // ---

    function get_association(id) {
        return canvas_assocs[id]
    }

    function add_association(ca) {
        canvas_assocs[ca.id] = ca
    }

    function remove_association(id) {
        var ca = get_association(id)
        delete canvas_assocs[id]
        return ca
    }

    function association_exists(id) {
        return get_association(id) != undefined
    }

    function iterate_associations(func) {
        for (var id in canvas_assocs) {
            var ret = func(get_association(id))
            if (ret) {
                return ret
            }
        }
    }

    // ---

    function init_model() {
        canvas_topics = {}
        canvas_assocs = {}
        reset_highlighting()
        trans_x = 0, trans_y = 0
    }

    function reset_highlighting_conditionally(object_id) {
        if (highlight_object_id == object_id) {
            reset_highlighting()
        }
    }

    function reset_highlighting() {
        highlight_object_id = -1
    }



    // === GUI Helper ===

    /**
     * Creates the HTML5 canvas element, binds the event handlers, and adds it to the document.
     *
     * Called in 2 situations:
     * 1) When the main GUI is build initially.
     * 2) When the canvas is resized interactively.
     */
    function create_canvas_element() {
        var canvas = document.createElement("canvas")
        self.dom = $(canvas).attr({id: "canvas", width: width, height: height})
        dm4c.split_panel.set_left_panel(self)
        ctx = canvas.getContext("2d")
        // Note: the canvas font must be set early.
        // Topic label measurement takes place *before* drawing.
        ctx.font = LABEL_FONT
        //
        // bind event handlers
        self.dom.mousedown(mousedown)
        self.dom.mouseup(mouseup)
        canvas.oncontextmenu = do_contextmenu
        canvas.ondragover = dragover
        canvas.ondrop = drop
    }

    /**
     * Resizes the HTML5 canvas element.
     *
     * Called in 2 situations:
     * 1) The user resizes the main window.
     * 2) The user resizes the canvas (by moving the split pane's resizable-handle).
     *
     * @param   size    the new canvas size.
     */
    function resize_canvas(size) {
        if (dm4c.LOG_GUI) dm4c.log("Rebuilding canvas")
        width  = size.width
        height = size.height
        // Note: we don't empty the entire canvas-panel to keep the resizable-handle element.
        $("#canvas-panel #canvas").remove()
        // Note: in order to resize the canvas element we must recreate it.
        // Otherwise the browsers would just distort the canvas rendering.
        create_canvas_element()
        ctx.translate(trans_x, trans_y)
        draw()
    }

    function translate(tx, ty) {
        ctx.translate(tx, ty)
        trans_x += tx
        trans_y += ty
    }

    function scroll_to_center(x, y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            var dx = (width  / 2 - x) / ANIMATION_STEPS
            var dy = (height / 2 - y) / ANIMATION_STEPS
            var animation_count = 0;
            var animation = setInterval(function() {
                translate(dx, dy)
                draw()
                if (++animation_count == ANIMATION_STEPS) {
                    clearInterval(animation)
                }
            }, ANIMATION_DELAY)
        }
    }

    /**
     * Returns the x coordinate of the mouse event.
     *
     * @as_canvas_coordinate    false: returned as screen coordinate.
     *                          true: returned as canvas coordinate (involves canvas viewport).
     */
    function cx(event, as_canvas_coordinate) {
        return event.layerX + (as_canvas_coordinate ? -trans_x : 0)
    }

    function cy(event, as_canvas_coordinate) {
        return event.layerY + (as_canvas_coordinate ? -trans_y : 0)
    }



    // ------------------------------------------------------------------------------------------------- Private Classes

    /**
     * Properties:
     *  id, type_uri, label
     *  x, y                    Topic position. Represents the center of the topic's icon.
     *  width, height           Icon size.
     */
    function CanvasTopic(topic) {

        var self = this
        var label_wrapper

        this.x = topic.x
        this.y = topic.y

        init(topic);

        this.draw = function() {
            try {
                // icon
                var icon = dm4c.get_type_icon(this.type_uri)
                var x = this.x - this.width / 2
                var y = this.y - this.height / 2
                ctx.drawImage(icon, x, y)
                // label
                label_wrapper.draw(x, y + this.height + LABEL_DIST_Y + 16, ctx)    // 16px = 1em
                // Note: the context must be passed to every draw() call.
                // The context changes when the canvas is resized.
            } catch (e) {
                dm4c.log("### ERROR at Canvas.draw_topics:\nicon.src=" + icon.src + "\nicon.width=" + icon.width +
                    "\nicon.height=" + icon.height  + "\nicon.complete=" + icon.complete
                    /* + "\n" + JSON.stringify(e) */)
            }
        }

        this.move_to = function(x, y) {
            this.x = x
            this.y = y
        }

        this.move_by = function(tx, ty) {
            this.x += tx
            this.y += ty
        }

        this.update = function(topic) {
            init(topic)
        }

        // ---

        function init(topic) {
            self.id       = topic.id
            self.type_uri = topic.type_uri
            self.label    = topic.value
            //
            var icon = dm4c.get_type_icon(topic.type_uri)
            self.width  = icon.width
            self.height = icon.height
            //
            label_wrapper = new js.TextWrapper(self.label, LABEL_MAX_WIDTH, 19, ctx)    // line height 19px = 1.2em
        }
    }

    /**
     * Properties:
     *  id, type_uri
     *  role_1, role_2
     *  color           (CSS string)
     */
    function CanvasAssoc(assoc) {

        var ca = this

        this.id = assoc.id
        this.role_1 = assoc.role_1
        this.role_2 = assoc.role_2

        init(assoc)

        this.get_topic1_id = function() {
            return this.role_1.topic_id
        }

        this.get_topic2_id = function() {
            return this.role_2.topic_id
        }

        // ---

        this.update = function(assoc) {
            init(assoc)
        }

        function init(assoc) {
            ca.type_uri = assoc.type_uri
            ca.color = dm4c.get_type_color(assoc.type_uri)
        }
    }

    // ---

    function GridPositioning() {

        // Settings
        var GRID_DIST_X = 220   // LABEL_MAX_WIDTH + 20 pixel padding
        var GRID_DIST_Y = 80
        var START_X = 50 - trans_x
        var START_Y = 50
        var MIN_Y = -9999

        var item_count = 0
        var start_pos = find_start_postition()
        var grid_x = start_pos.x
        var grid_y = start_pos.y

        this.next_position = function() {
            var pos = {x: grid_x, y: grid_y}
            if (item_count == 0) {
                scroll_to_center(width / 2, pos.y + trans_y)
            }
            //
            advance_position()
            item_count++
            //
            return pos
        }

        function find_start_postition() {
            var max_y = MIN_Y
            iterate_topics(function(ct) {
                if (ct.y > max_y) {
                    max_y = ct.y
                }
            })
            var x = START_X
            var y = max_y != MIN_Y ? max_y + GRID_DIST_Y : START_Y
            return {x: x, y: y}
        }

        function advance_position() {
            if (grid_x + GRID_DIST_X + trans_x > width) {
                grid_x = START_X
                grid_y += GRID_DIST_Y
            } else {
                grid_x += GRID_DIST_X
            }
        }
    }
}
