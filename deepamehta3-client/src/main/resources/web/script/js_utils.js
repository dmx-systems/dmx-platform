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
    includes: function(array, fn) {
        for (var i = 0, e; e = array[i]; i++) {
            if (fn(e)) {
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
     * Copies all attributes from source object to destination object.
     */
    copy: function(src_obj, dst_obj) {
        for (var key in src_obj) {
            dst_obj[key] = src_obj[key]
        }
    },

    /**
     * Constructs a new object dynamically.
     *
     * @param   class_name  Name of class.
     * @param   <varargs>   Variable number of arguments. Passed to the constructor.
     */
    new_object: function(class_name) {
        if (arguments.length == 1) {
            return new Function("return new " + class_name)()
        } else if (arguments.length == 2) {
            return new Function("arg1", "return new " + class_name + "(arg1)")(arguments[1])
        } else if (arguments.length == 3) {
            return new Function("arg1", "arg2", "return new " + class_name + "(arg1, arg2)")(arguments[1], arguments[2])
        } else if (arguments.length == 4) {
            return new Function("arg1", "arg2", "arg3", "return new " + class_name + "(arg1, arg2, arg3)")
                                                                            (arguments[1], arguments[2], arguments[3])
        } else {
            alert("ERROR (new_object): too much arguments (" +
                (arguments.length - 1) + "), maximum is 3.\nclass_name=" + class_name)
        }
    },



    // ************
    // *** Text ***
    // ************



    render_text: function(text) {
        return text.replace ? text.replace(/\n/g, "<br>") : text
    },

    /**
     * "vendor/dm3-time/script/dm3-time.js" -> "dm3-time"
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



    // ***************
    // *** Cookies ***
    // ***************



    set_cookie: function(key, value) {
        /* var days = 2
        var expires = new Date()
        expires.setTime(expires.getTime() + days * 24 * 60 * 60 * 1000) */
        //
        // DeepaMehta note: the cookie's path must be explicitly set to "/". If not set the browser would set it to
        // "/de.deepamehta.3-client" (the "directory" of the page that loaded this script) and the cookie will not be
        // send back to the server for XHR requests as these are bound to "/core".
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
