function accesscontrol_plugin() {

    var DEFAULT_USER = "admin"
    var DEFAULT_PASSWORD = ""
    var ENCRYPTED_PASSWORD_PREFIX = "-SHA256-"  // don't change this

    dm3c.css_stylesheet("/de.deepamehta.3-accesscontrol/style/dm3-accesscontrol.css")
    dm3c.javascript_source("/de.deepamehta.3-accesscontrol/script/vendor/sha256.js")

    var self = this

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ******************************************************
    // *** Client Hooks (triggered by deepamehta3-client) ***
    // ******************************************************



    this.init = function() {

        dm3c.add_to_special_menu({value: "loginout-item", label: menu_item_label()})
        create_login_dialog()
        extend_rest_client()

        function create_login_dialog() {
            var login_dialog = $("<div>").attr("id", "login-dialog")
            var login_message = $("<div>").attr("id", "login-message").html("&nbsp;")
            login_dialog.append($("<div>").addClass("field-name").text("Username"))
            login_dialog.append($("<input>").attr({id: "login-username"}))
            login_dialog.append($("<div>").addClass("field-name").text("Password"))
            login_dialog.append($("<input>").attr({id: "login-password", type: "password"}))
            // Note: purpose of the login message container is maintaining the space
            // when the login message is faded out (display=none)
            login_dialog.append($("<div>").attr("id", "login-message-container").append(login_message))
            $("body").append(login_dialog)
            $("#login-message-container").height($("#login-message").height())
            $("#login-dialog").dialog({
                title: "Login", buttons: {"OK": do_login},
                modal: true, autoOpen: false, closeOnEscape: true, draggable: false, resizable: false
            })
        }

        function do_login() {
            var username = $("#login-username").val()
            var password = $("#login-password").val()
            var user = lookup_user(username, password)
            if (user) {
                show_message("Login OK", "login-ok", close_login_dialog)
                self.login(user)
            } else {
                show_message("Login failed", "login-failed")
            }

            function close_login_dialog() {
                $("#login-dialog").parent().fadeOut(400, function() {
                    $("#login-dialog").dialog("close")
                    // clear fields for possible re-open
                    $("#login-username").val("")
                    $("#login-password").val("")
                    $("#login-message").text("")
                })
            }
        }

        function show_message(message, css_class, fn) {
            $("#login-message").fadeOut(200, function() {
                $(this).text(message).removeClass().addClass(css_class).fadeIn(1000, fn)
            })
        }

        function extend_rest_client() {
            dm3c.restc.get_user = function() {
                return this.request("GET", "/accesscontrol/user")
            }
            dm3c.restc.get_topic_by_owner = function(user_id, type_uri) {
                return this.request("GET", "/accesscontrol/owner/" + user_id + "/" + encodeURIComponent(type_uri))
            }
            dm3c.restc.set_owner = function(topic_id, user_id) {
                return this.request("POST", "/accesscontrol/topic/" + topic_id + "/owner/" + user_id)
            }
            dm3c.restc.create_acl_entry = function(topic_id, role, permissions) {
                return this.request("POST", "/accesscontrol/acl/" + topic_id, {role: role, permissions: permissions})
            }
            dm3c.restc.join_workspace = function(workspace_id, user_id) {
                return this.request("POST", "/accesscontrol/user/" + user_id + "/" + workspace_id)
            }
        }
    }

    this.handle_special_command = function(label) {
        if (label == "Login...") {
            $("#login-dialog").dialog("open")
        } else if (label == "Logout \"" + get_username() + "\"") {
            this.logout()
        }
    }

    // ---

    this.has_write_permission = function(topic) {
        return topic.permissions.write
    }

    this.has_create_permission = function(topic_type) {
        return topic_type.permissions.create
    }



    // *********************************************************************
    // *** Access Control Hooks (triggered by deepamehta3-accesscontrol) ***
    // *********************************************************************



    this.user_logged_in = function(user) {
        refresh_menu_item()
        dm3c.render_topic()
    }

    this.user_logged_out = function() {
        refresh_menu_item()
        dm3c.render_topic()
    }



    // ******************
    // *** Public API ***
    // ******************



    this.create_user = function(username, password) {
        var properties = {
            "de/deepamehta/core/property/username": username,
            "de/deepamehta/core/property/password": encrypt_password(password)
        }
        return dm3c.create_topic("de/deepamehta/core/topictype/user", properties)
    }

    // ---

    this.get_user = function() {
        return dm3c.restc.get_user()
    }

    this.get_topic_by_owner = function(user_id, type_uri) {
        return dm3c.restc.get_topic_by_owner(user_id, type_uri)
    }

    this.set_owner = function(topic_id, user_id) {
        dm3c.restc.set_owner(topic_id, user_id)
    }

    this.create_acl_entry = function(topic_id, role, permissions) {
        dm3c.restc.create_acl_entry(topic_id, role, permissions)
    }

    this.join_workspace = function(workspace_id, user_id) {
        dm3c.restc.join_workspace(workspace_id, user_id)
    }

    // ---

    this.login = function(user) {
        var username = user.properties["de/deepamehta/core/property/username"]
        js.set_cookie("dm3_username", username)
        //
        adjust_create_widget()
        //
        dm3c.trigger_hook("user_logged_in", user)
    }

    this.logout = function() {
        js.remove_cookie("dm3_username")
        //
        adjust_create_widget()
        //
        dm3c.trigger_hook("user_logged_out")
    }



    // ----------------------------------------------------------------------------------------------- Private Functions

    function lookup_user(username, password) {
        var user = dm3c.restc.get_topic_by_property("de/deepamehta/core/property/username", username)
        if (user && user.properties["de/deepamehta/core/property/password"] == encrypt_password(password)) {
            return user
        }
    }

    /**
     * Returns the username of the logged in user, or null/undefined if no user is logged in.
     */
    function get_username() {
        return js.get_cookie("dm3_username")
    }

    function encrypt_password(password) {
        return ENCRYPTED_PASSWORD_PREFIX + SHA256(password)
    }

    // ---

    function refresh_menu_item() {
        dm3c.ui.set_menu_item_label("special-menu", "loginout-item", menu_item_label())
    }

    function menu_item_label() {
        return get_username() ? "Logout \"" + get_username() + "\"" : "Login..."
    }

    // ---

    function adjust_create_widget() {
        dm3c.reload_types()
        var menu = dm3c.recreate_type_menu("create-type-menu")
        if (menu.get_item_count()) {
            $("#create-widget").show()
        } else {
            $("#create-widget").hide()
        }
    }
}
