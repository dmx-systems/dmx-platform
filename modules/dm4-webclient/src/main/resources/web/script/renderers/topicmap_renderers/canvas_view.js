/**
 * A topicmap view based on HTML5 Canvas.
 */
function CanvasView() {

    // Settings
    var HIGHLIGHT_COLOR = "#0000ff"
    var HIGHLIGHT_BLUR = 16
    var ANIMATION_STEPS = 30
    var ANIMATION_DELAY = 10
    var LABEL_FONT = "1em 'Lucida Grande', Verdana, Arial, Helvetica, sans-serif"   // copied from webclient.css
    var LABEL_COLOR = "black"

    // View
    var canvas_topics           // topics displayed on canvas (Object, key: topic ID, value: TopicView)
    var canvas_assocs           // associations displayed on canvas (Object, key: assoc ID, value: AssociationView)
    var ctx                     // canvas 2D drawing context. Initialized by this.resize()
    var width, height           // canvas size (in pixel)
    var grid_positioning        // while grid positioning is in progress: a GridPositioning object, null otherwise

    // Viewmodel
    var topicmap                // the viewmodel underlying this view (a TopicmapViewmodel)

    // Customization
    var canvas_default_configuration = new CanvasDefaultConfiguration(canvas_topics, canvas_assocs)
    var customizers = []

    // Short-term interaction state
    var topic_move_in_progress      // true while topic move is in progress (boolean)
    var cluster_move_in_progress    // true while cluster move is in progress (boolean)
    var canvas_move_in_progress     // true while canvas translation is in progress (boolean)
    var association_in_progress     // true while new association is pulled (boolean)
    var action_topic                // the topic being selected/moved/associated (TopicView)
    var action_assoc                // the association being selected/cluster-moved (AssociationView)
    var cluster                     // the cluster being moved (ClusterView)
    var tmp_x, tmp_y                // coordinates while action is in progress

    // Coordinate systems (for mouse event interpretation)
    // Note: constants begin at 1 as 0 could be interpreted as "not set"
    var Coord = {
        WINDOW: 1,          // browser window display area
        CANVAS: 2,          // physical canvas display area -- the default
        CANVAS_SPACE: 3     // virtual canvas space (involves canvas translation)
    }

    var self = this

    // ------------------------------------------------------------------------------------------------------ Public API

    this.set_topicmap = function(topicmap_viewmodel) {
        // reset canvas translation
        if (topicmap) {
            ctx.translate(-topicmap.trans_x, -topicmap.trans_y)
        }
        //
        topicmap = topicmap_viewmodel
        //
        ctx.translate(topicmap.trans_x, topicmap.trans_y)
        // update view
        clear()
        topicmap.iterate_topics(function(topic) {
            if (topic.visibility) {
                add_topic(topic)
            }
        })
        topicmap.iterate_associations(function(assoc) {
            add_association(assoc)
        })
    }

    // ---

    /**
     * @param   topic   A TopicViewmodel.
     */
    this.show_topic = function(topic) {
        add_topic(topic)
        show()
    }

    /**
     * @param   assoc   An AssociationViewmodel.
     */
    this.show_association = function(assoc) {
        add_association(assoc)
        show()
    }

    // ---

    /**
     * @param   topic   A TopicViewmodel.
     */
    this.update_topic = function(topic) {
        get_topic(topic.id).update(topic)
        show()
    }

    /**
     * @param   assoc   An AssociationViewmodel.
     */
    this.update_association = function(assoc) {
        get_association(assoc.id).update(assoc)
        show()
    }

    // ---

    this.remove_topic = function(id) {
        delete canvas_topics[id]
        show()
    }

    this.remove_association = function(id) {
        delete canvas_assocs[id]
        show()
    }

    // ---

    this.init_topic_position = function(topic) {
        // restores topic position if topic is already contained in this topicmap but hidden
        var t = topicmap.get_topic(topic.id)
        if (t && !t.visibility) {
            topic.x = t.x
            topic.y = t.y
        }
        //
        if (topic.x == undefined || topic.y == undefined) {
            if (grid_positioning) {
                var pos = grid_positioning.next_position()
            } else if (topicmap.has_selection()) {
                var pos = find_free_position(topicmap.get_selection_pos())
            } else {
                var pos = random_position()
            }
            topic.x = pos.x
            topic.y = pos.y
        }
        topic.x = Math.floor(topic.x)
        topic.y = Math.floor(topic.y)
    }

    this.begin_association = function(topic_id, x, y) {
        association_in_progress = true
        action_topic = get_topic(topic_id)
        //
        tmp_x = x
        tmp_y = y
        show()
    }

    this.scroll_to_center = function(topic_id) {
        var ct = get_topic(topic_id)
        scroll_to_center(ct.x + topicmap.trans_x, ct.y + topicmap.trans_y)
    }

    /**
     * Resizes the HTML5 canvas element.
     *
     * @param   size    the new canvas size.
     */
    this.resize = function(size) {
        width  = size.width
        height = size.height
        //
        // 1) create canvas element
        // Note: in order to resize the canvas element we must recreate it.
        // Otherwise the browsers would just distort the canvas rendering.
        self.dom = $("<canvas>").attr({id: "canvas", width: width, height: height})
        // replace existing canvas element
        // Note: we can't call dm4c.split_panel.set_left_panel() here (-> endless recursion)
        $("#canvas").remove()
        $("#canvas-panel").append(self.dom)
        //
        // 2) initialize the 2D context
        // Note: the canvas element must be already on the page
        ctx = self.dom.get(0).getContext("2d")
        ctx.font = LABEL_FONT   // the canvas font must be set early. Label measurement takes place *before* drawing.
        if (topicmap) { // ### TODO: refactor
            ctx.translate(topicmap.trans_x, topicmap.trans_y)
        }
        //
        bind_event_handlers()
        //
        show()
    }

    this.refresh = function() {
        show()
    }

    // ---

    this.start_grid_positioning = function() {
        grid_positioning = new GridPositioning()
    }

    this.stop_grid_positioning = function() {
        grid_positioning = null
    }

    // ---

    this.add_customizer = function(customizer_func) {
        customizers.push(new customizer_func(canvas_topics, canvas_assocs))
    }



    // ----------------------------------------------------------------------------------------------- Private Functions

    function get_topic(id) {
        return canvas_topics[id]
    }

    function get_association(id) {
        return canvas_assocs[id]
    }

    // ---

    /**
     * @param   topic   A TopicViewmodel.
     */
    function add_topic(topic) {
        canvas_topics[topic.id] = invoke_single_customizer("create_topic", [topic, ctx])
    }

    /**
     * @param   assoc   An AssociationViewmodel.
     */
    function add_association(assoc) {
        canvas_assocs[assoc.id] = new AssociationView(assoc)
    }

    // ---

    function iterate_topics(visitor_func) {
        for (var id in canvas_topics) {
            var ret = visitor_func(get_topic(id))
            if (ret) {
                return ret
            }
        }
    }

    function iterate_associations(visitor_func) {
        for (var id in canvas_assocs) {
            var ret = visitor_func(get_association(id))
            if (ret) {
                return ret
            }
        }
    }

    // ---

    function clear() {
        canvas_topics = {}
        canvas_assocs = {}
    }



    // ***************
    // *** Drawing ***
    // ***************



    function show() {
        // Note: we can't show until a topicmap is available
        if (!topicmap) {
            return
        }
        //
        ctx.clearRect(-topicmap.trans_x, -topicmap.trans_y, width, height)
        // fire event
        dm4c.fire_event("pre_draw_canvas", ctx)
        //
        draw_associations()
        //
        if (association_in_progress) {
            draw_line(action_topic.x, action_topic.y, tmp_x - topicmap.trans_x, tmp_y - topicmap.trans_y,
                dm4c.ASSOC_WIDTH, dm4c.DEFAULT_ASSOC_COLOR)
        }
        //
        ctx.fillStyle = LABEL_COLOR     // set label style
        draw_topics()
    }

    // ---

    function draw_topics() {
        iterate_topics(function(topic) {
            draw_object(topic, customize_draw_topic)
        })
    }

    function draw_associations() {
        iterate_associations(function(assoc) {
            draw_object(assoc, draw_association)
        })
    }

    // ---

    function draw_association(ca) {
        var ct1 = ca.get_topic_1()
        var ct2 = ca.get_topic_2()
        // error check
        if (!ct1 || !ct2) {
            // TODO: deleted associations must be removed from all topicmaps.
            // ### alert("ERROR in draw_associations: association " + this.id + " is missing a topic")
            // ### delete canvas_assocs[i]
            return
        }
        //
        var color = dm4c.get_type_color(ca.type_uri)
        draw_line(ct1.x, ct1.y, ct2.x, ct2.y, dm4c.ASSOC_WIDTH, color)
    }

    // ---

    function draw_object(topic_or_association, drawing_func) {
        // highlight
        var highlight = topicmap.is_selected(topic_or_association.id)
        set_highlight_style(highlight)
        //
        drawing_func(topic_or_association)
        //
        reset_highlight_style(highlight)
    }

    function draw_line(x1, y1, x2, y2, width, color) {
        ctx.lineWidth = width
        ctx.strokeStyle = color
        ctx.beginPath()
        ctx.moveTo(x1, y1)
        ctx.lineTo(x2, y2)
        ctx.stroke()
    }

    // ---

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



    // === Customization ===

    function customize_draw_topic(ct) {
        invoke_customizer("draw_topic", [ct, ctx])
    }

    // ---

    function invoke_customizer(func_name, args) {
        var do_default = true
        for (var i = 0, customizer; customizer = customizers[i]; i++) {
            if (!(customizer[func_name] && customizer[func_name].apply(undefined, args))) {
                do_default = false
            }
        }
        if (do_default) {
            canvas_default_configuration[func_name].apply(undefined, args)
        }
    }

    function invoke_single_customizer(func_name, args) {
        var ret_value
        for (var i = 0, customizer; customizer = customizers[i]; i++) {
            var ret = customizer[func_name] && customizer[func_name].apply(undefined, args)
            if (ret_value) {
                throw "CanvasViewError: more than one customizer feel responsible for \"" + func_name + "\""
            }
            ret_value = ret
        }
        return ret_value || canvas_default_configuration[func_name].apply(undefined, args)
    }



    // **********************
    // *** Event Handling ***
    // **********************



    function bind_event_handlers() {
        self.dom.bind("mousedown",   do_mousedown)
        self.dom.bind("mouseup",     do_mouseup)
        self.dom.bind("mousemove",   do_mousemove)
        self.dom.bind("mouseleave",  do_mouseleave)
        self.dom.bind("dblclick",    do_doubleclick)
        self.dom.bind("contextmenu", do_contextmenu)
        self.dom.bind("dragover",    do_dragover)
        self.dom.bind("drop",        do_drop)
    }



    // === Mouse Events ===

    /**
     * @param   event   a jQuery event object (the browser's event object normalized according to W3C standards).
     *                  http://api.jquery.com/category/events/event-object/
     *                  http://www.w3.org/TR/2003/WD-DOM-Level-3-Events-20030331/ecma-script-binding.html
     */
    function do_mousedown(event) {
        if (association_in_progress) {
            return
        }
        //
        if (event.which == 1) {
            // Note: on a Mac the ctrl key emulates the right mouse button. However, Safari and Chrome for Mac still
            // return 1 in event.which (left mouse button) on a ctrl-click. We must prevent from interpreting this
            // as a left-click.
            if (event.ctrlKey) {
                return
            }
            //
            var p = pos(event)
            tmp_x = p.x
            tmp_y = p.y
            //
            var ct = find_topic(event)
            if (ct) {
                if (event.shiftKey) {
                    dm4c.do_select_topic(ct.id)
                    self.begin_association(ct.id, p.x, p.y)
                } else {
                    action_topic = ct
                }
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

    function do_mousemove(event) {
        // Note: action_topic is defined for a) topic move, and b) association in progress
        if (action_topic || action_assoc || canvas_move_in_progress) {
            var p = pos(event)
            var dx = p.x - tmp_x
            var dy = p.y - tmp_y
            if (canvas_move_in_progress) {
                translate_by(dx, dy)
            } else if (action_assoc) {
                if (!cluster_move_in_progress) {
                    cluster_move_in_progress = true
                    var cluster_viewmodel = topicmap.create_cluster(action_assoc)
                    cluster = new ClusterView(cluster_viewmodel)
                }
                cluster.move_by(dx, dy)
            } else if (!association_in_progress) {
                topic_move_in_progress = true
                action_topic.move_by(dx, dy)
            }
            tmp_x = p.x
            tmp_y = p.y
            show()
        }
    }

    function do_mouseleave(event) {
        if (topic_move_in_progress) {
            end_topic_move()
        } else if (cluster_move_in_progress) {
            end_cluster_move()
        } else if (canvas_move_in_progress) {
            end_canvas_move()
        } else if (association_in_progress) {
            end_association_in_progress()
            show()
        }
    }

    function do_mouseup(event) {
        close_context_menu()
        //
        if (topic_move_in_progress) {
            end_topic_move()
        } else if (cluster_move_in_progress) {
            end_cluster_move()
        } else if (canvas_move_in_progress) {
            end_canvas_move()
        } else if (association_in_progress) {
            var ct = find_topic(event)
            if (ct && ct.id != action_topic.id) {
                dm4c.do_create_association("dm4.core.association", ct)
            }
            //
            end_association_in_progress()
            show()
        } else {
            if (action_topic) {
                dm4c.do_select_topic(action_topic.id)
                action_topic = null
            } else if (action_assoc) {
                dm4c.do_select_association(action_assoc.id)
                action_assoc = null
            }
        }
    }

    function do_doubleclick(event) {
        var ct = find_topic(event)
        if (ct) {
            dm4c.fire_event("topic_doubleclicked", ct)
        }
    }

    // ---

    function end_topic_move() {
        // update viewmodel
        topicmap.set_topic_position(action_topic.id, action_topic.x, action_topic.y)
        // Note: the view is already up-to-date. It is constantly updated while mouse dragging.
        //
        // fire event
        dm4c.fire_event("post_move_topic", action_topic)
        //
        topic_move_in_progress = false
        action_topic = null
    }

    function end_cluster_move() {
        // update viewmodel
        topicmap.set_cluster_position(cluster)
        // Note: the view is already up-to-date. It is constantly updated while mouse dragging.
        //
        // fire event
        dm4c.fire_event("post_move_cluster", cluster)
        //
        cluster_move_in_progress = false
        action_assoc = null
    }

    function end_canvas_move() {
        // update viewmodel
        topicmap.set_translation(topicmap.trans_x, topicmap.trans_y)
        // Note: the view is already up-to-date. It is constantly updated while mouse dragging.
        //
        // fire event
        dm4c.fire_event("post_move_canvas", topicmap.trans_x, topicmap.trans_y)
        //
        canvas_move_in_progress = false
    }

    function end_association_in_progress() {
        association_in_progress = false
        action_topic = null
    }



    // === Context Menu Events ===

    function do_contextmenu(event) {
        close_context_menu()
        //
        // 1) assemble commands
        var ct, ca
        if (ct = find_topic(event)) {
            dm4c.do_select_topic(ct.id)
            // Note: only dm4c.selected_object has the composite value (the canvas topic has not)
            var commands = dm4c.get_topic_commands(dm4c.selected_object, "context-menu")
        } else if (ca = find_association(event)) {
            dm4c.do_select_association(ca.id)
            // Note: only dm4c.selected_object has the composite value (the canvas assiation has not)
            var commands = dm4c.get_association_commands(dm4c.selected_object, "context-menu")
        } else {
            var p = pos(event, Coord.CANVAS_SPACE)
            var commands = dm4c.get_canvas_commands(p.x, p.y, "context-menu")
        }
        // 2) show menu
        if (commands.length) {
            open_context_menu(commands, event)
        }
        //
        return false
    }

    /**
     * Builds a context menu from a set of commands and shows it.
     *
     * @param   commands    Array of commands. Must neither be empty nor null/undefined.
     * @param   event       The mouse event that triggered the context menu.
     */
    function open_context_menu(commands, event) {
        // fire event (compare to GUITookit's open_menu())
        dm4c.pre_open_context_menu()
        //
        // "Opening context nenu: event.screenY=" + event.screenY +
        //    ", event.clientY=" + event.clientY + ", event.pageY=" + event.pageY +
        //    ", event.originalEvent.layerY=" + event.originalEvent.layerY
        var cm_pos = pos(event, Coord.WINDOW)
        var contextmenu = $("<div>").addClass("menu").css({
            top:  cm_pos.y + "px",
            left: cm_pos.x + "px"
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
                var item_offset = pos(event)
                handler(cm_pos.x + item_offset.x, cm_pos.y + item_offset.y)
                close_context_menu()
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
    function do_dragover () {
        // Return false is Required. Otherwise we don't receive a drop.
        return false
    }

    function do_drop(event) {
        // e.preventDefault();  // Useful for debugging when exception is thrown before false is returned.
        dm4c.fire_event("process_drop", event.originalEvent.dataTransfer)
        return false
    }



    // ***************************
    // *** Geometry Management ***
    // ***************************



    function find_topic(event) {
        var p = pos(event, Coord.CANVAS_SPACE)
        return find_topic_by_position(p)
    }

    function find_association(event) {
        var p = pos(event, Coord.CANVAS_SPACE)
        return find_association_by_position(p)
    }

    // ---

    /**
     * @param   pos     an object with "x" and "y" properties.
     */
    function find_topic_by_position(pos) {
        return iterate_topics(function(ct) {
            if (pos.x >= ct.x - ct.width  / 2 && pos.x < ct.x + ct.width  / 2 &&
                pos.y >= ct.y - ct.height / 2 && pos.y < ct.y + ct.height / 2) {
                //
                return ct
            }
        })
    }

    /**
     * @param   pos     an object with "x" and "y" properties.
     */
    function find_association_by_position(pos) {
        var x = pos.x
        var y = pos.y
        return iterate_associations(function(ca) {
            var ct1 = ca.get_topic_1()
            var ct2 = ca.get_topic_2()
            // bounding rectangle
            var aw2 = dm4c.ASSOC_WIDTH / 2   // buffer to make orthogonal associations selectable
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
            //
            if (Math.abs(g1 - g2) < dm4c.ASSOC_CLICK_TOLERANCE) {
                return ca
            }
        })
    }

    // ---

    /**
     * Interprets a mouse event according to a coordinate system.
     *
     * @param   coordinate_system   Optional: Coord.WINDOW, Coord.CANVAS (default), Coord.CANVAS_SPACE
     *
     * @return  an object with "x" and "y" properties.
     */
    function pos(event, coordinate_system) {
        // set default
        coordinate_system = coordinate_system || Coord.CANVAS
        //
        switch (coordinate_system) {
        case Coord.WINDOW:
            return {
                x: event.clientX,
                y: event.clientY
            }
        case Coord.CANVAS:
            return {
                x: event.originalEvent.layerX,
                y: event.originalEvent.layerY
            }
        case Coord.CANVAS_SPACE:
            return {
                x: event.originalEvent.layerX - topicmap.trans_x,
                y: event.originalEvent.layerY - topicmap.trans_y
            }
        }
    }

    /**
     * @return  an object with "x" and "y" properties.
     */
    function find_free_position(start_pos) {
        var RADIUS_INCREMENT = 150
        var round_count = 0
        var radius = 0
        while (true) {
            round_count++
            radius += RADIUS_INCREMENT
            var max_tries = 10 * round_count
            for (var t = 0; t < max_tries; t++) {
                var pos = {
                    x: start_pos.x + 2 * radius * Math.random() - radius,
                    y: start_pos.y + 2 * radius * Math.random() - radius
                }
                if (is_position_free(pos)) {
                    return pos
                }
            }
        }
    }

    function is_position_free(pos) {
        return true     // ### TODO
    }

    /**
     * @return  an object with "x" and "y" properties.
     */
    function random_position() {
        return {
            x: width  * Math.random() - topicmap.trans_x,
            y: height * Math.random() - topicmap.trans_y
        }
    }

    // ---

    function translate_by(dx, dy) {
        // update viewmodel
        topicmap.translate_by(dx, dy)   // Note: topicmap.translate_by() doesn't update the DB.
        // update view
        ctx.translate(dx, dy)
    }

    function scroll_to_center(x, y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            var dx = (width  / 2 - x) / ANIMATION_STEPS
            var dy = (height / 2 - y) / ANIMATION_STEPS
            var step_count = 0;
            var animation = setInterval(animation_step, ANIMATION_DELAY)
        }

        function animation_step() {
            translate_by(dx, dy)
            show()
            if (++step_count == ANIMATION_STEPS) {
                clearInterval(animation)
                // Note: the Topicmaps module's setTopicmapTranslation()
                // resource method expects integers (otherwise 404)
                topicmap.trans_x = Math.floor(topicmap.trans_x)
                topicmap.trans_y = Math.floor(topicmap.trans_y)
                //
                end_canvas_move()
            }
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Classes

    /**
     * Properties:
     *  id, type_uri
     *  topic_id_1, topic_id_2
     *
     * @param   assoc   An AssociationViewmodel.
     */
    function AssociationView(assoc) {

        var self = this

        this.id = assoc.id
        this.topic_id_1 = assoc.topic_id_1
        this.topic_id_2 = assoc.topic_id_2

        init(assoc)

        // ---

        this.get_topic_1 = function() {
            return get_topic(this.topic_id_1)
        }

        this.get_topic_2 = function() {
            return get_topic(this.topic_id_2)
        }

        // ---

        /**
         * @param   assoc   An AssociationViewmodel.
         */
        this.update = function(assoc) {
            init(assoc)
        }

        // ---

        function init(assoc) {
            self.type_uri = assoc.type_uri
        }
    }

    /**
     * @param   cluster     A ClusterViewmodel
     */
    function ClusterView(cluster) {

        var topics = []

        cluster.iterate_topics(function(topic) {
            topics.push(get_topic(topic.id))
        })

        this.move_by = function(dx, dy) {
            this.iterate_topics(function(topic) {
                topic.move_by(dx, dy)
            })
        }

        this.iterate_topics = function(visitor_func) {
            for (var i = 0, ct; ct = topics[i]; i++) {
                visitor_func(ct)
            }
        }
    }

    // ---

    function GridPositioning() {

        // Settings
        var GRID_DIST_X = 220   // MAX_TOPIC_LABEL_WIDTH + 20 pixel padding
        var GRID_DIST_Y = 80
        var START_X = 50 - topicmap.trans_x
        var START_Y = 50
        var MIN_Y = -9999

        var item_count = 0
        var start_pos = find_start_postition()
        var grid_x = start_pos.x
        var grid_y = start_pos.y

        this.next_position = function() {
            var pos = {x: grid_x, y: grid_y}
            if (item_count == 0) {
                scroll_to_center(width / 2, pos.y + topicmap.trans_y)
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
            if (grid_x + GRID_DIST_X + topicmap.trans_x > width) {
                grid_x = START_X
                grid_y += GRID_DIST_Y
            } else {
                grid_x += GRID_DIST_X
            }
        }
    }
}
