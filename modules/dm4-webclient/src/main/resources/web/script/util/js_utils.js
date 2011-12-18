/**
 * Generic JavaScript Utilities.
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

    /**
     * Copies all properties from source object to destination object.
     * ### not in use
    copy: function(src_obj, dst_obj) {
        for (var key in src_obj) {
            dst_obj[key] = src_obj[key]
        }
    },*/



    // ***************
    // *** Classes ***
    // ***************



    /**
     * Constructs a new object dynamically.
     *
     * @param   class_name  Name of class.
     * @param   <varargs>   Variable number of arguments. Passed to the constructor.
     */
    new_object: function(class_name) {
        var args = []
        var argvals = []
        for (var i = 1; i < arguments.length; i++) {
            args.push("arg" + i)
            argvals.push(arguments[i])
        }
        var argstr = args.join()
        //
        return new Function(argstr, "return new " + class_name + "(" + argstr + ")").apply(undefined, argvals)
    },

    /**
     * ### FIXME: drop this method? eval() is evil.
     *
     * @param   superclass      the baseclass constructor (a function)
     * @param   subclass_name   (a string)
     */
    instantiate: function(superclass, subclass_name) {
        var obj = new superclass()
        eval(subclass_name).apply(obj)
        return obj
    },

    /**
     * Extends an object with all the methods defined in a superclass.
     *
     * @param   obj         The object to be extended
     * @param   superclass  The superclass (a function)
     */
    extend: function(obj, superclass) {
        superclass.apply(obj)
    },



    // ************
    // *** Text ***
    // ************



    begins_with: function(text, str) {
        return text.search("^" + str) != -1
    },

    render_text: function(text) {
        return text.replace ? text.replace(/\n/g, "<br>") : text
    },

    /**
     * "vendor/dm4-time/script/dm4-time.js" -> "dm4-time"
     */
    basename: function(path) {
        path.match(/.*\/(.*)\..*/)
        return RegExp.$1
    },

    filename: function(path) {
        path.match(/.*\/(.*)/)
        return RegExp.$1
    },

    filename_ext: function(path) {
        return path.substr(path.lastIndexOf(".") + 1)
    },

    to_camel_case: function(str) {
        var res = ""
        var words = str.split("_")
        for (var i = 0, word; word = words[i]; i++) {
            res += word[0].toUpperCase()
            res += word.substr(1)
        }
        return res
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
            var width = 0   // current line's width
            var words = text.split(" ")
            for (var i = 0, word; word = words[i]; i++) {
                wrap_word(word + " ")
            }
            wrapped_lines.push(line)

            function wrap_word(word) {
                var w = ctx.measureText(word).width
                if (width + w <= max_width) {
                    line  += word
                    width += w
                } else {
                    if (line) {
                        wrapped_lines.push(line)
                    }
                    line = word
                    width = w
                }
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
