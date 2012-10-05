/**
 * A page renderer that models a page as a set of fields.
 *
 * @see     PageRenderer interface (script/interfaces/page_renderer.js).
 */
(function() {

    dm4c.add_page_renderer("dm4.webclient.topic_renderer", {

        // === Page Renderer Implementation ===

        render_page: function(topic) {
            var render_mode = dm4c.render.page_model.mode.INFO
            //
            var page_model = create_page_model(topic, render_mode)
            dm4c.fire_event("pre_render_page", topic, page_model)
            render_page_model(page_model, render_mode)
            //
            dm4c.render.topic_associations(topic.id)
        },

        render_form: function(topic) {
            var render_mode = dm4c.render.page_model.mode.FORM
            //
            var page_model = create_page_model(topic, render_mode)
            dm4c.fire_event("pre_render_form", topic, page_model)
            render_page_model(page_model, render_mode)
            //
            return function() {
                dm4c.do_update_topic(dm4c.render.page_model.build_object_model(page_model))
            }
        }
    })



    // ----------------------------------------------------------------------------------------------- Private Functions

    // === Page Model ===

    /**
     * @param   render_mode     this.mode.INFO or this.mode.FORM (object). ### FIXDOC
     */
    function create_page_model(topic, render_mode) {
        return dm4c.render.page_model.create_page_model(topic, undefined, "", topic, render_mode)
    }

    /**
     * @param   page_model      the page model to render. If undefined nothing is rendered.
     * @param   render_mode     this.mode.INFO or this.mode.FORM (object). ### FIXDOC
     */
    function render_page_model(page_model, render_mode) {
        dm4c.render.page_model.render_page_model(page_model, render_mode, 0, $("#page-content"))
    }
})()
