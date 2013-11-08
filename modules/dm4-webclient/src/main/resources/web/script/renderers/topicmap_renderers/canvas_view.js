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
    var topics              // topics displayed on canvas (Object, key: topic ID, value: TopicView)
    var assocs              // associations displayed on canvas (Object, key: assoc ID, value: AssociationView)
    var ctx                 // canvas 2D drawing context. Initialized by this.resize()
    var width, height       // canvas size (in pixel)
    var grid_positioning    // while grid positioning is in progress: a GridPositioning object, null otherwise

    // Viewmodel
    var topicmap            // the viewmodel underlying this view (a TopicmapViewmodel)

    // Customization
    var default_view_customizer
    var view_customizers = []
    var CANVAS_FLAVOR = false
    var DOM_FLAVOR    = false

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
        WINDOW: 1,      // browser window display area
        CANVAS: 2,      // physical canvas display area -- the default
        TOPICMAP: 3     // virtual (endless) topicmap space
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
        ctx.translate(topicmap.trans_x, topicmap.trans_y)   // canvas
        DOM_FLAVOR && update_translation_dom()              // topic layer DOM
        // update view
        DOM_FLAVOR && empty_topic_layer()                   // topic layer DOM
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
        // update view
        add_topic(topic)
        // render
        show()
    }

    /**
     * @param   assoc   An AssociationViewmodel.
     */
    this.show_association = function(assoc) {
        // update view
        add_association(assoc)
        // render
        show()
    }

    // ---

    /**
     * @param   topic   A TopicViewmodel.
     */
    this.update_topic = function(topic) {
        var topic_view = get_topic(topic.id)
        // update view
        topic_view.update(topic)
        // render
        show()                                              // canvas
        DOM_FLAVOR && position_topic_dom(topic_view)        // topic layer DOM
    }

    /**
     * @param   assoc   An AssociationViewmodel.
     */
    this.update_association = function(assoc) {
        // update view
        get_association(assoc.id).update(assoc)
        // render
        show()
    }

    // ---

    this.remove_topic = function(id) {
        var topic_view = get_topic(id)
        if (topic_view) {
            // update view
            delete topics[id]
            // render
            show()                                          // canvas
            DOM_FLAVOR && remove_topic_dom(topic_view)      // topic layer DOM
        }
    }

    this.remove_association = function(id) {
        var assoc_view = get_association(id)
        if (assoc_view) {
            // update view
            delete assocs[id]
            // render
            show()
        }
    }

    // ---

    this.set_topic_selection = function(topic_id) {
        // render
        show()                                              // canvas
        DOM_FLAVOR && update_selection_dom(topic_id)        // topic layer DOM
    }

    this.set_association_selection = function(topic_id) {
        // render
        show()                                              // canvas
        DOM_FLAVOR && remove_selection_dom()                // topic layer DOM
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
        var tv = get_topic(topic_id)
        scroll_to_center(tv.x + topicmap.trans_x, tv.y + topicmap.trans_y)
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
        var canvas_element = $("<canvas>").attr({id: "canvas", width: width, height: height})
        // replace existing canvas element
        // Note: we can't call dm4c.split_panel.set_left_panel() here (-> endless recursion)
        $(".topicmap-renderer #canvas").remove()
        $(".topicmap-renderer").append(canvas_element)
        //
        // 2) initialize the 2D context
        // Note: the canvas element must be already on the page
        ctx = canvas_element.get(0).getContext("2d")
        ctx.font = LABEL_FONT   // the canvas font must be set early. Label measurement takes place *before* drawing.
        if (topicmap) { // ### TODO: refactor
            ctx.translate(topicmap.trans_x, topicmap.trans_y)
        }
        //
        bind_event_handlers(canvas_element)
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

    this.add_view_customizer = function(customizer_constructor) {
        add_view_customizer(customizer_constructor)
    }



    // ----------------------------------------------------------------------------------------------- Private Functions



    // *************
    // *** Model ***
    // *************



    function get_topic(id) {
        return topics[id]
    }

    function get_association(id) {
        return assocs[id]
    }

    // ---

    /**
     * @param   topic   A TopicViewmodel.
     */
    function add_topic(topic) {
        var topic_view = new TopicView(topic)
        topics[topic.id] = topic_view
        //
        DOM_FLAVOR && create_topic_dom(topic_view)          // topic layer DOM
        //
        invoke_customizers("on_update_topic", [topic_view, ctx])
        invoke_customizers("on_update_view_properties", [topic_view])
    }

    /**
     * @param   assoc   An AssociationViewmodel.
     */
    function add_association(assoc) {
        assocs[assoc.id] = new AssociationView(assoc)
    }

    // ---

    function iterate_topics(visitor_func) {
        for (var id in topics) {
            var ret = visitor_func(get_topic(id))
            if (ret) {
                return ret
            }
        }
    }

    function iterate_associations(visitor_func) {
        for (var id in assocs) {
            var ret = visitor_func(get_association(id))
            if (ret) {
                return ret
            }
        }
    }

    // ---

    function clear() {
        topics = {}
        assocs = {}
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
        //
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

    function draw_association(av) {
        var tv1 = av.get_topic_1()
        var tv2 = av.get_topic_2()
        // error check
        if (!tv1 || !tv2) {
            // TODO: deleted associations must be removed from all topicmaps.
            // ### alert("ERROR in draw_associations: association " + this.id + " is missing a topic")
            // ### delete assocs[i]
            return
        }
        //
        var color = dm4c.get_type_color(av.type_uri)
        draw_line(tv1.x, tv1.y, tv2.x, tv2.y, dm4c.ASSOC_WIDTH, color)
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



    // *********************
    // *** Customization ***
    // *********************



    default_view_customizer = new DefaultViewCustomizer()
    check_customizer(default_view_customizer)

    function add_view_customizer(customizer_constructor) {
        var canvas_view_facade = {
            get_topic:           get_topic,
            iterate_topics:      iterate_topics,
            update_topic:        update_topic,
            set_view_properties: set_view_properties,
            pos:                 pos
        }
        var customizer = new customizer_constructor(canvas_view_facade)
        if (check_customizer(customizer)) {
            view_customizers.push(customizer)
        }

        // ### compare to canvas_renderer.update_topic()
        function update_topic(topic) {
            // update viewmodel
            var topic_viewmodel = topicmap.update_topic(topic)  // ### TODO: update all topicmaps?
            // update view
            self.update_topic(topic_viewmodel)
        }

        function set_view_properties(topic_id, view_props) {
            // update viewmodel
            topicmap.set_view_properties(topic_id, view_props)
            // update view
            var topic_view = get_topic(topic_id)
            topic_view.set_view_properties(view_props)
            // render
            show()
            DOM_FLAVOR && position_topic_dom(topic_view)        // topic layer DOM
        }
    }

    function check_customizer(customizer) {
        if (detect_customizer_flavors(customizer)) {
            return true
        } else {
            console.log("CanvasViewWarning:", js.class_name(customizer), "is not a valid view customizer -- ignored")
        }
    }

    function detect_customizer_flavors(customizer) {
        var is_valid = false
        // ### TODO: adapt detector methods when framework progresses
        if (customizer.draw_topic) {
            CANVAS_FLAVOR = true
            is_valid = true
            log("CANVAS")
        }
        if (customizer.topic_dom) {
            DOM_FLAVOR = true
            is_valid = true
            log("DOM")
        }
        return is_valid

        function log(flavor) {
            console.log("CanvasView:", flavor, "flavor detected for view customizer", js.class_name(customizer))
        }
    }

    function customize_draw_topic(tv) {
        invoke_customizers("draw_topic", [tv, ctx])
    }

    /**
     * @param   args    array of arguments
     */
    function invoke_customizers(func_name, args) {
        var invoke_default = true
        for (var i = 0, customizer; customizer = view_customizers[i]; i++) {
            var func = customizer[func_name]
            if (!(func && func.apply(undefined, args))) {
                invoke_default = false
            }
        }
        if (invoke_default) {
            var func = default_view_customizer[func_name]
            func && func.apply(undefined, args)     // ### condition required?
        }
    }



    // **********************
    // *** Event Handling ***
    // **********************



    function bind_event_handlers(canvas_element) {
        canvas_element.bind("mousedown",   do_mousedown)
        canvas_element.bind("mouseup",     do_mouseup)
        canvas_element.bind("mousemove",   do_mousemove)
        canvas_element.bind("mouseleave",  do_mouseleave)
        canvas_element.bind("dblclick",    do_doubleclick)
        canvas_element.bind("contextmenu", do_contextmenu)
        canvas_element.bind("dragover",    do_dragover)
        canvas_element.bind("drop",        do_drop)
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
            var canvas_pos = pos(event)
            var topicmap_pos = pos(event, Coord.TOPICMAP)
            //
            tmp_x = canvas_pos.x
            tmp_y = canvas_pos.y
            //
            invoke_customizers("on_mousedown", [
                {canvas: canvas_pos, topicmap: topicmap_pos},
                {shift: event.shiftKey}
            ])
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
            // topics (including their childs) are prevented from ending an association in progress
            if ($(event.relatedTarget).closest(".topic", "#topic-layer").length == 0) {
                end_association_in_progress()
            }
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
            end_association_in_progress(detect_topic(event))
        } else if (action_topic) {
            dm4c.do_select_topic(action_topic.id)
            action_topic = null
        } else if (action_assoc) {
            dm4c.do_select_association(action_assoc.id)
            action_assoc = null
        }
    }

    function do_doubleclick(event) {
        var tv = detect_topic(event)
        if (tv) {
            dm4c.fire_event("topic_doubleclicked", tv)
        }
    }

    // ---

    function end_topic_move() {
        // update viewmodel
        topicmap.set_topic_position(action_topic.id, action_topic.x, action_topic.y)
        // Note: the view is already up-to-date. It is constantly updated while mouse dragging.
        //
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
        dm4c.fire_event("post_move_canvas", topicmap.trans_x, topicmap.trans_y)
        //
        canvas_move_in_progress = false
    }

    function end_association_in_progress(tv) {
        if (tv && tv.id != action_topic.id) {
            dm4c.do_create_association("dm4.core.association", action_topic.id, tv.id)
        }
        //
        association_in_progress = false
        action_topic = null
        show()
    }



    // === Context Menu Events ===

    function do_contextmenu(event) {
        // 1) assemble commands
        var tv, av
        if (tv = detect_topic(event)) {
            dm4c.do_select_topic(tv.id)
            // Note: only dm4c.selected_object has the composite value (the TopicView has not)
            var commands = dm4c.get_topic_commands(dm4c.selected_object, "context-menu")
        } else if (av = detect_association(event)) {
            dm4c.do_select_association(av.id)
            // Note: only dm4c.selected_object has the composite value (the AssociationView has not)
            var commands = dm4c.get_association_commands(dm4c.selected_object, "context-menu")
        } else {
            var p = pos(event, Coord.TOPICMAP)
            var commands = dm4c.get_canvas_commands(p.x, p.y, "context-menu")
        }
        // 2) show menu
        open_context_menu(commands, event)
        //
        return false
    }

    /**
     * Builds a context menu from a set of commands and shows it.
     *
     * @param   commands    Array of commands. May be empty. Must not null/undefined.
     * @param   event       The mouse event that triggered the context menu.
     */
    function open_context_menu(commands, event) {
        close_context_menu()
        //
        if (!commands.length) {
            return
        }
        // fire event (compare to GUITookit's open_menu())
        dm4c.pre_open_context_menu()
        //
        // "Opening context nenu: event.screenY=" + event.screenY +
        //    ", event.clientY=" + event.clientY + ", event.pageY=" + event.pageY +
        //    ", event.originalEvent.layerY=" + event.originalEvent.layerY
        var cm_pos = pos(event, Coord.WINDOW)
        var contextmenu = $("<div>").addClass("menu").css({
            top:  cm_pos.y,
            left: cm_pos.x
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
        $("#topicmap-panel").append(contextmenu)
        contextmenu.show()

        function context_menu_handler(handler) {
            return function(event) {
                // pass the coordinates of the command selecting mouse click to the command handler
                var p = pos(event)
                handler(p.x, p.y)
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
        $("#topicmap-panel .menu").remove()
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



    function detect_topic(event) {
        return detect_topic_at(pos(event, Coord.TOPICMAP))
    }

    function detect_association(event) {
        return detect_association_at(pos(event, Coord.TOPICMAP))
    }

    // ---

    /**
     * Detects the topic that is located at a given position.
     * Detection relies on the topic view's bounding box ("x1", "y1", "x2", "y2" properties).
     * Note: View Customizers are responsible for adding these properties to the topic view.
     *
     * @param   pos     an object with "x" and "y" properties. Coord.TOPICMAP space.
     *
     * @return  The detected topic, or undefined if no topic is located at the given position.
     */
    function detect_topic_at(pos) {
        return iterate_topics(function(tv) {
            if (pos.x >= tv.x1 && pos.x < tv.x2 && pos.y >= tv.y1 && pos.y < tv.y2) {
                return tv
            }
        })
    }

    /**
     * Detects the association that is located at a given position.
     *
     * @param   pos     an object with "x" and "y" properties. Coord.TOPICMAP space.
     *
     * @return  The detected association, or undefined if no association is located at the given position.
     */
    function detect_association_at(pos) {
        var x = pos.x
        var y = pos.y
        return iterate_associations(function(av) {
            var tv1 = av.get_topic_1()
            var tv2 = av.get_topic_2()
            // bounding box
            var aw2 = dm4c.ASSOC_WIDTH / 2   // buffer to make orthogonal associations selectable
            var bx1 = Math.min(tv1.x, tv2.x) - aw2
            var bx2 = Math.max(tv1.x, tv2.x) + aw2
            var by1 = Math.min(tv1.y, tv2.y) - aw2
            var by2 = Math.max(tv1.y, tv2.y) + aw2
            var in_bounding = x > bx1 && x < bx2 && y > by1 && y < by2
            if (!in_bounding) {
                return
            }
            // gradient
            var dx1 = x - tv1.x
            var dx2 = x - tv2.x
            var dy1 = y - tv1.y
            var dy2 = y - tv2.y
            if (bx2 - bx1 > by2 - by1) {
                var g1 = dy1 / dx1
                var g2 = dy2 / dx2
            } else {
                var g1 = dx1 / dy1
                var g2 = dx2 / dy2
            }
            //
            if (Math.abs(g1 - g2) < dm4c.ASSOC_CLICK_TOLERANCE) {
                return av
            }
        })
    }

    // ---

    /**
     * Interprets a mouse event according to a coordinate system.
     *
     * @param   coordinate_system   Optional: Coord.WINDOW, Coord.CANVAS (default), Coord.TOPICMAP
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
                x: event.clientX,
                y: event.clientY - $("#topicmap-panel").position().top
                // ### x: event.originalEvent.layerX,
                // ### y: event.originalEvent.layerY
            }
        case Coord.TOPICMAP:
            return {
                x: event.clientX - topicmap.trans_x,
                y: event.clientY - topicmap.trans_y - $("#topicmap-panel").position().top
                // ### x: event.originalEvent.layerX - topicmap.trans_x,
                // ### y: event.originalEvent.layerY - topicmap.trans_y
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
        topicmap.translate_by(dx, dy)               // Note: topicmap.translate_by() doesn't update the DB.
        // render
        ctx.translate(dx, dy)                       // canvas
        DOM_FLAVOR && update_translation_dom()      // topic layer DOM
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



    // ***********************
    // *** Topic Layer DOM ***
    // ***********************



    function create_topic_dom(topic_view) {

        var topic_dom = $("<div>").addClass("topic")
        topic_view.set_dom(topic_dom)
        invoke_customizers("topic_dom", [topic_view])
        $("#topic-layer").append(topic_dom)
        position_topic_dom(topic_view)
        invoke_customizers("topic_dom_appendix", [topic_view])
        add_event_handlers()
        configure_draggable_handle()

        function add_event_handlers() {
            var has_moved
            topic_dom
                .mousedown(function() {
                    close_context_menu()
                    has_moved = false
                })
                .mouseup(function() {
                    if (association_in_progress) {
                        end_association_in_progress(topic_view)
                    } else if (!has_moved) {
                        dm4c.do_select_topic(topic_view.id)
                    }
                })
                .contextmenu(function(event) {
                    dm4c.do_select_topic(topic_view.id)
                    // Note: only dm4c.selected_object has the composite value (the TopicView has not)
                    var commands = dm4c.get_topic_commands(dm4c.selected_object, "context-menu")
                    open_context_menu(commands, event)
                    return false
                })
            topic_dom.draggable({
                drag: function(event, ui) {
                    has_moved = true
                    // update view
                    update_topic_view(topic_view)
                    // render
                    show()
                },
                stop: function(event, ui) {
                    // update viewmodel
                    topicmap.set_topic_position(topic_view.id, topic_view.x, topic_view.y)
                }
            })
        }

        function configure_draggable_handle() {
            var handles = []
            invoke_customizers("topic_dom_draggable_handle", [topic_dom, handles])
            if (handles.length) {
                if (handles.length > 1) {
                    console.log("WARNING: more than one draggable handle provided by view customizers. " +
                        "Only the first is used.")
                }
                topic_dom.draggable("option", "handle", handles[0])
            }
        }
    }

    function update_selection_dom(topic_id) {
        // remove former selection
        remove_selection_dom()
        //
        // set new selection
        get_topic(topic_id).dom.addClass("selected")
        // The same via DOM traversal:
        // $("#topicmap-panel .topic#t-" + topic_id).addClass("selected")
        // Note: we don't store topic IDs in the DOM anymore.
        // Meanwhile we store the topic DOM in the TopicView object.
    }

    function remove_selection_dom() {
        $("#topicmap-panel .topic.selected").removeClass("selected")
        // Note: the topicmap viewmodel selection is already updated. So we can't get the formerly
        // selected topic ID and can't use get_topic(). So we do DOM traversal instead.
        // ### TODO: consider equipping the canvas view with a selection model.
    }

    function position_topic_dom(topic_view) {
        var s = topic_dom_size(topic_view.dom)
        topic_view.dom.css({
            top:  topic_view.y - s.height / 2,
            left: topic_view.x - s.width  / 2
        })
    }

    function remove_topic_dom(topic_view) {
        topic_view.dom.remove()
    }

    function update_translation_dom() {
        $("#topic-layer").css({         
            top:  topicmap.trans_y,
            left: topicmap.trans_x
        })
    }

    function empty_topic_layer() {
        $("#topic-layer").empty()
    }

    function update_topic_view(topic_view) {
        var topic_dom = topic_view.dom
        var p = topic_dom.position()
        var s = topic_dom_size(topic_dom)
        topic_view.move_to(
            Math.floor(p.left + s.width  / 2),  // for non-integers the update request results in 404
            Math.floor(p.top  + s.height / 2)
        )
    }

    function topic_dom_size(topic_dom) {
        return {
            width:  topic_dom.outerWidth(),
            height: topic_dom.outerHeight()
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Classes

    /**
     * Renders a topic as "icon + label".
     * The clickable area is the icon.
     * The label is truncated and line wrapped.
     */
    function DefaultViewCustomizer() {

        var LABEL_DIST_Y = 4        // in pixel



        // === Hook Implementations ===

        /**
         * Adds "x1", "y1", "x2", "y2" properties to the topic view. Click detection relies on this bounding box.
         * Adds "width" and "height" custom properties. Updated on topic update (label or type changed).
         * Adds "label_wrapper" custom property.        Updated on topic update (label or type changed).
         * Adds "label_pos_y" custom property.          Updated on topic move.
         *
         * @param   topic_view      A TopicView object.
         *                          Has "id", "type_uri", "label", "x", "y", "view_props", "dom" properties
         *                          plus the viewmodel-derived custom properties.
         */
        this.on_update_topic = function(topic_view, ctx) {
            sync_icon_and_label(topic_view, ctx)
            sync_geometry(topic_view)
        }

        this.on_move_topic = function(topic_view) {
            sync_geometry(topic_view)
        }

        // ---

        this.draw_topic = function(topic_view, ctx) {
            var tv = topic_view
            // 1) render icon
            // Note: the icon object is not hold in the topic view, but looked up every time. This saves us
            // from touching all topic view objects once a topic type's icon changes (via view configuration).
            // Icon lookup is supposed to be a cheap operation.
            var icon = dm4c.get_type_icon(tv.type_uri)
            ctx.drawImage(icon, tv.x1, tv.y1)
            // 2) render label
            tv.label_wrapper.draw(tv.x1, tv.label_pos_y, ctx)
            // Note: the context must be passed to every draw() call.
            // The context changes when the canvas is resized.
        }

        this.on_mousedown = function(pos, modifier) {
            var tv = detect_topic_at(pos.topicmap)
            if (tv) {
                if (!modifier.shift) {
                    action_topic = tv
                } else {
                    dm4c.do_select_topic(tv.id)
                    self.begin_association(tv.id, pos.canvas.x, pos.canvas.y)
                }
            } else {
                var av = detect_association_at(pos.topicmap)
                if (av) {
                    action_assoc = av
                } else {
                    canvas_move_in_progress = true
                }
            }
        }



        // === Private Methods ===

        function sync_icon_and_label(tv, ctx) {
            var icon = dm4c.get_type_icon(tv.type_uri)
            tv.width  = icon.width
            tv.height = icon.height
            //
            var label = js.truncate(tv.label, dm4c.MAX_TOPIC_LABEL_CHARS)
            tv.label_wrapper = new js.TextWrapper(label, dm4c.MAX_TOPIC_LABEL_WIDTH, 19, ctx)
            //                                                        // line height 19px = 1.2em
        }

        function sync_geometry(tv) {
            // bounding box
            tv.x1 = tv.x - tv.width / 2,
            tv.y1 = tv.y - tv.height / 2
            tv.x2 = tv.x1 + tv.width
            tv.y2 = tv.y1 + tv.height
            // label
            tv.label_pos_y = tv.y2 + LABEL_DIST_Y + 16    // 16px = 1em
        }
    }

    // ---

    /**
     * The generic topic view, to be enriched by view customizers.
     *
     * Properties:
     *  id, type_uri, label
     *  composite
     *  x, y                    Topic position.
     *  view_props
     *  x1, y1, x2, y2          Bounding box. Canvas click detection relies on these. To be added by view customizer.
     *                          Not needed for DOM based topic rendering.
     *
     * @param   topic   A TopicViewmodel.
     */
    function TopicView(topic) {

        var self = this

        this.id = topic.id
        this.x = topic.x
        this.y = topic.y
        this.view_props = topic.view_props

        init(topic)

        // ---

        this.set_dom = function(dom) {
            this.dom = dom
        }

        // ---

        this.move_to = function(x, y) {
            this.x = x
            this.y = y
            invoke_customizers("on_move_topic", [this])
        }

        this.move_by = function(dx, dy) {
            this.x += dx
            this.y += dy
            invoke_customizers("on_move_topic", [this])
        }

        /**
         * @param   topic   A TopicViewmodel.
         */
        this.update = function(topic) {
            init(topic)
            invoke_customizers("on_update_topic", [this, ctx])
        }

        this.set_view_properties = function(view_props) {
            // Note: the TopicView is already up-to-date. It is updated by viewmodel side effect.
            invoke_customizers("on_update_view_properties", [this])
        }

        // ---

        /**
         * @param   topic   A TopicViewmodel.
         */
        function init(topic) {
            self.type_uri  = topic.type_uri
            self.label     = topic.label
            self.composite = topic.composite
        }
    }

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

        var topics = []     // array of TopicView

        cluster.iterate_topics(function(topic) {
            topics.push(get_topic(topic.id))
        })

        this.move_by = function(dx, dy) {
            this.iterate_topics(function(topic) {
                // update view
                topic.move_by(dx, dy)
                // render
                DOM_FLAVOR && position_topic_dom(topic)     // topic layer DOM
            })
        }

        this.iterate_topics = function(visitor_func) {
            for (var i = 0, tv; tv = topics[i]; i++) {
                visitor_func(tv)
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
            iterate_topics(function(tv) {
                if (tv.y > max_y) {
                    max_y = tv.y
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
