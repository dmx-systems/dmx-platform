dm4c.add_plugin("de.deepamehta.accesscontrol", function() {

    var ENCRYPTED_PASSWORD_PREFIX = "-SHA256-"
    var self = this

    dm4c.load_script("/de.deepamehta.accesscontrol/script/vendor/sha256.js")



    // === REST Client Extension ===

    dm4c.restc.login = function(authorization) {
        this.request("POST", "/accesscontrol/login", undefined, undefined, {"Authorization": authorization})
    }
    dm4c.restc.logout = function() {
        this.request("POST", "/accesscontrol/logout")
    }
    dm4c.restc.get_username = function() {
        return this.request("GET", "/accesscontrol/user", undefined, undefined, undefined, "text")
        // Note: response 204 No Content yields to null result
    }
    dm4c.restc.get_topic_permissions = function(topic_id) {
        return this.request("GET", "/accesscontrol/topic/" + topic_id)
    }
    dm4c.restc.get_association_permissions = function(assoc_id) {
        return this.request("GET", "/accesscontrol/association/" + assoc_id)
    }
    dm4c.restc.get_creator = function(object_id) {
        return this.request("GET", "/accesscontrol/object/" + object_id + "/creator",
            undefined, undefined, undefined, "text")
    }
    dm4c.restc.get_owner = function(object_id) {
        return this.request("GET", "/accesscontrol/object/" + object_id + "/owner",
            undefined, undefined, undefined, "text")
    }
    dm4c.restc.get_modifier = function(object_id) {
        return this.request("GET", "/accesscontrol/object/" + object_id + "/modifier",
            undefined, undefined, undefined, "text")
    }
    dm4c.restc.join_workspace = function(username, workspace_id) {
        this.request("POST", "/accesscontrol/user/" + username + "/workspace/" + workspace_id)
    }



    // === Webclient Listeners ===

    dm4c.add_listener("init", function() {

        var login_widget

        create_login_widget()

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
                            var user_account = dm4c.restc.get_topic_by_value("dm4.accesscontrol.user_account", username,
                                true)   // include_childs=true
                            dm4c.show_topic(new Topic(user_account), "show", undefined, true)   // coordinates=undefined
                                                                                                // do_center=true
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
                    dom.append(dm4c.render.link("Login", do_open_login_dialog))
                }
            }
        }

        function do_open_login_dialog() {
            var username_input = $("<input>")
            var password_input = $("<input>").attr("type", "password")
            var message_div = $("<div>").attr("id", "login-message")
            var dialog_content = $("<div>").addClass("field-label").text("Username")
                .add(username_input)
                .add($("<div>").addClass("field-label").text("Password"))
                .add(password_input)
                .add(message_div)
            // Note: as of jQuery 1.9 you can't add objects to a disconnected (not in a document)
            // jQuery object with the after() method. Use add() instead.
            var login_dialog = dm4c.ui.dialog({
                title: "Login",
                content: dialog_content,
                button_label: "OK",
                button_handler: do_try_login,
                auto_close: false
            })
            //
            dm4c.on_return_key(username_input, function() {
                password_input.focus()
                return false    // stop propagation to prevent the dialog from invoking the button handler
            })

            function do_try_login() {
                try {
                    var username = username_input.val()
                    var password = password_input.val()
                    dm4c.restc.login(authorization())   // throws 401 if login fails
                    show_message("Login OK", "ok", function() {
                        login_dialog.close(400)
                    })
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
        }

        // ---

        function update_gui_login(username) {
            // Note: the types must be reloaded *before* the logged_in event is fired.
            // Consider the Workspaces plugin: refreshing the workspace menu relies on the type cache.
            dm4c.reload_types(function() {
                // update model
                clear_permissions_cache()
                // update view
                login_widget.show_user(username)
                // signal login status change
                dm4c.fire_event("logged_in", username)
            })
        }

        function update_gui_logout() {
            // Note: the types must be reloaded *before* the logged_out event is fired.
            // Consider the Workspaces plugin: refreshing the workspace menu relies on the type cache.
            dm4c.reload_types(function() {
                // update model
                clear_permissions_cache()
                // update view
                login_widget.show_login()
                // signal login status change
                dm4c.fire_event("logged_out")
                dm4c.fire_event("logged_out_2")
            })
        }

        // ---

        function shutdown_gui() {
            $("body").text("You're logged out now.")
        }
    })

    dm4c.add_listener("post_refresh_create_menu", function(type_menu) {
        // Note: the toolbar's Create menu is only refreshed when the login status changes, not when a workspace is
        // selected. (At workspace selection time the Create menu is not refreshed but shown/hidden in its entirety.)
        // So, we check the READ permission here, not the CREATE permission. (The CREATE permission involves the
        // WRITEability of the selected workspace.)
        if (!dm4c.has_read_permission("dm4.accesscontrol.user_account")) {
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
        var password_topic = topic_model.childs["dm4.accesscontrol.password"]
        var password = password_topic.value
        if (!js.begins_with(password, ENCRYPTED_PASSWORD_PREFIX)) {
            password_topic.value = encrypt_password(password)
        }
    })

    // ---

    dm4c.add_listener("has_write_permission_for_topic", function(topic) {
        return get_topic_permissions(topic.id)["dm4.accesscontrol.operation.write"]
    })

    dm4c.add_listener("has_write_permission_for_association", function(assoc) {
        return get_association_permissions(assoc.id)["dm4.accesscontrol.operation.write"]
    })

    // ### TODO: make the types cachable (like topics/associations). That is, don't deliver the permissions along
    // with the types (in the child topics). Instead let the client request the permissions separately.

    // ### TODO: add the same for association types
    dm4c.add_listener("has_read_permission", function(topic_type) {
        return is_topic_type_readable(topic_type.uri)
    })

    // ### TODO: add the same for association types
    dm4c.add_listener("has_create_permission", function(topic_type) {
        return is_topic_type_readable(topic_type.uri) && is_workspace_writable()
    })



    // ------------------------------------------------------------------------------------------------------ Public API

    /**
     * Returns the username (string) of the logged in user, or null if no user is logged in.
     */
    this.get_username = function() {
        return dm4c.restc.get_username()
    }

    this.create_user_account = function(username, password) {
        return dm4c.create_topic("dm4.accesscontrol.user_account", {
            "dm4.accesscontrol.username": username,
            "dm4.accesscontrol.password": encrypt_password(password)
        })
    }

    this.is_workspace_writable = function() {
        return is_workspace_writable()
    }



    // ----------------------------------------------------------------------------------------------- Private Functions

    function is_topic_type_readable(topic_type_uri) {
        // Note: at startup the webclient loads all readable types into the type cache
        return dm4c.has_topic_type(topic_type_uri)
    }

    function is_workspace_writable() {
        var workspace_id = dm4c.get_plugin("de.deepamehta.workspaces").get_workspace_id()
        return get_topic_permissions(workspace_id)["dm4.accesscontrol.operation.write"]
    }

    // ---

    function encrypt_password(password) {
        return ENCRYPTED_PASSWORD_PREFIX + SHA256(password)
    }

    // === Permissions Cache ===

    /**
     * Key is a topic/association ID.
     * Value is an object:
     *     {
     *         "dm4.accesscontrol.operation.write": true
     *     }
     *
     * Note: at client-side there is no explicit READ permission.
     * The Webclient never gets hold of an object the user is not allowed to read.
     * The server would not send it in the first place.
     */
    var permissions_cache = {}

    function get_topic_permissions(topic_id) {
        return get_permissions(topic_id, dm4c.restc.get_topic_permissions, "topic")
    }

    function get_association_permissions(assoc_id) {
        return get_permissions(assoc_id, dm4c.restc.get_association_permissions, "association")
    }

    function get_permissions(object_id, retrieve_function, object_info) {
        var permissions = permissions_cache[object_id]
        if (!permissions) {
            permissions = retrieve_function.call(dm4c.restc, object_id)
            // error check
            if (!permissions || permissions["dm4.accesscontrol.operation.write"] == undefined) {
                throw "AccessControlError: invalid permissions info for " + object_info + " " + object_id
            }
            permissions_cache[object_id] = permissions
        }
        return permissions
    }

    function clear_permissions_cache() {
        permissions_cache = {}
    }
})
