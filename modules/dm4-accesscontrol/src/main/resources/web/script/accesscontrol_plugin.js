function accesscontrol_plugin() {

    var DEFAULT_USER = "admin"
    var DEFAULT_PASSWORD = ""
    var ENCRYPTED_PASSWORD_PREFIX = "-SHA256-"  // don't change this

    dm4c.load_stylesheet("/de.deepamehta.accesscontrol/style/accesscontrol.css")
    dm4c.load_script("/de.deepamehta.accesscontrol/script/vendor/sha256.js")

    var self = this



    // === REST Client Extension ===

    /**
     * @return  a User Account topic, or null if there is no such user
     */
    dm4c.restc.lookup_user_account = function(username) {
        return this.request("GET", "/accesscontrol/user/" + username)
    }
    dm4c.restc.get_user_account = function() {
        return this.request("GET", "/accesscontrol/user")
    }
    dm4c.restc.get_owned_topic = function(user_id, type_uri) {
        return this.request("GET", "/accesscontrol/owner/" + user_id + "/" + type_uri)
    }
    dm4c.restc.set_owner = function(topic_id, user_id) {
        return this.request("POST", "/accesscontrol/topic/" + topic_id + "/owner/" + user_id)
    }
    dm4c.restc.create_acl_entry = function(topic_id, role, permissions) {
        return this.request("POST", "/accesscontrol/topic/" + topic_id + "/role/" + role, permissions)
    }
    dm4c.restc.join_workspace = function(workspace_id, user_id) {
        return this.request("POST", "/accesscontrol/user/" + user_id + "/" + workspace_id)
    }



    // === Webclient Listeners ===

    dm4c.register_listener("init", function() {

        dm4c.toolbar.special_menu.add_item({label: menu_item_label(), value: "loginout-item", handler: do_handle_menu})
        create_login_dialog()

        function create_login_dialog() {
            var username_input = $("<input>")
            var password_input = $("<input>").attr("type", "password")
            var message_div = $("<div>").attr("id", "login-message")
            var dialog_content = $("<div>").addClass("field-label").text("Username")
                .after(username_input)
                .after($("<div>").addClass("field-label").text("Password"))
                .after(password_input)
                .after(message_div)
            dm4c.ui.dialog("login-dialog", "Login", dialog_content, "auto", "OK", do_login)

            function do_login() {
                var username = username_input.val()
                var password = password_input.val()
                var user = try_login(username, password)
                if (user) {
                    show_message("Login OK", "login-ok", close_login_dialog)
                    self.login(user)
                } else {
                    show_message("Login failed", "login-failed")
                }
            }

            function show_message(message, css_class, callback) {
                message_div.fadeOut(200, function() {
                    $(this).text(message).removeClass().addClass(css_class).fadeIn(1000, callback)
                })
            }

            function close_login_dialog() {
                $("#login-dialog").parent().fadeOut(400, function() {
                    $("#login-dialog").dialog("close")
                    // clear fields for possible re-open
                    username_input.val("")
                    password_input.val("")
                    message_div.text("")
                })
            }
        }

        function do_handle_menu(item) {
            if (item.label == "Login...") {
                $("#login-dialog").dialog("open")
            } else if (item.label == "Logout \"" + get_username() + "\"") {
                self.logout()
            } else {
                alert("Unexpected menu item: " + js.stringify(item))
            }
        }
    })

    // ---

    dm4c.register_listener("has_write_permission", function(topic) {
        return topic.composite["dm4.accesscontrol.permissions"]
                    .composite["dm4.accesscontrol.operation_write"].value
    })

    dm4c.register_listener("has_create_permission", function(topic_type) {
        return topic_type.composite["dm4.accesscontrol.permissions"]
                         .composite["dm4.accesscontrol.operation_create"].value
    })



    // === Access Control Listeners ===

    dm4c.register_listener("user_logged_in", function(user) {
        refresh_menu_item()
        dm4c.render_topic()
    })

    dm4c.register_listener("user_logged_out", function() {
        refresh_menu_item()
        dm4c.render_topic()
    })



    // ------------------------------------------------------------------------------------------------------ Public API

    this.create_user = function(username, password) {
        return dm4c.create_topic("dm4.accesscontrol.user_account", {
            "dm4.accesscontrol.username": username,
            "dm4.accesscontrol.password": encrypt_password(password)
        })
    }

    // ---

    this.get_user_account = function() {
        return dm4c.restc.get_user_account()
    }

    this.get_owned_topic = function(user_id, type_uri) {
        return dm4c.restc.get_owned_topic(user_id, type_uri)
    }

    this.set_owner = function(topic_id, user_id) {
        dm4c.restc.set_owner(topic_id, user_id)
    }

    this.create_acl_entry = function(topic_id, role, permissions) {
        dm4c.restc.create_acl_entry(topic_id, role, permissions)
    }

    this.join_workspace = function(workspace_id, user_id) {
        dm4c.restc.join_workspace(workspace_id, user_id)
    }

    // ---

    this.login = function(user) {
        var username = user.get("dm4.accesscontrol.username")
        js.set_cookie("dm4_username", username)
        //
        adjust_create_widget()
        //
        dm4c.trigger_hook("user_logged_in", user)
    }

    this.logout = function() {
        js.remove_cookie("dm4_username")
        //
        adjust_create_widget()
        //
        dm4c.trigger_hook("user_logged_out")
    }



    // ----------------------------------------------------------------------------------------------- Private Functions

    function try_login(username, password) {
        var user = dm4c.restc.lookup_user_account(username)
        if (user) {
            user = new Topic(user)
            if (user.get("dm4.accesscontrol.password") == encrypt_password(password)) {
                return user
            }
        }
    }

    /**
     * Returns the username of the logged in user, or null/undefined if no user is logged in.
     */
    function get_username() {
        return js.get_cookie("dm4_username")
    }

    function encrypt_password(password) {
        return ENCRYPTED_PASSWORD_PREFIX + SHA256(password)
    }

    // ---

    function refresh_menu_item() {
        dm4c.ui.set_menu_item_label("special-menu", "loginout-item", menu_item_label())
    }

    function menu_item_label() {
        return get_username() ? "Logout \"" + get_username() + "\"" : "Login..."
    }

    // ---

    function adjust_create_widget() {
        dm4c.reload_types()
        var menu = dm4c.recreate_type_menu("create-type-menu")
        if (menu.get_item_count()) {
            $("#create-widget").show()
        } else {
            $("#create-widget").hide()
        }
    }
}
