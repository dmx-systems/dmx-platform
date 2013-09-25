/**
 * A topicmap viewmodel that is attached to the database. There are methods for:
 *  - loading a topicmap from DB
 *  - manipulating the topicmap by e.g. adding/removing topics and associations
 *
 * ### TODO: introduce common base class for TopicmapViewmodel and GeomapViewmodel (see dm4-geomaps module)
 */
function TopicmapViewmodel(topicmap_id, config) {

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
            // update memory
            topic = new TopicViewmodel(id, type_uri, label, x, y, true)     // visibility=true
            topics[id] = topic
            // update DB
            if (is_writable()) {
                dm4c.restc.add_topic_to_topicmap(topicmap_id, id, x, y)
            }
            //
            return topic
        } else if (!topic.visibility) {
            // update memory
            topic.set_visibility(true)
            // update DB
            if (is_writable()) {
                dm4c.restc.set_topic_visibility(topicmap_id, id, true)
            }
            //
            return topic
        } else {
            // topic already visible in topicmap
        }
    }

    this.add_association = function(id, type_uri, topic_id_1, topic_id_2) {
        var assoc = assocs[id]
        if (!assoc) {
            // update memory
            assoc = new AssociationViewmodel(id, type_uri, topic_id_1, topic_id_2)
            assocs[id] = assoc
            // update DB
            if (is_writable()) {
                dm4c.restc.add_association_to_topicmap(topicmap_id, id)
            }
            //
            return assoc
        } else {
            // association already in topicmap
        }
    }

    // ---

    this.set_topic_position = function(id, x, y) {
        var topic = topics[id]
        // update memory
        topic.set_position(x, y)
        // update DB
        if (is_writable()) {
            dm4c.restc.set_topic_position(topicmap_id, id, x, y)
        }
    }

    // ---

    this.hide_topic = function(id) {
        var topic = topics[id]
        // update memory
        topic.set_visibility(false)
        // update DB
        if (is_writable()) {
            dm4c.restc.set_topic_visibility(topicmap_id, id, false)
        }
    }

    this.hide_association = function(id) {
        var assoc = assocs[id]
        // update memory
        assoc.delete()  // Note: a hidden association is removed from the viewmodel, just like a deleted association
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
            // update memory
            topic.delete()
            // Note: no DB update here. The persisted view is already up-to-date (view data is stored in association).
        }
    }

    this.delete_association = function(id) {
        var assoc = assocs[id]
        if (assoc) {
            // update memory
            assoc.delete()
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
        // Note: the DB is not updated here.
        // This method is called repeatedly while moving the canvas.
    }

    // ---

    this.get_topic_associations = function(topic_id) {
        var cas = []
        iterate_associations(function(ca) {
            if (ca.is_player_topic(topic_id)) {
                cas.push(ca)
            }
        })
        return cas
    }

    // ---

    this.create_cluster = function(ca) {
        return new ClusterViewmodel(ca)
    }

    this.set_cluster_position = function(cluster) {
        // update memory
        cluster.iterate_topics(function(ct) {
            self.get_topic(ct.id).set_position(ct.x, ct.y)
        })
        // update DB
        if (is_writable()) {
            dm4c.restc.set_cluster_position(topicmap_id, cluster_coords())
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

    this.draw_background = function(ctx) {
        if (this.background_image) {
            ctx.drawImage(this.background_image, 0, 0)
        }
    }



    // ----------------------------------------------------------------------------------------------- Private Functions

    function load() {

        var topicmap = dm4c.restc.get_topicmap(topicmap_id)
        info = new Topic(topicmap.info)
        //
        init_topics()
        init_associations()
        init_translation()
        init_background_image()

        function init_topics() {
            for (var i = 0, topic; topic = topicmap.topics[i]; i++) {
                var x = topic.visualization["dm4.topicmaps.x"].value
                var y = topic.visualization["dm4.topicmaps.y"].value
                var visibility = topic.visualization["dm4.topicmaps.visibility"].value
                topics[topic.id] = new TopicViewmodel(topic.id, topic.type_uri, topic.value, x, y, visibility)
            }
        }

        function init_associations() {
            for (var i = 0, assoc; assoc = topicmap.assocs[i]; i++) {
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
        this.label = label
        this.x = x
        this.y = y
        this.visibility = visibility

        this.set_visibility = function(visibility) {
            this.visibility = visibility
            //
            if (!visibility) {
                reset_selection_conditionally()
            }
        }

        this.set_position = function(x, y) {
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

        this.delete = function() {
            // Note: all topic references are deleted already
            delete topics[id]
            reset_selection_conditionally()
        }

        // ---

        function reset_selection_conditionally() {
            if (self.is_topic_selected && self.selected_object_id == id) {
                self.reset_selection()
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

        this.is_player_topic = function(topic_id) {
            return topic_id == this.topic_id_1 || topic_id == this.topic_id_2
        }

        this.get_other_topic = function(topic_id) {
            if (topic_id == this.topic_id_1) {
                return this.get_topic_2()
            } else if (topic_id == this.topic_id_2) {
                return this.get_topic_1()
            } else {
                throw "AssociationViewmodelError: topic " + topic_id + " is not a player in " + JSON.stringify(this)
            }
        }

        // ---

        /**
         * @param   assoc   an Association object
         */
        this.update = function(assoc) {
            this.type_uri = assoc.type_uri
        }

        this.delete = function() {
            delete assocs[id]
            reset_selection_conditionally()
        }

        // ---

        function reset_selection_conditionally() {
            if (!self.is_topic_selected && self.selected_object_id == id) {
                self.reset_selection()
            }
        }
    }

    function ClusterViewmodel(ca) {

        var cts = []    // array of TopicView

        add_to_cluster(ca.get_topic_1())

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
            var cas = self.get_topic_associations(ct.id)
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
