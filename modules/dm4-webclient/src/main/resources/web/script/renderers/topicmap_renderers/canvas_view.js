/**
 * HTML5 Canvas based topicmap view.
 *
 * ### TODO: refactoring.
 * ### All Canvas specific code (create, drawing, event handling, resize) should be move here (from CanvasRenderer).
 * ### The geometry management (find_topic() ...) and selection model (highlighting) on the other hand should
 * ### be moved to the viewmodel class, namely TopicmapViewmodel.
 */
CanvasView = function() {

    // View
    var canvas_topics = {}      // topics displayed on canvas (Object, key: topic ID, value: TopicView)
    var canvas_assocs = {}      // associations displayed on canvas (Object, key: assoc ID, value: AssociationView)

    // Viewmodel
    var topicmap                // the viewmodel underlying this view (a TopicmapViewmodel)

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
        topicmap.iterate_topics(function(topic) {
            if (topic.visibility) {
                add_topic(topic)
            }
        })
        topicmap.iterate_associations(function(assoc) {
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
        delete canvas_topics[id]
    }

    this.remove_association = function(id) {
        delete canvas_assocs[id]
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

    /**
     * @param   cluster     A ClusterViewmodel.
     */
    this.create_cluster = function(cluster) {
        return new ClusterView(cluster)
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

    function clear() {
        canvas_topics = {}
        canvas_assocs = {}
    }



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

    /**
     * @param   cluster     A ClusterViewmodel
     */
    function ClusterView(cluster) {

        var topics = []

        cluster.iterate_topics(function(topic) {
            topics.push(self.get_topic(topic.id))
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
}
