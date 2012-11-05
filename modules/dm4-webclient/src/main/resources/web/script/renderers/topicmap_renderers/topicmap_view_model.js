DefaultTopicmapRenderer.Model = function () {

    var canvas_topics = {}      // topics displayed on canvas (Object, key: topic ID, value: CanvasTopic)
    var canvas_assocs = {}      // associations displayed on canvas (Object, key: assoc ID, value: CanvasAssoc)

    var highlight_mode = "none" // "none", "topic", "assoc"
    var highlight_id            // ID of the highlighted topic/association. Ignored if highlight mode is "none".

    this.trans_x = 0            // canvas translation (in pixel)
    this.trans_y = 0            // canvas translation (in pixel)

    var self = this
    var ctx                     // the 2D context ### FIXME: remove from model

    // ------------------------------------------------------------------------------------------------------ Public API

    this.get_topic = function(id) {
        return canvas_topics[id]
    }

    this.get_association = function(id) {
        return canvas_assocs[id]
    }

    // ---

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
     * @param   topic   an object with "id", "type_uri", "value", "x", "y" properties.
     */
    this.add_topic = function(topic) {
        canvas_topics[topic.id] = new CanvasTopic(topic)
    }

    /**
     * @param   assoc   an object with "id", "type_uri", "role_1", "role_2" properties.
     */
    this.add_association = function(assoc) {
        canvas_assocs[assoc.id] = new CanvasAssoc(assoc)
    }

    // ---

    this.remove_topic = function(id) {
        var ct = this.get_topic(id)
        delete canvas_topics[id]
        return ct
    }

    this.remove_association = function(id) {
        var ca = this.get_association(id)
        delete canvas_assocs[id]
        return ca
    }

    // ---

    this.topic_exists = function(id) {
        return this.get_topic(id) != undefined
    }

    this.association_exists = function(id) {
        return this.get_association(id) != undefined
    }

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

    this.clear = function() {
        canvas_topics = {}
        canvas_assocs = {}
        this.reset_highlight()
    }



    // === Highlighting ===

    this.set_highlight_topic = function(topic_id) {
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
    }

    /**
     * Returns true if there is a highlight.
     */
    this.highlight = function() {
        return highlight_mode != "none"
    }

    // ---

    /**
     * Precondition: there is a highlight.
     *
     * @return  an object with "x" and "y" properties.
     */
    this.highlight_pos = function() {
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
    }



    // === Translation ===

    this.translate_by = function(dx, dy) {
        this.trans_x += dx
        this.trans_y += dy
    }



    // === Misc ===

    this.create_cluster = function(ca) {
        return new Cluster(ca)
    }

    // ### FIXME: remove context from model
    this.setContext = function(_ctx) {
        ctx = _ctx
    }



    // ----------------------------------------------------------------------------------------------- Private Functions

    /**
     * Precondition: a topic is highlighted.
     */
    function get_highlight_topic() {
        return self.get_topic(highlight_id)
    }

    /**
     * Precondition: an association is highlighted.
     */
    function get_highlight_association() {
        return self.get_association(highlight_id)
    }



    // ------------------------------------------------------------------------------------------------- Private Classes

    /**
     * Properties:
     *  id, type_uri, label
     *  x, y                    Topic position. Represents the center of the topic's icon.
     *  width, height           Icon size.
     *  label_wrapper
     *
     * @param   topic   an object with "id", "type_uri", "value", "x", "y" properties.
     */
    function CanvasTopic(topic) {

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

        this.update = function(topic) {
            init(topic)
        }

        // ---

        function init(topic) {
            self.type_uri = topic.type_uri
            self.label    = topic.value
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
     *  role_1, role_2
     *
     * @param   assoc   an object with "id", "type_uri", "role_1", "role_2" properties.
     */
    function CanvasAssoc(assoc) {

        var _self = this    // Note: self is already used in closure

        this.id = assoc.id
        this.role_1 = assoc.role_1
        this.role_2 = assoc.role_2

        init(assoc)

        // ---

        this.get_topic_1 = function() {
            return self.get_topic(id1())
        }

        this.get_topic_2 = function() {
            return self.get_topic(id2())
        }

        // ---

        this.is_player_topic = function(topic_id) {
            return id1() == topic_id || id2() == topic_id
        }

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

        this.update = function(assoc) {
            init(assoc)
        }

        // ---

        function id1() {
            return _self.role_1.topic_id
        }

        function id2() {
            return _self.role_2.topic_id
        }

        // ---

        function init(assoc) {
            _self.type_uri = assoc.type_uri
        }
    }

    // ---

    function Cluster(ca) {

        var cts = []    // array of CanvasTopic

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
