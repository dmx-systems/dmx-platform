dm4c.add_plugin("de.deepamehta.config", function() {

    var config_defs

    refresh_config_defs()


    // === Requests ===

    function get_config_topic(topic_id, config_type_uri) {
        return dm4c.restc.request("GET", "/config/" + config_type_uri + "/topic/" + topic_id + "?include_childs=true")
    }

    function refresh_config_defs() {
        config_defs = dm4c.restc.request("GET", "/config")
    }



    // === Webclient Listeners ===

    dm4c.add_listener("topic_commands", function(topic) {
        var sub_commands = config_commands()
        return [
            {
                is_separator: true,
                context: "context-menu"
            },
            {
                label:   "Show Configuration",
                sub_commands: sub_commands,
                disabled: !sub_commands.length,
                context: "context-menu"
            }
        ]

        function config_commands() {
            var commands = []
            add_config_commands("topic_uri:" + topic.uri,      commands)
            add_config_commands("type_uri:"  + topic.type_uri, commands)
            return commands
        }

        function add_config_commands(configurable_uri, commands) {
            var config_type_uris = config_defs[configurable_uri]
            if (config_type_uris) {
                for (var i = 0, config_type_uri; config_type_uri = config_type_uris[i]; i++) {
                    commands.push(config_command(config_type_uri))
                }
            }
        }

        function config_command(config_type_uri) {
            return {
                label: dm4c.topic_type_name(config_type_uri),
                icon: dm4c.get_type_icon_src("dm4.core.topic_type"),
                handler: function() {
                    var config_topic = new Topic(get_config_topic(topic.id, config_type_uri))
                    dm4c.show_topic(config_topic, "show", undefined, true)     // coordinates=undefined, do_center=true
                    dm4c.show_association(config_topic.assoc)
                }
            }
        }
    })

    dm4c.add_listener("logged_in", function() {
        console.log("logged_in")
        refresh_config_defs()
    })

    dm4c.add_listener("authority_decreased", function() {
        console.log("authority_decreased")
        refresh_config_defs()
    })



    // ------------------------------------------------------------------------------------------------------ Public API

    this.get_config_topic = function(topic_id, config_type_uri) {
        return get_config_topic(topic_id, config_type_uri)
    }
})
