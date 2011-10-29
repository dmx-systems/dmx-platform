/**
 * @param   directives  an array of directives
 */
function Directives(directives) {

    this.iterate = function(visitor_func) {
        for (var i = 0, directive; directive = directives[i]; i++) {
            visitor_func(directive)
        }
    }

    this.get_created_topic = function() {
        var topic
        this.iterate(function(directive) {
            if (directive.type == "CREATE_TOPIC") {
                topic = directive.arg
            }
        })
        if (!topic) {
            throw "InvalidDirectivesError: no CREATE_TOPIC directive found in " + JSON.stringify(directives)
        }
        return new Topic(topic)
    }
}
