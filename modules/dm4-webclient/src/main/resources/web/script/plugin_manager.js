function PluginManager(config) {

    var plugins = {}            // key: plugin URI, value: plugin instance
    var page_renderers = {}     // key: page renderer URI, value: object with "render_page", "render_form", "page_css"
    var simple_renderers = {}   // key: simple renderer URI, value: object with "render_info", "render_form"
    var multi_renderers = {}    // key: multi renderer URI, value: object with "render_info", "render_form"

    var plugin_list
    var load_tracker

    // key: event name (string), value: registered listeners (array of functions)
    var listener_registry = {}

    // ------------------------------------------------------------------------------------------------------ Public API

    this.retrieve_plugin_list = function() {
        // retrieve list of installed plugins from server
        plugin_list = dm4c.restc.get_plugins()
        // "Plugins installed at server-side: " + plugin_list.length
        //
        var items_to_load = count_items_to_load(plugin_list) + config.internal_plugins.length
        // "Total items to load: " + items_to_load
        //
        return items_to_load
    }

    this.load_plugins = function(tracker) {
        load_tracker = tracker
        //
        load_internal_plugins(config.internal_plugins)
        //
        for (var i = 0, plugin; plugin = plugin_list[i]; i++) {
            load_plugin(plugin)
        }
    }

    this.add_plugin = function(plugin_uri, plugin_func) {
        // error check
        if (plugins[plugin_uri]) {
            throw "PluginManagerError: plugin URI clash with \"" + plugin_uri + "\""
        }
        //
        var plugin = {}
        plugin_func.call(plugin)
        plugins[plugin_uri] = plugin
        //
        track_load_state("plugin \"" + plugin_uri + "\"");
    }

    this.get_plugin = function(plugin_uri) {
        var plugin = plugins[plugin_uri]
        // error check
        if (!plugin) {
            throw "PluginManagerError: plugin \"" + plugin_uri + "\" is unknown"
        }
        //
        return plugin
    }

    // ---

    this.add_page_renderer = function(renderer_uri, renderer) {
        // error check
        if (page_renderers[renderer_uri]) {
            throw "PluginManagerError: page renderer URI clash with \"" + renderer_uri + "\""
        }
        //
        page_renderers[renderer_uri] = renderer
        //
        track_load_state("page renderer \"" + renderer_uri + "\"");
    }

    this.get_page_renderer = function(topic_or_association_or_renderer_uri) {
        if (typeof(topic_or_association_or_renderer_uri) == "string") {
            var renderer_uri = topic_or_association_or_renderer_uri
        } else {
            var type = topic_or_association_or_renderer_uri.get_type()
            var renderer_uri = type.get_page_renderer_uri()
        }
        var renderer = page_renderers[renderer_uri]
        // error check
        if (!renderer) {
            throw "PluginManagerError: page renderer \"" + renderer_uri + "\" is unknown"
        }
        //
        return renderer
    }

    // ---

    this.add_simple_renderer = function(renderer_uri, renderer) {
        // error check
        if (simple_renderers[renderer_uri]) {
            throw "PluginManagerError: simple renderer URI clash with \"" + renderer_uri + "\""
        }
        //
        simple_renderers[renderer_uri] = renderer
        //
        track_load_state("simple renderer \"" + renderer_uri + "\"");
    }

    this.get_simple_renderer = function(renderer_uri) {
        var renderer = simple_renderers[renderer_uri]
        // error check
        if (!renderer) {
            throw "PluginManagerError: simple renderer \"" + renderer_uri + "\" is unknown"
        }
        //
        return renderer
    }

    // ---

    this.add_multi_renderer = function(renderer_uri, renderer) {
        // error check
        if (multi_renderers[renderer_uri]) {
            throw "PluginManagerError: multi renderer URI clash with \"" + renderer_uri + "\""
        }
        //
        multi_renderers[renderer_uri] = renderer
        //
        track_load_state("multi renderer \"" + renderer_uri + "\"");
    }

    this.get_multi_renderer = function(renderer_uri) {
        var renderer = multi_renderers[renderer_uri]
        // error check
        if (!renderer) {
            throw "PluginManagerError: multi renderer \"" + renderer_uri + "\" is unknown"
        }
        //
        return renderer
    }

    // ---

    this.load_stylesheet = function(stylesheet) {
        load_stylesheet(stylesheet)
    }

    // === Listener Registry ===

    this.add_listener = function(event_name, listener) {
        // introduce event on-demand
        if (!event_exists(event_name)) {
            listener_registry[event_name] = []
        }
        //
        add_listener(event_name, listener)
    }

    // ---

    /**
     * Delivers the event to the registered listeners.
     *
     * @param   event_name  Name of the event.
     * @param   <varargs>   Variable number of arguments. Passed to the listeners.
     */
    this.deliver_event = function(event_name) {
        var result = []
        //
        var listeners = listener_registry[event_name]
        if (listeners) {
            // build arguments
            var args = Array.prototype.slice.call(arguments)    // create real array from arguments object
            args.shift()                                        // drop event_name argument
            //
            for (var i = 0, listener; listener = listeners[i]; i++) {
                // call listener
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

    /**
     * Counts the items which are loaded asynchronously and which are subject of load tracking.
     * Note: the helper files are not taken into account here.
     */
    function count_items_to_load(plugin_list) {
        var count = 0
        for (var i = 0, plugin; plugin = plugin_list[i]; i++) {
            // count plugin file
            if (plugin.has_plugin_file) {
                count++
            }
            // count renderers
            for (var renderer_type in plugin.renderers) {
                count += plugin.renderers[renderer_type].length
            }
        }
        return count
    }

    function track_load_state(item) {
        // item + " complete"
        load_tracker.track()
    }

    // ---

    function add_listener(event_name, listener) {
        listener_registry[event_name].push(listener)
    }

    function event_exists(event_name) {
        return listener_registry[event_name]
    }

    // ---

    function load_internal_plugins(plugins) {
        for (var i = 0, plugin; plugin = plugins[i]; i++) {
            load_plugin_file("/de.deepamehta.webclient/script/internal_plugins/" + plugin)
        }
    }

    function load_plugin(plugin) {
        var plugin_uri = plugin.plugin_uri
        //
        load_helper_files(plugin_uri, plugin.helper)
        //
        load_plugin_file_if_exists(plugin_uri, plugin.has_plugin_file)
        //
        load_renderers(plugin_uri, "page_renderers",   plugin.renderers.page_renderers)
        load_renderers(plugin_uri, "simple_renderers", plugin.renderers.simple_renderers)
        load_renderers(plugin_uri, "multi_renderers",  plugin.renderers.multi_renderers)
        // ### load_renderers(plugin_uri, "topicmap_renderers", plugin.renderers.topicmap_renderers)
        //
        load_stylesheets(plugin_uri, plugin.stylesheets)
    }

    function load_helper_files(plugin_uri, helper_files) {
        for (var i = 0, helper_file; helper_file = helper_files[i]; i++) {
            helper_file = "/" + plugin_uri + "/script/helper/" + helper_file
            // Note: helper files are loaded synchronously
            dm4c.load_script(helper_file)
        }
    }

    function load_plugin_file_if_exists(plugin_uri, has_plugin_file) {
        if (has_plugin_file) {
            var plugin_file = "/" + plugin_uri + "/script/plugin.js"
            load_plugin_file(plugin_file)
        }
    }

    function load_plugin_file(plugin_file) {
        dm4c.load_script(plugin_file, true)         // async=true
    }

    function load_renderers(plugin_uri, renderer_type, renderer_files) {
        for (var i = 0, renderer_file; renderer_file = renderer_files[i]; i++) {
            renderer_file = "/" + plugin_uri + "/script/renderers/" + renderer_type + "/" + renderer_file
            dm4c.load_script(renderer_file, true)   // async=true
        }
    }

    function load_stylesheets(plugin_uri, stylesheets) {
        for (var i = 0, stylesheet; stylesheet = stylesheets[i]; i++) {
            load_stylesheet("/" + plugin_uri + "/style/" + stylesheet)
        }
    }

    function load_stylesheet(stylesheet) {
        $("head").append($("<link>").attr({rel: "stylesheet", href: stylesheet, type: "text/css"}))
    }
}
