/**
 * @param   topic   a JavaScript object with these properties:
 *                      id        - mandatory, may be -1
 *                      uri       - mandatory, may be ""
 *                      type_uri  - mandatory
 *                      value     - mandatory, may be ""
 *                      composite - mandatory, may be {}
 */
function Topic(topic) {
    this.id        = topic.id
    this.uri       = topic.uri
    this.type_uri  = topic.type_uri
    this.value     = topic.value
    this.composite = build_composite(topic.composite)
}

// ### TODO: create common base class (DeepaMehtaObject) for topics and associations.
function build_composite(composite) {
    var comp = {}
    for (var child_type_uri in composite) {
        var child_topic = composite[child_type_uri]
        if (js.is_array(child_topic)) {
            comp[child_type_uri] = []
            for (var i = 0, topic; topic = child_topic[i]; i++) {
                comp[child_type_uri].push(new Topic(topic))
            }
        } else {
            comp[child_type_uri] = new Topic(child_topic)
        }
    }
    return comp
}

// === "Page Displayable" implementation ===

Topic.prototype.get_type = function() {
    return dm4c.get_topic_type(this.type_uri)
}

Topic.prototype.get_commands = function(context) {
    return dm4c.get_topic_commands(this, context)
}

// === Public API ===

/**
 * Returns the value of a direct child topic, specified by type URI. The value may be simple or composite.
 * You can lookup nested values by chaining the get() calls.
 *
 * If no such child topic exists undefined is returned.
 *
 * @param   child_type_uri  The URI of a direct child type.
 *
 * @return  A simple value (string, number, boolean) or a composite value (a Topic object) or an array
 *          (in case of multiple values).
 */
Topic.prototype.get = function(child_type_uri) {
    var child_topic = this.composite[child_type_uri]
    if (child_topic) {
        if (js.is_array(child_topic)) {
            return child_topic
        }
        var topic_type = dm4c.get_topic_type(child_type_uri)
        if (topic_type.is_simple()) {
            return child_topic.value
        } else {
            return child_topic
        }
    }
}

Topic.prototype.find_child_topic = function(type_uri) {
    return find_child_topic(this)

    function find_child_topic(topic) {
        if (topic.type_uri == type_uri) {
            return topic
        }
        for (var child_type_uri in topic.composite) {
            var child_topic = topic.composite[child_type_uri]
            if (js.is_array(child_topic)) {
                child_topic = child_topic[0]
            }
            child_topic = find_child_topic(child_topic)
            if (child_topic) {
                return child_topic
            }
        }
    }
}
