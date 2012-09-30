/**
 * Generic (DeepaMehta independent) JavaScript Utilities.
 */
var js = {



    // ************************
    // *** Arrays & Objects ***
    // ************************



    /**
     * Filters array elements that match a filter function.
     * The array is manipulated in-place.
     */
    filter: function(array, fn) {
        var i = 0, e
        while (e = array[i]) {
            if (!fn(e)) {
                array.splice(i, 1)
                continue
            }
            i++
        }
    },

    /**
     * Returns an array containing the keys of the object.
     */
    keys: function(object) {
        var a = []
        for (var key in object) {
            a.push(key)
        }
        return a
    },

    size: function(object) {
        var size = 0
        for (var key in object) {
            size++
        }
        return size
    },

    inspect: function(object) {
        var attr_keys = []
        var func_keys = []
        // sort keys
        for (var key in object) {
            if (typeof object[key] == "function") {
                func_keys.push(key)
            } else {
                attr_keys.push(key)
            }
        }
        attr_keys.sort()
        func_keys.sort()
        // build result
        var str = "\n"
        for (var i = 0, key; key = attr_keys[i]; i++) {
            str += key + ": " + object[key] + "\n"
        }
        for (var i = 0, key; key = func_keys[i]; i++) {
            str += "function " + key + "()\n"
        }
        return str
    },

    stringify: function(object) {
        var max_depth = 10
        var str = ""
        stringify(object, 0, "")
        return str

        function stringify(object, depth, indent) {
            switch (typeof object) {
            case "string":
                str += "\"" + object + "\" (string)"
                return
            case "number":
                str += object + " (number)"
                return
            case "boolean":
                str += object + " (boolean)"
                return
            case "object":
                str += (js.is_array(object) ? "[" : "{") + "\n"
                if (depth < max_depth) {
                    for (var key in object) {
                        // skip functions
                        if (typeof object[key] == "function") {
                            continue
                        }
                        //
                        str += indent + "\t" + key + ": "
                        stringify(object[key], depth + 1, indent + "\t")
                        str += "\n"
                    }
                } else {
                    str += indent + "\t" + (js.is_array(object) ? "ARRAY" : "OBJECT") +
                        " NOT SHOWN (max " + max_depth + " levels)\n"
                }
                str += indent + (js.is_array(object) ? "]" : "}")
                return
            case "function":
                // skip
                return
            case "undefined":
                str += "undefined"
                return
            default:
                str += "UNKNOWN (" + typeof(object) + ")"
                return
            }
        }
    },

    /**
     * Returns true if the array contains the object, false otherwise.
     */
    contains: function(array, object) {
        if (!array) {
            return false
        }
        for (var i = 0; i < array.length; i++) {
            if (array[i] == object) {
                return true
            }
        }
        return false
    },

    /**
     * Returns true if the array contains a positive element according to the indicator function.
     */
    includes: function(array, indicator_func) {
        for (var i = 0, e; e = array[i]; i++) {
            if (indicator_func(e)) {
                return true
            }
        }
        return false
    },

    /**
     * Substracts array2 from array1.
     */
    substract: function(array1, array2, fn) {
        js.filter(array1, function(e1) {
             return !js.includes(array2, function(e2) {
                 return fn(e1, e2)
             })
        })
    },

    clone: function(obj) {
        try {
            return JSON.parse(JSON.stringify(obj))
        } catch (e) {
            alert("ERROR (clone): " + JSON.stringify(e))
        }
    },

    is_array: function(obj) {
        return Object.prototype.toString.call(obj) == "[object Array]"
        // Note: since Javascript 1.8.5 (Firefox 4) there is Array.isArray(obj).
        // The approach used here is compatible with older Javascript versions and is preferred by ECMA.
    },



    // ***************
    // *** Classes ***
    // ***************



    /**
     * Extends an object with all the methods defined in a superclass.
     *
     * @param   obj         The object to be extended
     * @param   superclass  The superclass (a function)
     */
    extend: function(obj, superclass) {
        superclass.call(obj)
    },



    // ************
    // *** Text ***
    // ************



    begins_with: function(text, str) {
        return text.search("^" + str) != -1
    },

    render_text: function(text) {
        return text.replace(/\n/g, "<br>")
    },

    strip_html: function(text) {
        // Compare to the Java-equivalent stripHTML() in JavaUtils.java
        // *? is the reluctant version of the * quantifier (which is greedy).
        return text.replace(/<.*?>/g, "")
    },

    truncate: function(text, max_length) {
        if (text.length <= max_length) {
            return text
        }
        var i = text.lastIndexOf(" ", max_length)
        return text.substr(0, i >= 0 ? i : max_length) + " ..."
    },

    filename: function(path) {
        path.match(/.*\/(.*)/)
        return RegExp.$1
    },

    filename_ext: function(path) {
        return path.substr(path.lastIndexOf(".") + 1)
    },

    /**
     * "Type ID" -> "type-id"
     */
    to_id: function(str) {
        str = str.toLowerCase()
        str = str.replace(/ /g, "-")
        return str
    },

    /**
     * @param   date    the date to format (string). If empty (resp. evaluates to false) an empty string is returned.
     *                  Otherwise it must be parsable by the Date constructor, e.g. "12/30/2009".
     */
    format_date: function(date) {
        // For possible format strings see http://docs.jquery.com/UI/Datepicker/formatDate
        return date ? $.datepicker.formatDate("D, M d, yy", new Date(date)) : ""
    },

    format_timestamp: function(timestamp) {
        return new Date(timestamp).toLocaleString()
    },

    absolute_http_url: function(url) {
        if (!js.begins_with(url, "http://") && !js.begins_with(url, "https://")) {
            return "http://" + url
        }
        return url
    },

    TextWrapper: function(text, max_width, line_height, ctx) {

        var wrapped_lines = []
        wrap_text()

        // ---

        this.draw = function(x, y, ctx) {
            var dy = y
            for (var i = 0, line; line = wrapped_lines[i]; i++) {
                ctx.fillText(line, x, dy)
                dy += line_height
            }
        }

        // ---

        function wrap_text() {
            // do not wrap "text" if it's a number or a boolean
            if (!text.split) {
                wrapped_lines.push(text.toString())
                return
            }
            //
            var line = ""   // current line
            var width = 0   // width of current line
            var w           // width of current word
            var words = text.split(/([ \n])/)
            for (var i = 0; i < words.length; i += 2) {
                wrap_word(words[i - 1], words[i] + " ")     // Note: words[-1] == undefined
            }
            wrapped_lines.push(line)

            function wrap_word(sep, word) {
                w = ctx.measureText(word).width
                if (sep == "\n") {
                    begin_new_line(word)
                } else if (width + w <= max_width) {
                    append_to_line(word)
                } else {
                    begin_new_line(word)
                }
            }

            function append_to_line(word) {
                line  += word
                width += w
            }

            function begin_new_line(word) {
                if (line) {
                    wrapped_lines.push(line)
                }
                line  = word
                width = w
            }
        }
    },



    // ***************
    // *** Cookies ***
    // ***************



    set_cookie: function(key, value) {
        /* var days = 2
        var expires = new Date()
        expires.setTime(expires.getTime() + days * 24 * 60 * 60 * 1000) */
        //
        // DeepaMehta note: the cookie's path must be explicitly set to "/". If not set the browser would set it to
        // "/de.deepamehta.webclient" (the "directory" of the page that loaded this script) and the cookie will not be
        // send back to the server for XHR requests as these are bound to "/core". ### FIXDOC: still true?
        // Vice versa we can't set the cookie's path to "/core" because it would not be accessible here at client-side.
        document.cookie = key + "=" + value + ";path=/" // + ";expires=" + expires.toGMTString()
    },

    get_cookie: function(key) {
        // Note: document.cookie contains all cookies as one string, e.g. "username=jri; workspace_id=83".
        if (document.cookie.match(new RegExp("\\b" + key + "=(\\w*)"))) {
            return RegExp.$1
        }
    },

    remove_cookie: function(key) {
        // Note: setting the expire date to yesterday removes the cookie
        var days = -1
        var expires = new Date()
        expires.setTime(expires.getTime() + days * 24 * 60 * 60 * 1000)
        //
        document.cookie = key + "=;path=/;expires=" + expires.toGMTString()
    }
}
