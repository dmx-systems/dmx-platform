/**
 * A topicmap model that is attached to the database. There are methods for:
 *  - loading a topicmap from DB
 *  - manipulating the topicmap by e.g. adding/removing topics and associations
 *
 * ### TODO: introduce common base class for Topicmap and Geomap (see external deepamehta-geomaps module)
 */
function Topicmap(topicmap_id, config) {

    var LOG_TOPICMAPS = false
    var self = this

    var info                // The underlying Topicmap topic (a Topic object)
    var topics = {}         // topics of this topicmap (key: topic ID, value: TopicmapTopic object)
    var assocs = {}         // associations of this topicmap (key: association ID, value: TopicmapAssociation object)
    this.trans_x, this.trans_y      // topicmap translation (in pixel)
    this.selected_object_id = -1    // ID of the selected topic or association, or -1 for no selection
    this.is_topic_selected          // true indicates topic selection, false indicates association selection
                                    // only evaluated if there is a selection (selected_object_id != -1)
    this.background_image   // JavaScript Image object

    load()



    // ------------------------------------------------------------------------------------------------------ Public API

    this.get_id = function() {
        return topicmap_id
    }

    this.get_renderer_uri = function() {
        return info.get("dm4.topicmaps.canvas_renderer_uri")
    }

    this.get_topic = function(id) {
        return topics[id]
    }

    this.iterate_topics = function(visitor_func) {
        iterate_topics(visitor_func)
    }

    this.iterate_associations = function(visitor_func) {
        iterate_associations(visitor_func)
    }

    this.add_topic = function(id, type_uri, label, x, y) {
        var topic = topics[id]
        if (!topic) {
            if (LOG_TOPICMAPS) dm4c.log("Adding topic " + id + " (\"" + label + "\") to topicmap " + topicmap_id)
            // update DB
            var response = dm4c.restc.add_topic_to_topicmap(topicmap_id, id, x, y)
            if (LOG_TOPICMAPS) dm4c.log("..... => ref ID " + response.ref_id)
            // update memory
            topics[id] = new TopicmapTopic(id, type_uri, label, x, y, true, response.ref_id)
        } else if (!topic.visibility) {
            if (LOG_TOPICMAPS)
                dm4c.log("Showing topic " + id + " (\"" + topic.label + "\") on topicmap " + topicmap_id)
            topic.show()
        } else {
            if (LOG_TOPICMAPS)
                dm4c.log("Topic " + id + " (\"" + label + "\") already visible in topicmap " + topicmap_id)
        }
    }

    this.add_association = function(id, type_uri, topic_id_1, topic_id_2) {
        if (!assocs[id]) {
            if (LOG_TOPICMAPS) dm4c.log("Adding association " + id + " to topicmap " + topicmap_id)
            // update DB
            var response = dm4c.restc.add_association_to_topicmap(topicmap_id, id)
            if (LOG_TOPICMAPS) dm4c.log("..... => ref ID " + response.ref_id)
            // update memory
            assocs[id] = new TopicmapAssociation(id, type_uri, topic_id_1, topic_id_2, response.ref_id)
        } else {
            if (LOG_TOPICMAPS) dm4c.log("Association " + id + " already in topicmap " + topicmap_id)
        }
    }

    this.move_topic = function(id, x, y) {
        var topic = topics[id]
        if (LOG_TOPICMAPS) dm4c.log("Moving topic " + id + " (\"" + topic.label + "\") on topicmap " + topicmap_id
            + " to x=" + x + ", y=" + y)
        topic.move_to(x, y)
    }

    this.hide_topic = function(id) {
        var topic = topics[id]
        if (LOG_TOPICMAPS) dm4c.log("Hiding topic " + id + " (\"" + topic.label + "\") from topicmap " +
            topicmap_id)
        topic.hide()
    }

    this.hide_association = function(id) {
        var assoc = assocs[id]
        if (LOG_TOPICMAPS) dm4c.log("Hiding association " + id + " from topicmap " + topicmap_id)
        assoc.hide()
    }

    /**
     * @param   topic   a Topic object
     */
    this.update_topic = function(topic) {
        var t = topics[topic.id]
        if (t) {
            if (LOG_TOPICMAPS) dm4c.log("..... Updating topic " + t.id + " (\"" + t.label + "\") on topicmap " +
                topicmap_id)
            t.update(topic)
        }
    }

    /**
     * @param   assoc   an Association object
     */
    this.update_association = function(assoc) {
        var a = assocs[assoc.id]
        if (a) {
            if (LOG_TOPICMAPS) dm4c.log("..... Updating association " + a.id + " on topicmap " + topicmap_id)
            a.update(assoc)
        }
    }

    this.delete_topic = function(id) {
        var topic = topics[id]
        if (topic) {
            if (LOG_TOPICMAPS) dm4c.log("..... Deleting topic " + id + " (\"" + topic.label + "\") from topicmap " +
                topicmap_id)
            topic.remove()
        }
    }

    this.delete_association = function(id) {
        var assoc = assocs[id]
        if (assoc) {
            if (LOG_TOPICMAPS) dm4c.log("..... Deleting association " + id + " from topicmap " + topicmap_id)
            assoc.remove()
        }
    }

    this.set_topic_selection = function(topic) {
        this.selected_object_id = topic.id
        this.is_topic_selected = true
    }

    this.set_association_selection = function(assoc) {
        this.selected_object_id = assoc.id
        this.is_topic_selected = false
    }

    this.reset_selection = function() {
        this.selected_object_id = -1
    }

    this.prepare_topic_for_display = function(topic) {
        // restores topic position if topic is already contained in this topicmap but hidden
        var t = this.get_topic(topic.id)
        if (t && !t.visibility) {
            topic.x = t.x
            topic.y = t.y
        }
    }

    this.set_translation = function(trans_x, trans_y) {
        // update memory
        this.trans_x = trans_x
        this.trans_y = trans_y
        // update DB
        if (is_writable()) {
            dm4c.restc.set_topicmap_translation(topicmap_id, trans_x, trans_y)
        }
    }

    this.draw_background = function(ctx) {
        if (this.background_image) {
            ctx.drawImage(this.background_image, 0, 0)
        }
    }



    // ----------------------------------------------------------------------------------------------- Private Functions

    function load() {

        if (LOG_TOPICMAPS) dm4c.log("Loading topicmap " + topicmap_id)

        var topicmap = dm4c.restc.get_topicmap(topicmap_id)
        info = new Topic(topicmap.info)

        if (LOG_TOPICMAPS) dm4c.log("..... " + topicmap.topics.length + " topics")
        init_topics()

        if (LOG_TOPICMAPS) dm4c.log("..... " + topicmap.assocs.length + " associations")
        init_associations()

        init_translation()
        init_background_image()

        function init_topics() {
            for (var i = 0, topic; topic = topicmap.topics[i]; i++) {
                var x = topic.visualization["dm4.topicmaps.x"].value
                var y = topic.visualization["dm4.topicmaps.y"].value
                var visibility = topic.visualization["dm4.topicmaps.visibility"].value
                if (LOG_TOPICMAPS) dm4c.log(".......... ID " + topic.id + ": type_uri=\"" + topic.type_uri +
                    "\", label=\"" + topic.value + "\", x=" + x + ", y=" + y + ", visibility=" + visibility +
                    ", ref_id=" + topic.ref_id)
                topics[topic.id] = new TopicmapTopic(topic.id, topic.type_uri, topic.value, x, y, visibility,
                    topic.ref_id)
            }
        }

        function init_associations() {
            for (var i = 0, assoc; assoc = topicmap.assocs[i]; i++) {
                if (LOG_TOPICMAPS) dm4c.log(".......... ID " + assoc.id + ": type_uri=\"" + assoc.type_uri +
                    "\", topic_id_1=" + assoc.role_1.topic_id + ", topic_id_2=" + assoc.role_2.topic_id +
                    ", ref_id=" + assoc.ref_id)
                assocs[assoc.id] = new TopicmapAssociation(assoc.id, assoc.type_uri,
                    assoc.role_1.topic_id, assoc.role_2.topic_id, assoc.ref_id)
            }
        }

        // ---

        function init_translation() {
            var trans = info.get("dm4.topicmaps.state").get("dm4.topicmaps.translation")
            self.trans_x = trans.get("dm4.topicmaps.translation_x")
            self.trans_y = trans.get("dm4.topicmaps.translation_y")
        }

        function init_background_image() {
            var file = info.get("dm4.files.file")
            if (file) {
                var image_url = "/proxy/file:" + file.get("dm4.files.path")
                self.background_image = dm4c.create_image(image_url)
            }
        }
    }

    // ---

    function iterate_topics(visitor_func) {
        for (var id in topics) {
            visitor_func(topics[id])
        }
    }

    function iterate_associations(visitor_func) {
        for (var id in assocs) {
            visitor_func(assocs[id])
        }
    }

    // ---

    function is_writable() {
        return config.is_writable()
    }



    // ------------------------------------------------------------------------------------------------- Private Classes

    function TopicmapTopic(id, type_uri, label, x, y, visibility, ref_id) {

        this.id = id
        this.type_uri = type_uri
        this.label = label      // ### FIXME: rename to "value"
        this.x = x
        this.y = y
        this.visibility = visibility
        this.ref_id = ref_id    // ID of the "dm4.topicmaps.topic_mapcontext" association
                                // that is used by the topicmap to reference this topic.
                                // ### FIXME: the ref_id should be removed from the client-side model.
                                // ### TODO: extend topicmaps REST API instead of exposing internals.

        var _self = this

        this.show = function() {
            set_visibility(true)
        }

        this.hide = function() {
            set_visibility(false)
            reset_selection()
        }

        this.move_to = function(x, y) {
            // update DB ### TODO: extend topicmaps REST API instead of operating on the DB directly
            dm4c.restc.update_association({id: ref_id, composite: {"dm4.topicmaps.x": x, "dm4.topicmaps.y": y}})
            // update memory
            this.x = x
            this.y = y
        }

        /**
         * @param   topic   a Topic object
         */
        this.update = function(topic) {
            this.label = topic.value
        }

        this.remove = function() {
            // Note: all topic references are deleted already
            delete topics[id]
            reset_selection()
        }

        // ---

        function set_visibility(visibility) {
            // update DB ### TODO: extend topicmaps REST API instead of operating on the DB directly
            dm4c.restc.update_association({id: ref_id, composite: {"dm4.topicmaps.visibility": visibility}})
            // update memory
            _self.visibility = visibility
        }

        function reset_selection() {
            if (self.is_topic_selected && self.selected_object_id == id) {
                self.selected_object_id = -1
            }
        }
    }

    function TopicmapAssociation(id, type_uri, topic_id_1, topic_id_2, ref_id) {

        this.id = id
        this.type_uri = type_uri
        this.topic_id_1 = topic_id_1
        this.topic_id_2 = topic_id_2
        this.ref_id = ref_id    // ID of the "dm4.topicmaps.association_mapcontext" association
                                // that is used by the topicmap to reference this association.
                                // ### FIXME: the ref_id should be removed from the client-side model.
                                // ### TODO: extend topicmaps REST API instead of exposing internals.

        this.hide = function() {
            // update DB
            dm4c.restc.remove_association_from_topicmap(topicmap_id, id, ref_id)
            // update memory
            delete assocs[id]
            reset_selection()
        }

        /**
         * @param   assoc   an Association object
         */
        this.update = function(assoc) {
            this.type_uri = assoc.type_uri
        }

        this.remove = function() {
            delete assocs[id]
            reset_selection()
        }

        // ---

        function reset_selection() {
            if (!self.is_topic_selected && self.selected_object_id == id) {
                self.selected_object_id = -1
            }
        }
    }
}
