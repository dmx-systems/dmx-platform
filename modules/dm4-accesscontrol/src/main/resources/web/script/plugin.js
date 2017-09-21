dm4c.add_plugin("de.deepamehta.accesscontrol", function() {

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

    var ENCODED_PASSWORD_PREFIX = "-SHA256-"
    var system_workspace_id                     // constant
    var self = this

    dm4c.load_script("/de.deepamehta.accesscontrol/script/vendor/sha256.js")



    // === REST Client Extension ===

    dm4c.restc.login = function(authorization) {
        this.request("POST", "/accesscontrol/login", undefined, undefined, {"Authorization": authorization}, undefined,
            function() {return false})      // by returning false the error handler prevents the global error handler
    }
    dm4c.restc.logout = function() {
        this.request("POST", "/accesscontrol/logout")
    }
    dm4c.restc.get_username = function() {
        return this.request("GET", "/accesscontrol/user", undefined, undefined, undefined, "text")
        // Note: response 204 No Content yields to undefined result
    }
    dm4c.restc.get_username_topic = function() {
        return this.request("GET", "/accesscontrol/username")
        // Note: response 204 No Content yields to undefined result
    }
    dm4c.restc.create_user_account = function(username, password) { // password is expected to be SHA256 encoded
        return this.request("POST", "/accesscontrol/user_account", {username: username, password: password})
    }
    dm4c.restc.get_workspace_owner = function(workspace_id) {
        return this.request("GET", "/accesscontrol/workspace/" + workspace_id + "/owner",
            undefined, undefined, undefined, "text")
    }
    dm4c.restc.create_membership = function(username, workspace_id) {
        this.request("POST", "/accesscontrol/user/" + username + "/workspace/" + workspace_id)
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
    dm4c.restc.get_modifier = function(object_id) {
        return this.request("GET", "/accesscontrol/object/" + object_id + "/modifier",
            undefined, undefined, undefined, "text")
    }
    dm4c.restc.get_methods = function() {
        return this.request("GET", "/accesscontrol/methods")
    }



    // === Webclient Listeners ===

    dm4c.add_listener("init", function() {

        var login_widget
        var authmethods = dm4c.restc.get_methods();
        console.log(authmethods);
        
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
            var authmethod_dropdown = dm4c.ui.menu(do_select_topicmap)
            var authmethod_value = "Basic";
            if (authmethods.length > 0) {
                authmethod_dropdown.add_item({label:"Basic"})
                authmethods.map(function(item){
                    authmethod_dropdown.add_item({label:item})
                })
            }
            var username_input = $("<input>")
            var password_input = $("<input>").attr("type", "password")
            var message_div = $("<div>").attr("id", "login-message")
            var dialog_content = dm4c.render.label("Username")
                .add(authmethod_dropdown.dom)
                .add(username_input)
                .add(dm4c.render.label("Password")).add(password_input)
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

            function do_select_topicmap(e) {
                authmethod_value = e.label;
                console.log ("do_select_topicmap", e)
            }

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
                    return authmethod_value + " " + btoa(username + ":" + password)   // ### FIXME: btoa() might not work in IE
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
            // update model
            increase_authority(username)
            // update view
            login_widget.show_user(username)
        }

        function update_gui_logout() {
            // update model
            decrease_authority(true)    // is_logout=true
            // update view
            login_widget.show_login()
        }

        // ---

        function shutdown_gui() {
            $("body").text("You're logged out now.")
        }
    })

    // ---

    dm4c.add_listener("topic_commands", function(topic) {
        return [
            {
                is_separator: true,
                context: "context-menu"
            },
            {
                label:   "Get Info",
                handler: function() {
                    open_info_dialog("Topic Info", topic.id, topic.type_uri == "dm4.workspaces.workspace")
                },
                context: "context-menu"
            }
        ]
    })

    dm4c.add_listener("association_commands", function(assoc) {
        return [
            {
                is_separator: true,
                context: "context-menu"
            },
            {
                label:   "Get Info",
                handler: function() {
                    open_info_dialog("Association Info", assoc.id)
                },
                context: "context-menu"
            }
        ]
    })

    dm4c.add_listener("post_refresh_create_menu", function(type_menu) {
        // Note: the toolbar's Create menu is only refreshed when the login status changes, not when a workspace is
        // selected. (At workspace selection time the Create menu is not refreshed but shown/hidden in its entirety.)
        // So, we check the READ permission here, not the CREATE permission. (The CREATE permission involves the
        // WRITEability of the selected workspace.) ### FIXDOC
        if (!init_system_workspace_id() || !dm4c.has_write_permission_for_topic(system_workspace_id)) {
            return
        }
        //
        type_menu.add_separator()
        type_menu.add_item({label: "New User Account", handler: do_open_new_user_account_dialog})

        function do_open_new_user_account_dialog() {
            var username_input = dm4c.render.input()
            var password_input = dm4c.render.input()
            dm4c.ui.dialog({
                title: "New User Account",
                content: dm4c.render.label("Username").add(username_input)
                    .add(dm4c.render.label("Password")).add(password_input),
                button_label: "Create",
                button_handler: do_create_user_account
            })

            function do_create_user_account() {
                var username = username_input.val()
                var password = password_input.val()
                var username_topic = self.create_user_account(username, password);
                dm4c.show_topic(username_topic, "show", undefined, true)    // coordinates=undefined, do_center=true
            }
        }
    })

    dm4c.add_listener("pre_update_topic", function(topic_model) {
        if (topic_model.type_uri == "dm4.accesscontrol.user_account") {
            var password_topic = topic_model.childs["dm4.accesscontrol.password"]
            var password = password_topic.value
            if (!js.begins_with(password, ENCODED_PASSWORD_PREFIX)) {
                password_topic.value = encode_password(password)
            }
        }
    })

    dm4c.add_listener("post_update_association", function(assoc, old_assoc) {
        // Note: old_assoc is undefined if the updated association is NOT the selected one
        if (assoc.type_uri != "dm4.accesscontrol.membership" && old_assoc) {
            check_membership(old_assoc)
        }
    })

    dm4c.add_listener("post_delete_association", function(assoc) {
        check_membership(assoc)
    })

    // ---

    dm4c.add_listener("has_write_permission_for_topic", function(topic_id) {
        return get_topic_permissions(topic_id)["dm4.accesscontrol.operation.write"]
    })

    dm4c.add_listener("has_write_permission_for_association", function(assoc_id) {
        return get_association_permissions(assoc_id)["dm4.accesscontrol.operation.write"]
    })

    dm4c.add_listener("has_retype_permission_for_association", function(assoc, assoc_type) {
        // ### TODO: enforce the retype policy at *server-side*
        if (assoc_type.uri == "dm4.accesscontrol.membership") {
            if (assoc.matches("dm4.workspaces.workspace", "dm4.accesscontrol.username")) {
                var workspace = assoc.get_topic_by_type("dm4.workspaces.workspace")
                return dm4c.has_write_permission_for_topic(workspace.id)
            }
            return false
        }
        return true
    })



    // ------------------------------------------------------------------------------------------------------ Public API

    /**
     * Returns the username (string) of the logged in user, or undefined if no user is logged in.
     */
    this.get_username = function() {
        return dm4c.restc.get_username()
    }

    /**
     * Returns the Username topic of the logged in user, or undefined if no user is logged in.
     */
    this.get_username_topic = function() {
        return dm4c.restc.get_username_topic()
    }

    // ---

    /**
     * @param   password    unencoded
     */
    this.create_user_account = function(username, password) {
        return new Topic(dm4c.restc.create_user_account(username, encode_password(password)))
    }



    // ----------------------------------------------------------------------------------------------- Private Functions

    function init_system_workspace_id() {
        if (!system_workspace_id) {
            // Note: if not logged in the System workspace is not readable.
            // The get_workspace() call would fail with error 500.
            if (self.get_username()) {
                system_workspace_id = dm4c.get_plugin("de.deepamehta.workspaces")
                    .get_workspace("dm4.workspaces.system").id
            }
        }
        return system_workspace_id
    }

    function encode_password(password) {
        return ENCODED_PASSWORD_PREFIX + SHA256(password)
    }

    /**
     * Checks if the given association is a membership and if at the username end is the current user.
     * If so the Webclient is updated to reflect a decrease in authority.
     */
    function check_membership(assoc) {
        if (assoc.type_uri == "dm4.accesscontrol.membership") {
            var username_topic = assoc.get_topic_by_type("dm4.accesscontrol.username")
            if (username_topic.value == self.get_username()) {
                decrease_authority()
            }
        }
    }

    // ---

    function increase_authority(username) {
        // Note: the types must be reloaded *before* the logged_in event is fired.
        // Consider the Workspaces plugin: refreshing the workspace menu relies on the type cache.
        dm4c.reload_types(function() {
            // update model
            clear_permissions_cache()
            // signal authority change
            dm4c.fire_event("logged_in", username)
        })
    }

    function decrease_authority(is_logout) {
        // Note: the types must be reloaded *before* the authority_decreased events are fired.
        // Consider the Workspaces plugin: refreshing the workspace menu relies on the type cache.
        dm4c.reload_types(function() {
            // update model
            clear_permissions_cache()
            // signal authority change
            if (is_logout) {
                dm4c.fire_event("logged_out")
            }
            dm4c.fire_event("authority_decreased")
            dm4c.fire_event("authority_decreased_2")
        })
    }

    // === Info Dialog ===

    function open_info_dialog(title, object_id, is_workspace) {
        var created  = dm4c.restc.get_creation_time(object_id)
        var modified = dm4c.restc.get_modification_time(object_id)
        var creator  = dm4c.restc.get_creator(object_id)
        var modifier = dm4c.restc.get_modifier(object_id)
        //
        if (is_workspace) {
            title = "Workspace Info"
            var workspace_id = object_id
        } else {
            var workspace = dm4c.restc.get_assigned_workspace(object_id)
            var workspace_id = workspace && workspace.id
        }
        var owner = workspace_id && dm4c.restc.get_workspace_owner(workspace_id)
        //
        var content = dm4c.render.label("Created").add($("<div>").text(new Date(created) + " by " + creator))
            .add(dm4c.render.label("Modified")).add($("<div>").text(new Date(modified) + " by " + modifier))
            .add(dm4c.render.label("Owner")).add($("<div>").text("" + owner))
        //
        dm4c.ui.dialog({
            title: title,
            content: content
        })
    }

    // === Permissions Cache ===

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

// Enable debugging for dynamically loaded scripts:
//# sourceURL=access_control_plugin.js
