/**
 * A topicmap viewmodel that is attached to the database. There are methods for:
 *  - loading a topicmap from DB
 *  - manipulating the topicmap by e.g. adding/removing topics and associations
 *
 * @param   config  an object with 2 properties:
 *                     "is_writable" (boolean) -- indicates weather changes to this model are supposed to be persistent.
 *                     "customizers" (array of viewmodel customer instances) -- the registered viewmodel customizers.
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

    /**
     * @param   topic   a domain topic (has "id", "type_uri", "value" properties).
     *
     * @return  The topic viewmodel that represents what is about to be added to the view (a TopicViewmodel object).
     *          This is either a new viewmodel (in case the domain topic was not yet contained in the topicmap) or
     *          a modified viewmodel (in case the domain topic is already contained in the topicmap but was hidden).
     *          If the domain topic is already contained in the topicmap and is visible already nothing is returned.
     */
    this.add_topic = function(topic, x, y) {
        var _topic = topics[topic.id]
        if (!_topic) {
            // update memory
            var view_props = default_view_props()
            _topic = add_topic(topic, view_props)
            // update DB
            if (is_writable()) {
                dm4c.restc.add_topic_to_topicmap(topicmap_id, topic.id, view_props)
            }
            //
            return _topic
        } else if (!_topic.visibility) {
            // update memory
            _topic.set_visibility(true)
            // update DB
            if (is_writable()) {
                dm4c.restc.set_topic_visibility(topicmap_id, topic.id, true)
            }
            //
            return _topic
        } else {
            // topic already visible in topicmap
        }

        function default_view_props() {
            var view_props = {
                "dm4.topicmaps.x": x,
                "dm4.topicmaps.y": y,
                "dm4.topicmaps.visibility": true
            }
            invoke_customizers("enrich_view_properties", [topic, view_props])
            return view_props
        }
    }

    /**
     * @param   assoc   a domain association (has "id", "type_uri", "role_1", "role_2" properties).
     */
    this.add_association = function(assoc) {
        var _assoc = assocs[assoc.id]
        if (!_assoc) {
            // update memory
            _assoc = add_association(assoc)
            // update DB
            if (is_writable()) {
                dm4c.restc.add_association_to_topicmap(topicmap_id, assoc.id)
            }
            //
            return _assoc
        } else {
            // association already in topicmap
        }
    }

    // ---

    this.set_view_properties = function(topic_id, view_props) {
        var topic = this.get_topic(topic_id)
        // update memory
        topic.set_view_properties(view_props)
        // update DB
        if (is_writable()) {
            dm4c.restc.set_view_properties(topicmap_id, topic_id, view_props)
        }
    }

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
            if (t.visibility) {
                return t
            }
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
     * Returns the ID of the selected topic, provided a topic is selected, false otherwise.
     */
    /* ### this.get_selected_topic_id = function() {
        return this.has_selection() && this.is_topic_selected && this.selected_object_id
    } */

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
        // update memory
        this.trans_x += dx
        this.trans_y += dy
        // update DB
        //
        // Note: the DB is not updated here. This works around the fact that the canvas view does not have its own
        // translation model. Instead it updates the topicmap viewmodel repeatedly while moving the canvas. We don't
        // want create many DB update requests while a canvas drag.
        // ### TODO: consider equipping the canvas view with a translation model.
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
                var view_props = compact_composite_format(topic.view_props)
                add_topic(topic, view_props)
            }
        }

        function init_associations() {
            for (var i = 0, assoc; assoc = topicmap.assocs[i]; i++) {
                add_association(assoc)
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

    function compact_composite_format(comp_value) {
        var simple_comp = {}
        for (var type_uri in comp_value) {
            var val = comp_value[type_uri]
            // ### TODO
            if (js.is_array(val)) {
                throw "multiple-valued view properties are not supported"
            } else if (js.size(val.composite)) {
                throw "composite view properties are not supported"
            }
            //
            simple_comp[type_uri] = val.value
        }
        return simple_comp
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

    /**
     * @param   topic   a domain topic (has "id", "type_uri", "value" properties).
     */
    function add_topic(topic, view_props) {
        var _topic = new TopicViewmodel(topic, view_props)
        topics[topic.id] = _topic
        return _topic
    }

    /**
     * @param   assoc   a domain association (has "id", "type_uri", "role_1", "role_2" properties).
     */
    function add_association(assoc) {
        var _assoc = new AssociationViewmodel(assoc)
        assocs[assoc.id] = _assoc
        return _assoc
    }

    // ---

    function is_writable() {
        return config.is_writable
    }



    // === Customization ===

    /**
     * @param   args    array of arguments
     */
    function invoke_customizers(func_name, args) {
        for (var i = 0, customizer; customizer = config.customizers[i]; i++) {
            customizer[func_name] && customizer[func_name].apply(undefined, args)
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Classes

    /**
     * @param   topic   a domain topic (has "id", "type_uri", "value" properties).
     */
    function TopicViewmodel(topic, view_props) {

        this.id       = topic.id
        this.type_uri = topic.type_uri
        this.label    = topic.value
        // standard view properties
        this.x          = view_props["dm4.topicmaps.x"]
        this.y          = view_props["dm4.topicmaps.y"]
        this.visibility = view_props["dm4.topicmaps.visibility"]
        // enable access to custom view properties
        this.view_props = view_props

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

        this.set_view_properties = function(view_props) {
            // Note: this has a side effect on the corresponding TopicView object as it holds
            // a reference to this topic's view_props object (instead of creating another one).
            // So the CanvasView must not care about updating the TopicView object.
            js.copy(view_props, this.view_props)
        }

        /**
         * @param   topic   a domain topic (has "id", "type_uri", "value" properties).
         */
        this.update = function(topic) {
            this.type_uri = topic.type_uri
            this.label    = topic.value
        }

        this.delete = function() {
            // Note: all topic references are deleted already
            delete topics[topic.id]
            reset_selection_conditionally()
        }

        // ---

        function reset_selection_conditionally() {
            if (self.is_topic_selected && self.selected_object_id == topic.id) {
                self.reset_selection()
            }
        }
    }

    /**
     * @param   assoc   a domain association (has "id", "type_uri", "role_1", "role_2" properties).
     */
    function AssociationViewmodel(assoc) {

        this.id = assoc.id
        this.type_uri = assoc.type_uri
        this.topic_id_1 = assoc.role_1.topic_id
        this.topic_id_2 = assoc.role_2.topic_id

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
            delete assocs[assoc.id]
            reset_selection_conditionally()
        }

        // ---

        function reset_selection_conditionally() {
            if (!self.is_topic_selected && self.selected_object_id == assoc.id) {
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
