function facets_plugin() {

    // ------------------------------------------------------------------------------------------------------ Public API

    /**
     * Extends the page model of the webclient's default topic renderer by facets.
     * Utility method to be called by other plugins.
     *
     * @param   topic           The facetted topic the page/form is rendered for. Usually that is the selected topic.
     * @param   facet_types     The facet types to add to the page model (array of topic objects of type "Topic Type").
     * @param   page_model      The page model to extend.
     * @param   setting         "viewable" or "editable" (string).
     */
    this.add_facets_to_page_model = function(topic, facet_types, page_model, setting) {
        for (var i = 0; i < facet_types.length; i++) {
            var facet_type = dm4c.get_topic_type(facet_types[i].uri)
            // compare to TopicRenderer.create_page_model
            var assoc_def = facet_type.assoc_defs[0]
            var child_topic_type = dm4c.get_topic_type(assoc_def.part_topic_type_uri)
            var child_field_uri = dm4c.COMPOSITE_PATH_SEPARATOR + assoc_def.uri
            // ### TODO: cardinality many
            var child_topic = topic.composite[assoc_def.uri] || dm4c.empty_topic(child_topic_type.uri)
            var child_model = TopicRenderer.create_page_model(child_topic, assoc_def, child_field_uri, topic, setting)
            // ### FIXME: child_model may be undefined
            page_model.add_child(assoc_def.uri, child_model)
        }
    }
}
