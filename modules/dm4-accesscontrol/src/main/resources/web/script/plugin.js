dm4c.add_plugin("de.deepamehta.accesscontrol", function() {

    var ENCRYPTED_PASSWORD_PREFIX = "-SHA256-"
    var self = this

    dm4c.load_script("/de.deepamehta.accesscontrol/script/vendor/sha256.js")



    // === REST Client Extension ===

    dm4c.restc.login = function(authorization) {
        return this.request("POST", "/accesscontrol/login", undefined, {"Authorization": authorization})
    }
    dm4c.restc.logout = function() {
        return this.request("POST", "/accesscontrol/logout")
    }
    dm4c.restc.get_username = function() {
        return this.request("GET", "/accesscontrol/user", undefined, undefined, "text")
        // Note: response 204 No Content yields to null result
    }
    // ### FIXME: adapt to server-side
    dm4c.restc.get_topic_permissions = function(topic_id) {
        return this.request("GET", "/accesscontrol/topic/" + topic_id)
    }
    dm4c.restc.get_owned_topic = function(user_id, type_uri) {
        return this.request("GET", "/accesscontrol/owner/" + user_id + "/" + type_uri)
    }
    dm4c.restc.set_owner = function(topic_id, user_id) {
        this.request("POST", "/accesscontrol/topic/" + topic_id + "/owner/" + user_id)
    }
    dm4c.restc.create_acl_entry = function(topic_id, user_role_uri, permissions) {
        this.request("POST", "/accesscontrol/topic/" + topic_id + "/userrole/" + user_role_uri, permissions)
    }
    dm4c.restc.join_workspace = function(username, workspace_id) {
        this.request("POST", "/accesscontrol/user/" + username + "/workspace/" + workspace_id)
    }



    // === Webclient Listeners ===

    dm4c.add_listener("init", function() {

        var login_widget
        var login_dialog

        create_login_widget()
        create_login_dialog()

        function create_login_widget() {
            login_widget = new LoginWidget()
            dm4c.toolbar.dom.append(login_widget.dom)

            function LoginWidget() {
                var dom = $("<div>").attr({id: "login-widget"})    // attr("id", ...) doesn't create the div!
                var username = self.get_username()
                if (username) {
                    show_user(username)
                } else {
                    show_login()
                }
                this.dom = dom

                this.show_user = function(username) {
                    dom.empty()
                    show_user(username)
                }

                this.show_login = function() {
                    dom.empty()
                    show_login()
                }

                function show_user(username) {
                    dom.append("Logged in as ").append(username_link()).append("<br>")
                    dom.append(logout_link())

                    function username_link() {
                        return dm4c.render.link(username, function() {
                            var user_account = dm4c.restc.get_topic_by_value("dm4.accesscontrol.user_account", username)
                            dm4c.show_topic(new Topic(user_account), "show")
                        }, "Reveal the User Account topic")
                    }

                    function logout_link() {
                        return dm4c.render.link("Logout", function() {
                            try {
                                dm4c.restc.logout()
                                update_gui_logout()
                            } catch (e) {
                                shutdown_gui()
                            }
                        })
                    }
                }

                function show_login() {
                    dom.append(dm4c.render.link("Login", function() {
                        login_dialog.open()
                    }))
                }
            }
        }

        function create_login_dialog() {
            var username_input = $("<input>")
            var password_input = $("<input>").attr("type", "password")
            var message_div = $("<div>").attr("id", "login-message")
            var dialog_content = $("<div>").addClass("field-label").text("Username")
                .after(username_input)
                .after($("<div>").addClass("field-label").text("Password"))
                .after(password_input)
                .after(message_div)
            login_dialog = dm4c.ui.dialog({
                title: "Login",
                content: dialog_content,
                button_label: "OK",
                button_handler: do_try_login
            })
            //
            dm4c.on_return_key(username_input, function() {
                password_input.focus();
            })
            dm4c.on_return_key(password_input, function() {
                do_try_login();
            })

            function do_try_login() {
                try {
                    var username = username_input.val()
                    var password = password_input.val()
                    dm4c.restc.login(authorization())  // throws 401 if login fails
                    show_message("Login OK", "ok", close_login_dialog)
                    update_gui_login(username)
                } catch (e) {
                    show_message("Login failed", "failed")
                }

                /**
                 * Returns value for the "Authorization" header.
                 */
                function authorization() {
                    return "Basic " + btoa(username + ":" + password)   // ### FIXME: btoa() might not work in IE
                }
            }

            function show_message(message, css_class, callback) {
                message_div.fadeOut(200, function() {
                    $(this).text(message).removeClass().addClass(css_class).fadeIn(1000, callback)
                })
            }

            function close_login_dialog() {
                login_dialog.close(400, function() {
                    // clear fields for next re-open
                    username_input.val("")
                    password_input.val("")
                    message_div.text("")
                })
            }
        }

        // ---

        function update_gui_login(username) {
            // Note: the types must be reloaded *before* the logged_in event is fired.
            // Consider the Workspaces plugin: refreshing the workspace menu relies on the type cache.
            dm4c.reload_types(function() {
                // update view
                login_widget.show_user(username)
                dm4c.restore_selection()
                // fire event
                dm4c.fire_event("logged_in", username)
            })
        }

        function update_gui_logout() {
            // Note: the types must be reloaded *before* the logged_out event is fired.
            // Consider the Workspaces plugin: refreshing the workspace menu relies on the type cache.
            dm4c.reload_types(function() {
                // update view
                login_widget.show_login()
                dm4c.restore_selection()
                // fire event
                dm4c.fire_event("logged_out")
            })
        }

        // ---

        function shutdown_gui() {
            $("body").text("You're logged out now.")
        }
    })

    dm4c.add_listener("post_refresh_create_menu", function(type_menu) {
        if (!dm4c.has_create_permission("dm4.accesscontrol.user_account")) {
            return
        }
        //
        type_menu.add_separator()
        type_menu.add_item({label: "New User Account", handler: function() {
            dm4c.do_create_topic("dm4.accesscontrol.user_account");
        }})
    })

    dm4c.add_listener("pre_update_topic", function(topic_model) {
        if (topic_model.type_uri != "dm4.accesscontrol.user_account") {
            return
        }
        //
        var password_topic = topic_model.composite["dm4.accesscontrol.password"]
        var password = password_topic.value
        if (!js.begins_with(password, ENCRYPTED_PASSWORD_PREFIX)) {
            password_topic.value = encrypt_password(password)
        }
    })

    // ---

    dm4c.add_listener("has_write_permission_for_topic", function(topic) {
        var permissions = topic.composite["dm4.accesscontrol.permissions"]
        // error check
        if (!permissions) {
            throw "AccessControlError: topic " + topic.id + " (type_uri=\"" + topic.type_uri +
                "\") has no permissions info"
        }
        //
        return permissions.composite["dm4.accesscontrol.operation.write"].value
    })

    dm4c.add_listener("has_write_permission_for_association", function(assoc) {
        var permissions = assoc.composite["dm4.accesscontrol.permissions"]
        // error check
        if (!permissions) {
            throw "AccessControlError: association " + assoc.id + " (type_uri=\"" + assoc.type_uri +
                "\") has no permissions info"
        }
        //
        return permissions.composite["dm4.accesscontrol.operation.write"].value
    })

    dm4c.add_listener("has_create_permission", function(topic_type) {
        var permissions = topic_type.composite["dm4.accesscontrol.permissions"]
        // error check
        if (!permissions) {
            throw "AccessControlError: topic type \"" + topic_type.uri + "\" has no permissions info"
        }
        //
        return permissions.composite["dm4.accesscontrol.operation.create"].value
    })



    // ------------------------------------------------------------------------------------------------------ Public API

    this.create_user_account = function(username, password) {
        return dm4c.create_topic("dm4.accesscontrol.user_account", {
            "dm4.accesscontrol.username": username,
            "dm4.accesscontrol.password": encrypt_password(password)
        })
    }

    /**
     * Returns the username (string) of the logged in user, or null if no user is logged in.
     */
    this.get_username = function() {
        return dm4c.restc.get_username()
    }



    // ----------------------------------------------------------------------------------------------- Private Functions

    function encrypt_password(password) {
        return ENCRYPTED_PASSWORD_PREFIX + SHA256(password)
    }
})
