function PluginManager(config) {

    var plugin_sources = []
    var plugins = {}            // key: plugin URI, value: plugin instance
    var plugins_complete = 0

    var page_renderer_sources = []
    var page_renderers = {}     // key: page renderer class name, camel case (string), value: renderer instance

    var field_renderer_sources = []

    var css_stylesheets = []

    // key: hook name (string), value: registered listeners (array of functions)
    var listener_registry = {}

    // ### FIXME: drop this. Not in use. Only here for documentation purposes.
    // ### TODO: move to wiki documentation.
    var hook_names = [
        // Plugin
        "init",
        // Commands
        "topic_commands",
        "association_commands",
        "canvas_commands",
        // Storage (DB updates)
        "post_create_topic",
        "post_update_topic",
        "post_update_association",
        "post_delete_topic",
        "post_delete_association",
        // Selection (client model updates)
        "post_select_topic",
        "post_select_association",
        "post_reset_selection",
        // Show/Hide (view updates)
        "pre_show_topic",
        "post_show_topic",
        "post_show_association",
        "post_hide_topic",
        "post_hide_association",
        // Toolbar
        "searchmode_widget",
        "search",
        "post_refresh_create_menu",
        // Page Panel
        "pre_render_page",
        "pre_render_form",
        "post_destroy_form",
        "default_page_rendering",
        // Canvas
        "topic_doubleclicked",
        "post_move_topic",
        "post_move_canvas",
        "pre_draw_canvas",
        "process_drop",
        // History
        "pre_push_history",
        "pre_pop_history",
        // Permissions
        "has_write_permission",
        "has_create_permission"
    ]

    // ------------------------------------------------------------------------------------------------------ Public API

    this.load_plugins = function() {
        register_internal_plugins(config.internal_plugins)
        register_plugins()
        load_plugins()
    }

    this.add_plugin = function(plugin_uri, plugin_func) {
        // instantiate
        if (dm4c.LOG_PLUGIN_LOADING) dm4c.log(".......... instantiating \"" + plugin_uri + "\"")
        var plugin = {}
        plugin_func.call(plugin)
        plugins[plugin_uri] = plugin
        // all plugins complete?
        plugins_complete++
        if (plugins_complete == plugin_sources.length) {
            if (dm4c.LOG_PLUGIN_LOADING) dm4c.log("PLUGINS COMPLETE!")
            all_plugins_loaded()
        }

        function all_plugins_loaded() {
            load_page_renderers()
            load_field_renderers()
            load_stylesheets()
            //
            config.post_load_plugins()
        }
    }

    this.get_plugin = function(plugin_uri) {
        return plugins[plugin_uri]
    }

    this.get_page_renderer = function(topic_or_association_or_classname) {
        if (typeof(topic_or_association_or_classname) == "string") {
            var page_renderer_class = topic_or_association_or_classname
        } else {
            var type = topic_or_association_or_classname.get_type()
            var page_renderer_class = type.get_page_renderer_class()
        }
        var page_renderer = page_renderers[page_renderer_class]
        // error check
        if (!page_renderer) {
            throw "PluginManagerError: page renderer \"" + page_renderer_class + "\" is unknown"
        }
        //
        return page_renderer
    }

    // ---

    this.register_page_renderer = function(source_path) {
        page_renderer_sources.push(source_path)
    }

    this.register_field_renderer = function(source_path) {
        field_renderer_sources.push(source_path)
    }

    this.register_css_stylesheet = function(css_path) {
        css_stylesheets.push(css_path)
    }

    // === Listener Registry ===

    this.register_listener = function(hook_name, listener) {
        // introduce hook on-demand
        if (!hook_exists(hook_name)) {
            listener_registry[hook_name] = []
        }
        //
        register_listener(hook_name, listener)
    }

    // ---

    /**
     * Triggers the registered listeners for the named hook.
     *
     * @param   hook_name   Name of the hook.
     * @param   <varargs>   Variable number of arguments. Passed to the listeners.
     */
    this.trigger_listeners = function(hook_name) {
        var result = []
        //
        var listeners = listener_registry[hook_name]
        if (listeners) {
            // build arguments
            var args = Array.prototype.slice.call(arguments)    // create real array from arguments object
            args.shift()                                        // drop hook_name argument
            //
            for (var i = 0, listener; listener = listeners[i]; i++) {
                // trigger listener
                var res = listener.apply(undefined, args)       // FIXME: pass plugin reference for "this"?
                // store result
                if (res !== undefined) {    // Note: undefined is not added to the result, but null is
                    result.push(res)
                }
            }
        }
        //
        return result
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    function register_internal_plugins(plugins) {
        for (var i = 0, plugin; plugin = plugins[i]; i++) {
            register_plugin("/de.deepamehta.webclient/script/internal_plugins/" + plugin)
        }
    }

    /**
     * Retrieves the list of installed plugins and registers those which have a client component.
     */
    function register_plugins() {
        // retrieve list
        var plugins = dm4c.restc.get_plugins()
        if (dm4c.LOG_PLUGIN_LOADING) dm4c.log("Plugins installed at server-side: " + plugins.length)
        // register
        for (var i = 0, plugin; plugin = plugins[i]; i++) {
            if (plugin.has_client_component) {
                if (dm4c.LOG_PLUGIN_LOADING) dm4c.log("..... plugin \"" + plugin.plugin_uri +
                    "\" -- has client component")
                register_plugin("/" + plugin.plugin_uri + "/script/plugin.js")
            } else {
                if (dm4c.LOG_PLUGIN_LOADING) dm4c.log("..... plugin \"" + plugin.plugin_uri + "\"")
            }
        }
    }

    function register_plugin(source_path) {
        plugin_sources.push(source_path)
    }

    // ---

    function register_listener(hook_name, listener) {
        listener_registry[hook_name].push(listener)
    }

    function hook_exists(hook_name) {
        return listener_registry[hook_name]
    }

    // ---

    /**
     * Loads all registered plugins.
     */
    function load_plugins() {
        if (dm4c.LOG_PLUGIN_LOADING) dm4c.log("Loading " + plugin_sources.length + " plugins:")
        for (var i = 0, plugin_source; plugin_source = plugin_sources[i]; i++) {
            if (dm4c.LOG_PLUGIN_LOADING) dm4c.log("..... " + plugin_source)
            dm4c.load_script(plugin_source, function() {})      // load plugin asynchronously
        }
    }

    function load_page_renderers() {

        if (dm4c.LOG_PLUGIN_LOADING) dm4c.log("Loading " + page_renderer_sources.length + " page renderers:")
        for (var i = 0, page_renderer_src; page_renderer_src = page_renderer_sources[i]; i++) {
            load_page_renderer(page_renderer_src)
        }

        function load_page_renderer(page_renderer_src) {
            if (dm4c.LOG_PLUGIN_LOADING) dm4c.log("..... " + page_renderer_src)
            // load page renderer synchronously (Note: synchronous is required for displaying initial topic)
            dm4c.load_script(page_renderer_src)
            // instantiate
            var page_renderer_class = js.to_camel_case(js.basename(page_renderer_src))
            if (dm4c.LOG_PLUGIN_LOADING) dm4c.log(".......... instantiating \"" + page_renderer_class + "\"")
            page_renderers[page_renderer_class] = js.instantiate(PageRenderer, page_renderer_class)
        }
    }

    function load_field_renderers() {
        if (dm4c.LOG_PLUGIN_LOADING) dm4c.log("Loading " + field_renderer_sources.length + " data field renderers:")
        for (var i = 0, field_renderer_source; field_renderer_source = field_renderer_sources[i]; i++) {
            if (dm4c.LOG_PLUGIN_LOADING) dm4c.log("..... " + field_renderer_source)
            // load field renderer synchronously (Note: synchronous is required for displaying initial topic)
            dm4c.load_script(field_renderer_source)
        }
    }

    function load_stylesheets() {
        if (dm4c.LOG_PLUGIN_LOADING) dm4c.log("Loading " + css_stylesheets.length + " CSS stylesheets:")
        for (var i = 0, css_stylesheet; css_stylesheet = css_stylesheets[i]; i++) {
            if (dm4c.LOG_PLUGIN_LOADING) dm4c.log("..... " + css_stylesheet)
            $("head").append($("<link>").attr({rel: "stylesheet", href: css_stylesheet, type: "text/css"}))
        }
    }
}
