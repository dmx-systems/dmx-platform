dm4c.add_plugin("de.deepamehta.facets", function() {

    // ------------------------------------------------------------------------------------------------------ Public API

    /**
     * Extends the page model of the webclient's default topic renderer by facets.
     * Utility method to be called by other plugins.
     *
     * @param   topic           The facetted topic the page/form is rendered for. Usually that is the selected topic.
     * @param   facet_types     The facet types to add to the page model (array of topic objects of type "Topic Type").
     * @param   page_model      The page model to extend.
     * @param   render_mode     dm4c.render.page_model.mode.INFO or
     *                          dm4c.render.page_model.mode.FORM
     */
    this.add_facets_to_page_model = function(topic, facet_types, page_model, render_mode) {
        for (var i = 0; i < facet_types.length; i++) {
            var facet_type = dm4c.get_topic_type(facet_types[i].uri)
            var assoc_def = facet_type.assoc_defs[0]
            dm4c.render.page_model.extend_composite_page_model(topic, assoc_def, "", topic, render_mode, page_model)
        }
    }
})
