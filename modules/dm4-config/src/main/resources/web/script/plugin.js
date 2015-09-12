dm4c.add_plugin("de.deepamehta.config", function() {

    var config_defs = dm4c.restc.request("GET", "/config")

    dm4c.add_listener("topic_commands", function(topic) {
        return [
            {
                is_separator: true,
                context: "context-menu"
            },
            {
                label:   "Configure",
                sub_commands: sub_commands(),
                context: "context-menu"
            }
        ]

        function sub_commands() {
            var commands = []
            var config_type_uris = config_defs[topic.type_uri]
            if (config_type_uris) {
                for (var i = 0, config_type_uri; config_type_uri = config_type_uris[i]; i++) {
                    commands.push(command(config_type_uri))
                }
            }
            return commands
        }

        function command(config_type_uri) {
            return {
                label: dm4c.topic_type_name(config_type_uri),
                icon: dm4c.get_type_icon_src("dm4.core.topic_type"),
                handler: function() {
                    var config_topic = new Topic(
                        dm4c.restc.request("GET", "/config/" + config_type_uri + "/topic/" + topic.id)
                    )
                    console.log("Revealing \"" + config_type_uri + "\" config topic", config_topic)
                    dm4c.show_topic(config_topic, "show", undefined, true)     // coordinates=undefined, do_center=true
                    dm4c.show_association(config_topic.assoc)
                }
            }
        }
    })
})
