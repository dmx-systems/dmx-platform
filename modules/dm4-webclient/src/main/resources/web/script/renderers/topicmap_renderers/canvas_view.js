/**
 * Default topicmap viewmodel: the data needed to render a topicmap. ### FIXDOC
 * 
 * Note: the default topicmap viewmodel is renderer technology agnostic.
 * It is shared by e.g. the HTML5 Canvas based default renderer and the SVG based 3rd-party renderer. ### FIXDOC
 *
 * ### TODO: refactoring.
 * ### This is not actually a viewmodel but the render technology-specific view, namely the HTML5 Canvas view.
 * ### All Canvas specific code (create, drawing, event handling, resize) should be move here (from CanvasRenderer).
 * ### The geometry management (find_topic() ...) and selection model (highlighting) on the other hand should
 * ### be moved to the viewmodel class, namely TopicmapViewmodel.
 * ### Note: the dm4-topicmaps module is no longer an optional install then. It is a Webclient dependency.
 */
CanvasView = function() {

    // View
    var canvas_topics = {}      // topics displayed on canvas (Object, key: topic ID, value: TopicView)
    var canvas_assocs = {}      // associations displayed on canvas (Object, key: assoc ID, value: AssociationView)

    // Viewmodel
    var topicmap                // the viewmodel underlying this view (a TopicmapViewmodel)

    // ### var highlight_mode = "none" // "none", "topic", "assoc"
    // ### var highlight_id            // ID of the highlighted topic/association. Ignored if highlight mode is "none".

    // ### this.trans_x = 0            // canvas translation (in pixel)
    // ### this.trans_y = 0            // canvas translation (in pixel)

    var self = this
    var ctx                     // the 2D context

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
        topicmap_viewmodel.iterate_topics(function(topic) {
            if (topic.visibility) {
                add_topic(topic)
            }
        })
        topicmap_viewmodel.iterate_associations(function(assoc) {
            add_association(assoc)
        })
    }

    this.set_context = function(_ctx) {
        ctx = _ctx
    }

    // ---

    this.get_topic = function(id) {
        return canvas_topics[id]
    }

    this.get_association = function(id) {
        return canvas_assocs[id]
    }

    // ---

    // ### TODO: move to viewmodel
    this.get_associations = function(topic_id) {
        var cas = []
        this.iterate_associations(function(ca) {
            if (ca.is_player_topic(topic_id)) {
                cas.push(ca)
            }
        })
        return cas
    }

    // ---

    /**
     * @param   topic   A TopicViewmodel.
     */
    this.add_topic = function(topic) {
        add_topic(topic)
    }

    /**
     * @param   assoc   An AssociationViewmodel.
     */
    this.add_association = function(assoc) {
        add_association(assoc)
    }

    // ---

    /**
     * @param   topic   A TopicViewmodel.
     */
    this.update_topic = function(topic) {
        this.get_topic(topic.id).update(topic)
    }

    /**
     * @param   assoc   An AssociationViewmodel.
     */
    this.update_association = function(assoc) {
        this.get_association(assoc.id).update(assoc)
    }

    // ---

    this.remove_topic = function(id) {
        // ### var ct = this.get_topic(id)
        delete canvas_topics[id]
        // ### return ct
    }

    this.remove_association = function(id) {
        // ### var ca = this.get_association(id)
        delete canvas_assocs[id]
        // ### return ca
    }

    // ---

    /* ### this.topic_exists = function(id) {
        return this.get_topic(id) != undefined
    }

    this.association_exists = function(id) {
        return this.get_association(id) != undefined
    } */

    // ---

    /**
     * @param   an object with "x" and "y" properties.
     */
    this.find_topic = function(pos) {
        return this.iterate_topics(function(ct) {
            if (pos.x >= ct.x - ct.width  / 2 && pos.x < ct.x + ct.width  / 2 &&
                pos.y >= ct.y - ct.height / 2 && pos.y < ct.y + ct.height / 2) {
                //
                return ct
            }
        })
    }

    /**
     * @param   an object with "x" and "y" properties.
     */
    this.find_association = function(pos) {
        var x = pos.x
        var y = pos.y
        return this.iterate_associations(function(ca) {
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
            // dm4c.log(g1 + " " + g2 + " -> " + Math.abs(g1 - g2))
            //
            if (Math.abs(g1 - g2) < dm4c.ASSOC_CLICK_TOLERANCE) {
                return ca
            }
        })
    }

    // ---

    this.iterate_topics = function(func) {
        for (var id in canvas_topics) {
            var ret = func(this.get_topic(id))
            if (ret) {
                return ret
            }
        }
    }

    this.iterate_associations = function(func) {
        for (var id in canvas_assocs) {
            var ret = func(this.get_association(id))
            if (ret) {
                return ret
            }
        }
    }

    // ---

    function clear() {
        canvas_topics = {}
        canvas_assocs = {}
        // ### this.reset_highlight()
    }



    // === Highlighting ===

    /* ### this.set_highlight_topic = function(topic_id) {
        highlight_mode = "topic"
        highlight_id = topic_id
    }

    this.set_highlight_association = function(assoc_id) {
        highlight_mode = "assoc"
        highlight_id = assoc_id
    }

    // ---

    this.reset_highlight = function() {
        highlight_mode = "none"
    }

    this.reset_highlight_conditionally = function(id) {
        if (this.has_highlight(id)) {
            this.reset_highlight()
        }
    }

    // ---

    this.has_highlight = function(id) {
        return this.highlight() && highlight_id == id
    } */

    /**
     * Returns true if there is a highlight.
     */
    /* this.highlight = function() {
        return highlight_mode != "none"
    } */

    // ---

    /**
     * Precondition: there is a highlight.
     *
     * @return  an object with "x" and "y" properties.
     */
    /* this.highlight_pos = function() {
        switch (highlight_mode) {
        case "topic":
            var ct = get_highlight_topic()
            return {x: ct.x, y: ct.y}
        case "assoc":
            var ca = get_highlight_association()
            var ct1 = ca.get_topic_1()
            var ct2 = ca.get_topic_2()
            return {
                x: (ct1.x + ct2.x) / 2,
                y: (ct1.y + ct2.y) / 2
            }
        }
    } */



    // === Translation ===

    /* this.translate_by = function(dx, dy) {
        this.trans_x += dx
        this.trans_y += dy
    } */



    // === Misc ===

    this.create_cluster = function(ca) {
        return new Cluster(ca)
    }



    // ----------------------------------------------------------------------------------------------- Private Functions

    /**
     * @param   topic   A TopicViewmodel.
     */
    function add_topic(topic) {
        canvas_topics[topic.id] = new TopicView(topic)
    }

    /**
     * @param   assoc   An AssociationViewmodel.
     */
    function add_association(assoc) {
        canvas_assocs[assoc.id] = new AssociationView(assoc)
    }

    // ---

    /**
     * Precondition: a topic is highlighted.
     */
    /* ### function get_highlight_topic() {
        return self.get_topic(highlight_id)
    } */

    /**
     * Precondition: an association is highlighted.
     */
    /* ### function get_highlight_association() {
        return self.get_association(highlight_id)
    } */



    // ------------------------------------------------------------------------------------------------- Private Classes

    /**
     * Properties:
     *  id, type_uri, label
     *  x, y                    Topic position. Represents the center of the topic's icon.
     *  width, height           Icon size.
     *  label_wrapper
     *
     * @param   topic   A TopicViewmodel.
     */
    function TopicView(topic) {

        var self = this

        this.id = topic.id
        this.x = topic.x
        this.y = topic.y

        init(topic);

        // ---

        this.move_to = function(x, y) {
            this.x = x
            this.y = y
        }

        this.move_by = function(dx, dy) {
            this.x += dx
            this.y += dy
        }

        // ---

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

    /**
     * Properties:
     *  id, type_uri
     *  topic_id_1, topic_id_2
     *
     * @param   assoc   An AssociationViewmodel.
     */
    function AssociationView(assoc) {

        var _self = this    // Note: self is already used in closure

        this.id = assoc.id
        this.topic_id_1 = assoc.topic_id_1
        this.topic_id_2 = assoc.topic_id_2

        init(assoc)

        // ---

        // ### needed?
        this.get_topic_1 = function() {
            return self.get_topic(id1())
        }

        // ### needed?
        this.get_topic_2 = function() {
            return self.get_topic(id2())
        }

        // ---

        // ### needed?
        this.is_player_topic = function(topic_id) {
            return id1() == topic_id || id2() == topic_id
        }

        // ### needed?
        this.get_other_topic = function(topic_id) {
            if (id1() == topic_id) {
                return this.get_topic_2()
            } else if (id2() == topic_id) {
                return this.get_topic_1()
            } else {
                throw "CanvasAssocError: topic " + topic_id + " is not a player in " + JSON.stringify(this)
            }
        }

        // ---

        /**
         * @param   assoc   An AssociationViewmodel.
         */
        this.update = function(assoc) {
            init(assoc)
        }

        // ---

        function id1() {
            return _self.topic_id_1
        }

        function id2() {
            return _self.topic_id_2
        }

        // ---

        function init(assoc) {
            _self.type_uri = assoc.type_uri
        }
    }

    // ---

    function Cluster(ca) {

        var cts = []    // array of TopicView

        add_to_cluster(ca.get_topic_1())

        this.move_by = function(dx, dy) {
            this.iterate_topics(function(ct) {
                ct.move_by(dx, dy)
            })
        }

        this.iterate_topics = function(visitor_func) {
            for (var i = 0, ct; ct = cts[i]; i++) {
                visitor_func(ct)
            }
        }

        function add_to_cluster(ct) {
            if (is_in_cluster(ct)) {
                return
            }
            //
            cts.push(ct)
            var cas = self.get_associations(ct.id)
            for (var i = 0, ca; ca = cas[i]; i++) {
                add_to_cluster(ca.get_other_topic(ct.id))
            }
        }

        function is_in_cluster(ct) {
            return js.includes(cts, function(cat) {
                return cat.id == ct.id
            })
        }
    }
}
