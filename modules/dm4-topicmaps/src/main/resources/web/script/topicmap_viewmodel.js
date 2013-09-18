/**
 * A topicmap viewmodel that is attached to the database. There are methods for:
 *  - loading a topicmap from DB
 *  - manipulating the topicmap by e.g. adding/removing topics and associations
 *
 * ### TODO: introduce common base class for TopicmapViewmodel and GeomapViewmodel (see dm4-geomaps module)
 */
function TopicmapViewmodel(topicmap_id, config) {

    var LOG_TOPICMAPS = false
    var self = this

    var info                // The underlying Topicmap topic (a Topic object)
    var topics = {}         // topics of this topicmap (key: topic ID, value: TopicViewmodel object)
    var assocs = {}         // associations of this topicmap (key: association ID, value: AssociationViewmodel object)

    // Translation model
    this.trans_x, this.trans_y      // topicmap translation (in pixel)

    // Selection model
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
        return info.get("dm4.topicmaps.topicmap_renderer_uri")
    }

    // ---

    this.get_topic = function(id) {
        return topics[id]
    }

    this.get_association = function(id) {
        return assocs[id]
    }

    // ---

    this.topic_exists = function(id) {
        return this.get_topic(id) != undefined
    }

    this.association_exists = function(id) {
        return this.get_association(id) != undefined
    }

    // ---

    this.iterate_topics = function(visitor_func) {
        iterate_topics(visitor_func)
    }

    this.iterate_associations = function(visitor_func) {
        iterate_associations(visitor_func)
    }

    // ---

    this.add_topic = function(id, type_uri, label, x, y) {
        var topic = topics[id]
        if (!topic) {
            if (LOG_TOPICMAPS) dm4c.log("Adding topic " + id + " (\"" + label + "\") to topicmap " + topicmap_id)
            // update viewmodel
            topics[id] = new TopicViewmodel(id, type_uri, label, x, y, true)     // visibility=true
            // update DB
            if (is_writable()) {
                dm4c.restc.add_topic_to_topicmap(topicmap_id, id, x, y)
            }
            //
            return topics[id]
        } else if (!topic.visibility) {
            if (LOG_TOPICMAPS)
                dm4c.log("Showing topic " + id + " (\"" + topic.label + "\") on topicmap " + topicmap_id)
            // update memory
            topic.show()
            // update DB
            if (is_writable()) {
                dm4c.restc.set_topic_visibility(topicmap_id, id, true)
            }
        } else {
            if (LOG_TOPICMAPS)
                dm4c.log("Topic " + id + " (\"" + label + "\") already visible in topicmap " + topicmap_id)
        }
    }

    this.add_association = function(id, type_uri, topic_id_1, topic_id_2) {
        if (!assocs[id]) {
            if (LOG_TOPICMAPS) dm4c.log("Adding association " + id + " to topicmap " + topicmap_id)
            // update memory
            assocs[id] = new AssociationViewmodel(id, type_uri, topic_id_1, topic_id_2)
            // update DB
            if (is_writable()) {
                dm4c.restc.add_association_to_topicmap(topicmap_id, id)
            }
            //
            return assocs[id]
        } else {
            if (LOG_TOPICMAPS) dm4c.log("Association " + id + " already in topicmap " + topicmap_id)
        }
    }

    // ---

    this.move_topic = function(id, x, y) {
        var topic = topics[id]
        if (LOG_TOPICMAPS) dm4c.log("Moving topic " + id + " (\"" + topic.label + "\") on topicmap " + topicmap_id
            + " to x=" + x + ", y=" + y)
        // update memory
        topic.move_to(x, y)
        // update DB
        if (is_writable()) {
            dm4c.restc.move_topic(topicmap_id, id, x, y)
        }
    }

    // ---

    this.hide_topic = function(id) {
        var topic = topics[id]
        if (LOG_TOPICMAPS) dm4c.log("Hiding topic " + id + " (\"" + topic.label + "\") from topicmap " + topicmap_id)
        // update memory
        topic.hide()
        // update DB
        if (is_writable()) {
            dm4c.restc.set_topic_visibility(topicmap_id, id, false)
        }
    }

    this.hide_association = function(id) {
        var assoc = assocs[id]
        if (LOG_TOPICMAPS) dm4c.log("Hiding association " + id + " from topicmap " + topicmap_id)
        // update memory
        assoc.hide()
        // update DB
        if (is_writable()) {
            dm4c.restc.remove_association_from_topicmap(topicmap_id, id)
        }
    }

    // ---

    /**
     * @param   topic   A Topic object.
     */
    this.update_topic = function(topic) {
        var t = topics[topic.id]
        if (t) {
            if (LOG_TOPICMAPS) dm4c.log("..... Updating topic " + t.id + " (\"" + t.label + "\") on topicmap " +
                topicmap_id)
            // update memory
            t.update(topic)
            // Note: no DB update here. A topic update doesn't affect the view data.
            //
            return t
        }
    }

    /**
     * @param   assoc   An Association object.
     */
    this.update_association = function(assoc) {
        var a = assocs[assoc.id]
        if (a) {
            if (LOG_TOPICMAPS) dm4c.log("..... Updating association " + a.id + " on topicmap " + topicmap_id)
            // update memory
            a.update(assoc)
            // Note: no DB update here. An association update doesn't affect the view data.
            //
            return a
        }
    }

    // ---

    this.delete_topic = function(id) {
        var topic = topics[id]
        if (topic) {
            if (LOG_TOPICMAPS) dm4c.log("..... Deleting topic " + id + " (\"" + topic.label + "\") from topicmap " +
                topicmap_id)
            // update memory
            topic.remove()
            // Note: no DB update here. The persisted view is already up-to-date (view data is stored in association).
        }
    }

    this.delete_association = function(id) {
        var assoc = assocs[id]
        if (assoc) {
            if (LOG_TOPICMAPS) dm4c.log("..... Deleting association " + id + " from topicmap " + topicmap_id)
            // update memory
            assoc.remove()
            // Note: no DB update here. The persisted view is already up-to-date (view data is stored in association).
        }
    }

    // ---

    this.set_topic_selection = function(topic_id) {         // ### param was "topic"
        // update memory
        this.selected_object_id = topic_id
        this.is_topic_selected = true
        // Note: no DB update here. The selection is not yet persisted.
    }

    this.set_association_selection = function(assoc_id) {   // ### param was "assoc"
        // update memory
        this.selected_object_id = assoc_id
        this.is_topic_selected = false
        // Note: no DB update here. The selection is not yet persisted.
    }

    this.reset_selection = function() {
        // update memory
        this.selected_object_id = -1
        // Note: no DB update here. The selection is not yet persisted.
    }

    // ---

    /**
     * @param   id      A topic ID or an association ID
     */
    this.is_selected = function(id) {
        return this.has_selection() && this.selected_object_id == id
    }

    /**
     * Returns true if there is a selection.
     */
    this.has_selection = function() {
        return this.selected_object_id != -1
    }

    /**
     * Precondition: there is a selection.
     *
     * @return  an object with "x" and "y" properties.
     */
    this.get_selection_pos = function() {
        if (this.is_topic_selected) {
            var topic = this.get_topic(this.selected_object_id)
            return {x: topic.x, y: topic.y}
        } else {
            var assoc = this.get_association(this.selected_object_id)
            var topic_1 = assoc.get_topic_1()
            var topic_2 = assoc.get_topic_2()
            return {
                x: (topic_1.x + topic_2.x) / 2,
                y: (topic_1.y + topic_2.y) / 2
            }
        }
    }

    // ---

    this.prepare_topic_for_display = function(topic) {
        // restores topic position if topic is already contained in this topicmap but hidden
        var t = this.get_topic(topic.id)
        if (t && !t.visibility) {
            topic.x = t.x
            topic.y = t.y
        }
    }

    this.move_cluster = function(cluster) {
        // update memory
        cluster.iterate_topics(function(ct) {
            topics[ct.id].move_to(ct.x, ct.y)
        })
        // update DB
        if (is_writable()) {
            dm4c.restc.move_cluster(topicmap_id, cluster_coords())
        }

        function cluster_coords() {
            var coord = []
            cluster.iterate_topics(function(ct) {
                coord.push({
                    topic_id: ct.id,
                    x: ct.x,
                    y: ct.y
                })
            })
            return coord
        }
    }

    // ---

    this.set_translation = function(trans_x, trans_y) {
        // update memory
        this.trans_x = trans_x
        this.trans_y = trans_y
        // update DB
        if (is_writable()) {
            dm4c.restc.set_topicmap_translation(topicmap_id, trans_x, trans_y)
        }
    }

    this.translate_by = function(dx, dy) {
        this.trans_x += dx
        this.trans_y += dy
        // ### FIXME: persistence?
    }

    // ---

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
                    "\", label=\"" + topic.value + "\", x=" + x + ", y=" + y + ", visibility=" + visibility)
                topics[topic.id] = new TopicViewmodel(topic.id, topic.type_uri, topic.value, x, y, visibility)
            }
        }

        function init_associations() {
            for (var i = 0, assoc; assoc = topicmap.assocs[i]; i++) {
                if (LOG_TOPICMAPS) dm4c.log(".......... ID " + assoc.id + ": type_uri=\"" + assoc.type_uri +
                    "\", topic_id_1=" + assoc.role_1.topic_id + ", topic_id_2=" + assoc.role_2.topic_id)
                assocs[assoc.id] = new AssociationViewmodel(assoc.id, assoc.type_uri,
                    assoc.role_1.topic_id, assoc.role_2.topic_id)
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
                var image_url = "/filerepo/" + file.get("dm4.files.path")
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
        return config.is_writable
    }



    // ------------------------------------------------------------------------------------------------- Private Classes

    function TopicViewmodel(id, type_uri, label, x, y, visibility) {

        this.id = id
        this.type_uri = type_uri
        this.label = label      // ### FIXME: rename to "value"
        this.x = x
        this.y = y
        this.visibility = visibility

        this.show = function() {
            this.visibility = true
        }

        this.hide = function() {
            this.visibility = false
            reset_selection()
        }

        this.move_to = function(x, y) {
            this.x = x
            this.y = y
        }

        /**
         * @param   topic   a Topic object
         */
        this.update = function(topic) {
            this.type_uri = topic.type_uri
            this.label = topic.value
        }

        this.remove = function() {
            // Note: all topic references are deleted already
            delete topics[id]
            reset_selection()
        }

        // ---

        function reset_selection() {
            if (self.is_topic_selected && self.selected_object_id == id) {
                self.selected_object_id = -1
            }
        }
    }

    function AssociationViewmodel(id, type_uri, topic_id_1, topic_id_2) {

        this.id = id
        this.type_uri = type_uri
        this.topic_id_1 = topic_id_1
        this.topic_id_2 = topic_id_2

        // ---

        this.get_topic_1 = function() {
            return self.get_topic(this.topic_id_1)
        }

        this.get_topic_2 = function() {
            return self.get_topic(this.topic_id_2)
        }

        // ---

        this.hide = function() {
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
