/**
 * Generic (DeepaMehta independent) JavaScript Utilities.
 */
var js = {



    // ************************
    // *** Arrays & Objects ***
    // ************************



    /**
     * Returns the first array element that matches a filter function.
     * If there is no such element undefined is returned.
     */
    find: function(array, fn) {
        var i = 0, e
        while (e = array[i]) {
            if (fn(e)) {
                return e
            }
            i++
        }
    },

    /**
     * Keeps array elements that match a filter function.
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
     * Deletes array elements that match a filter function.
     * The array is manipulated in-place.
     */
    delete: function(array, fn) {
        this.filter(array, function(e) {
            return !fn(e)
        })
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

    copy: function(src_obj, dst_obj) {
        for (var prop in src_obj) {
            dst_obj[prop] = src_obj[prop]
        }
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
     * Extends an instance with all the methods defined in a superclass.
     *
     * @param   instance    The object to be extended
     * @param   superclass  The superclass (a function)
     */
    extend: function(instance, superclass) {
        superclass.call(instance)
    },

    class_name: function(instance) {
        return instance.__proto__.constructor.name  // ### TODO: is there a better way?
    },



    // ***************
    // *** Numbers ***
    // ***************



    round: function(val, decimals) {
        var factor = Math.pow(10, decimals)
        return Math.round(factor * val) / factor
    },



    // ************
    // *** Text ***
    // ************



    begins_with: function(text, str) {
        return text.indexOf(str) == 0
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
        // do not truncate "text" if it's no text (but a number or a boolean) or if there's no need for truncation
        if (!text.length || text.length <= max_length) {
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

    /**
     * @param   size    File size in bytes.
     */
    format_file_size: function(size) {
        var units = ["bytes", "KB", "MB", "GB"]
        for (var i = 0; i <= 2; i++) {
            if (size < 1024) {
                return result()
            }
            size /= 1024
        }
        return result()

        function result() {
            var decimals = Math.max(i - 1, 0)
            return js.round(size, decimals) + " " + units[i]
        }
    },

    absolute_http_url: function(url) {
        if (!js.begins_with(url, "http://") && !js.begins_with(url, "https://")) {
            return "http://" + url
        }
        return url
    },

    TextWrapper: function(text, max_width, line_height, ctx) {

        var wrapped_lines = []
        var box_width = 0, box_height   // size of the bounding box

        // cast if number or boolean
        if (!text.split) {
            text = text.toString()
        }
        //
        wrap_text()

        // ---

        this.draw = function(x, y, ctx) {
            var dy = y
            for (var i = 0, line; line = wrapped_lines[i]; i++) {
                ctx.fillText(line, x, dy)
                dy += line_height
            }
        }

        this.get_size = function() {
            return {width: box_width, height: box_height}
        }

        this.get_line_height = function() {
            return line_height
        }

        // ---

        function wrap_text() {
            var line = ""       // current line
            var line_width = 0  // width of current line
            var word_width      // width of current word
            var sep
            var space_width = ctx.measureText(" ").width
            var words = text.split(/([ \n])/)
            for (var i = 0; i < words.length; i += 2) {
                sep = separator()
                wrap_word(words[i - 1] == "\n", words[i])     // Note: words[-1] == undefined
                //
                if (line_width > box_width) {
                    box_width = line_width
                }
            }
            wrapped_lines.push(line)
            box_height = wrapped_lines.length * line_height

            function wrap_word(newline, word) {
                word_width = ctx.measureText(word).width
                if (newline) {
                    begin_new_line(word)
                } else if (line_width + sep.width + word_width <= max_width) {
                    append_to_line(word)
                } else {
                    begin_new_line(word)
                }
            }

            function append_to_line(word) {
                line += sep.char + word
                line_width += sep.width + word_width
            }

            function begin_new_line(word) {
                if (line) {
                    wrapped_lines.push(line)
                }
                line = word
                line_width = word_width
            }

            function separator() {
                return {
                    char:  line ? " " : "",
                    width: line ? space_width : 0
                }
            }
        }
    },



    // ***************
    // *** Network ***
    // ***************



    is_local_connection: function() {
        var hostname = location.hostname
        return hostname == "localhost" || hostname == "127.0.0.1"
    },



    // ***************
    // *** Cookies ***
    // ***************



    set_cookie: function(name, value) {
        /* var days = 2
        var expires = new Date()
        expires.setTime(expires.getTime() + days * 24 * 60 * 60 * 1000) */
        //
        // DeepaMehta note: the cookie's path must be explicitly set to "/". If not set the browser would set it to
        // "/de.deepamehta.webclient" (the "directory" of the page that loaded this script) and the cookie will not be
        // send back to the server for XHR requests as these are bound to "/core". ### FIXDOC: still true?
        // Vice versa we can't set the cookie's path to "/core" because it would not be accessible here at client-side.
        document.cookie = name + "=" + value + ";path=/" // + ";expires=" + expires.toGMTString()
    },

    /**
     * Returns a cookie value.
     *
     * @param   name    the name of the cookie, e.g. "dm4_workspace_id".
     *
     * @return  the cookie value (string) or undefined if no such cookie exist.
     */
    get_cookie: function(name) {
        // Note: document.cookie contains all cookies as one string, e.g. "dm4_workspace_id=123; dm4_topicmap_id=234"
        if (document.cookie.match(new RegExp("\\b" + name + "=(\\w*)"))) {
            return RegExp.$1
        }
    },

    remove_cookie: function(name) {
        // Note: setting the expire date to yesterday removes the cookie
        var days = -1
        var expires = new Date()
        expires.setTime(expires.getTime() + days * 24 * 60 * 60 * 1000)
        //
        document.cookie = name + "=;path=/;expires=" + expires.toGMTString()
    }
}
