function PluginManager(config) {

    var plugin_sources = config.embedded_plugins
    var plugins = {}            // key: plugin class name, base name of source file (string), value: plugin instance

    var page_renderer_sources = []
    var page_renderers = {}     // key: page renderer class name, camel case (string), value: renderer instance

    var field_renderer_sources = []

    var css_stylesheets = []

    // key: hook name, value: registered handlers (array of functions)
    var plugin_handlers = {
        // Plugin
        init: [],
        // Commands
        topic_commands: [],
        association_commands: [],
        canvas_commands: [],
        // Storage
        post_create_topic: [],
        post_update_topic: [],
        post_update_association: [],
        post_delete_topic: [],
        post_delete_association: [],
        // Selection
        post_select_topic: [],
        post_select_association: [],
        post_reset_selection: [],
        // Show/Hide
        pre_show_topic: [],
        post_show_topic: [],
        post_show_association: [],
        post_hide_topic: [],
        post_hide_association: [],
        // Toolbar
        searchmode_widget: [],
        search: [],
        post_refresh_create_menu: [],
        // Page Panel
        pre_render_page: [],
        pre_render_form: [],
        post_submit_form: [],
        // Canvas
        topic_doubleclicked: [],
        post_move_topic: [],
        post_move_canvas: [],
        pre_draw_canvas: [],
        process_drop: [],
        // History
        pre_push_history: [],
        pre_pop_history: [],
        // Permissions
        has_write_permission: [],
        has_create_permission: []
    }

    // ------------------------------------------------------------------------------------------------------ Public API

    this.load_plugins = function(callback) {
        register_plugins()
        load_plugins(callback)
    }

    this.get_plugin = function(plugin_class) {
        return plugins[plugin_class]
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
            throw "UnknownPageRenderer: page renderer \"" + page_renderer_class + "\" is not registered"
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

    // === Plugin Handlers ===

    this.add_hook = function(hook_name) {
        //
        if (hook_exists(hook_name)) {
            throw "PluginManagerError: hook with name \"" + hook_name + "\" already exists"
        }
        //
        plugin_handlers[hook_name] = []
    }

    this.register_plugin_handler = function(hook_name, plugin_handler) {
        //
        if (!hook_exists(hook_name)) {
            throw "PluginManagerError: \"" + hook_name + "\" is an unsupported hook"
        }
        //
        register_plugin_handler(hook_name, plugin_handler)
    }

    // ---

    /**
     * Triggers the named hook of all installed plugins.
     *
     * @param   hook_name   Name of the plugin hook to trigger.
     * @param   <varargs>   Variable number of arguments. Passed to the hook.
     */
    this.trigger_plugin_handlers = function(hook_name) {
        var result = []
        //
        var args = Array.prototype.slice.call(arguments)        // create real array from arguments object
        args.shift()                                            // drop hook_name argument
        //
        for (var i = 0, plugin_handler; plugin_handler = plugin_handlers[hook_name][i]; i++) {
            // trigger hook
            var res = plugin_handler.apply(undefined, args)     // FIXME: pass plugin reference for "this"?
            // store result
            if (res !== undefined) {    // Note: undefined is not added to the result, but null is
                result.push(res)
            }
        }
        //
        return result
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    /**
     * Registers server-side plugins to the list of plugins to load at client-side.
     */
    function register_plugins() {
        var plugins = dm4c.restc.get_plugins()
        if (dm4c.LOG_PLUGIN_LOADING) dm4c.log("Plugins installed at server-side: " + plugins.length)
        for (var i = 0, plugin; plugin = plugins[i]; i++) {
            if (plugin.plugin_file) {
                if (dm4c.LOG_PLUGIN_LOADING) dm4c.log("..... plugin \"" + plugin.plugin_id +
                    "\" contains client-side parts -- to be loaded")
                register_plugin("/" + plugin.plugin_id + "/script/" + plugin.plugin_file)
            } else {
                if (dm4c.LOG_PLUGIN_LOADING) dm4c.log("..... plugin \"" + plugin.plugin_id +
                    "\" contains no client-side parts -- nothing to load")
            }
        }
    }

    function register_plugin(source_path) {
        plugin_sources.push(source_path)
    }

    // ---

    function register_plugin_handler(hook_name, plugin_handler) {
        plugin_handlers[hook_name].push(plugin_handler)
    }

    function hook_exists(hook_name) {
        return plugin_handlers[hook_name]
    }

    // ---

    /**
     * Loads and instantiates all registered plugins.
     */
    function load_plugins(callback) {

        if (dm4c.LOG_PLUGIN_LOADING) dm4c.log("Loading " + plugin_sources.length + " plugins:")
        var plugins_complete = 0
        for (var i = 0, plugin_source; plugin_source = plugin_sources[i]; i++) {
            load_plugin(plugin_source)
        }

        function load_plugin(plugin_source) {
            if (dm4c.LOG_PLUGIN_LOADING) dm4c.log("..... " + plugin_source)
            // load plugin asynchronously
            dm4c.javascript_source(plugin_source, function() {
                // instantiate
                var plugin_class = js.basename(plugin_source)
                if (dm4c.LOG_PLUGIN_LOADING) dm4c.log(".......... instantiating \"" + plugin_class + "\"")
                plugins[plugin_class] = js.new_object(plugin_class)
                // all plugins complete?
                plugins_complete++
                if (plugins_complete == plugin_sources.length) {
                    if (dm4c.LOG_PLUGIN_LOADING) dm4c.log("PLUGINS COMPLETE!")
                    all_plugins_loaded()
                }
            })
        }

        function all_plugins_loaded() {
            load_page_renderers()
            load_field_renderers()
            load_stylesheets()
            //
            callback()
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
            dm4c.javascript_source(page_renderer_src)
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
            dm4c.javascript_source(field_renderer_source)
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
