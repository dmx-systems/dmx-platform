/**
 * A generic (DeepaMehta independent) GUI toolkit to create buttons, menus, context menus, combo boxes,
 * dialog boxes, and prompts. Based on jQuery UI.
 *
 * The DeepaMehta Webclient's GUIToolkit instance is accessible as dm4c.ui
 */
function GUIToolkit(config) {

    var gui = this



    // === Button ===

    /**
     * Creates and returns a jQuery UI button.
     *
     * @param   handler     The callback function. The generic JavaScript event arguments are passed to it.
     * @param   label       Optional: the button label (string).
     * @param   icon        Optional: the button icon (string).
     * @param   is_submit   Optional: if true a submit button is created (boolean).
     *
     * @return              The button (a jQuery object).
     */
    this.button = function(handler, label, icon, is_submit) {
        //
        if (!handler) {
            alert("WARNING (GUIToolkit.button): No handler specified for button");
        }
        //
        var button = $('<button type="' + (is_submit ? "submit" : "button") + '">').click(handler)
        // build options
        var options = {}
        if (label) {
            options.label = label
        } else {
            options.text = false
        }
        if (icon) {
            options.icons = {primary: "ui-icon-" + icon}
        }
        // create button
        return button.button(options)
    }



    // === Dialog Box ===

    /**
     * @param   config  an object with these properties:
     *                      id             - Optional: the dialog content div will get this ID. Useful to style the
     *                                       dialog with CSS. If not specified no "id" attribute is set.
     *                      title          - Optional: the dialog title. If not specified an empty string is used.
     *                      content        - Optional: the dialog content (HTML or jQuery objects). If not specified
     *                                       the dialog will be empty. It still can have a title and/or a button.
     *                      width          - Optional: the dialog width (CSS value). If not specified "auto" is used.
     *                      button_label   - Optional: the button label. If not specified no button appears.
     *                                       Note: the button label and button handler must be set together.
     *                      button_handler - Optional: the button handler function. If not specified no button appears.
     *                                       Note: the button label and button handler must be set together.
     */
    this.dialog = function(config) {

        return new Dialog()

        function Dialog() {
            var id = config.id
            var title = config.title
            var content = config.content
            var width = config.width || "auto"
            var button_label = config.button_label
            var button_handler = config.button_handler
            //
            var dialog = $("<div>", {id: id}).append(content)
            //
            var buttons = {}
            if (button_label && button_handler) {
                buttons[button_label] = button_handler
            }
            //
            var options = {
                modal: true, autoOpen: false, draggable: false, resizable: false, width: width,
                title: title, buttons: buttons
            }
            // Note: a dialog without a close button could be created by setting 2 more options
            // dialog.dialog("option", "dialogClass",    no_close_button ? "no-close-button" : "")
            // dialog.dialog("option", "closeOnEscape", !no_close_button)
            //
            // Firefox workaround, see http://bugs.jqueryui.com/ticket/3623 ### TODO: still needed?
            options.open = function() {
                $("body").css("overflow", "hidden")
            }
            //
            $("body").append(dialog)
            dialog.dialog(options)

            this.open = function() {
                dialog.dialog("open")
            }

            /**
             * @paran   duration    Optional: determines how long the fade out animation will run (in milliseconds).
             *                      If not specified (or 0) no animation is run (the dialog disappears immediately).
             * @paran   callback    Optional: function to be called once the animation is complete.
             */
            this.close = function(duration, callback) {
                duration = duration || 0
                dialog.parent().fadeOut(duration, function() {
                    dialog.dialog("close")
                    callback && callback()
                })
            }

            this.empty = function() {
                dialog.empty()
            }

            this.append = function(element) {
                dialog.append(element)
            }

            this.destroy = function() {
                // Note: "destroy" leaves the sole dialog content element in the DOM and returns it.
                // We remove it afterwards. We want the dialog to be completely removed from the DOM.
                dialog.dialog("destroy").remove()
            }
        }
    }



    // === Prompt ===

    this.prompt = function(title, input_label, button_label, callback) {
        var input = dm4c.render.input(undefined, 30).keyup(function(event) {
            if (event.which == 13) {
                do_submit()
            }
        })
        var dialog = this.dialog({
            title: title,
            content: $("<div>").addClass("field-label").text(input_label).add(input),
            button_label: button_label,
            button_handler: do_submit
        })
        dialog.open()

        function do_submit() {
            dialog.destroy()
            callback(input.val())   // Note: obviously the value can still be the read after destroying the dialog
        }
    }



    // === Menu ===

    // constant
    var SCROLLBAR_WIDTH = 15    // ### FIXME: suitable for all OS's?

    // global state
    var opened_menu = null      // the menu currently open (a BaseMenu), or null if no menu is open

    $(function() {
        // Close the open menu when clicked elsewhere.
        // Note: a neater approach would be to let the menu close itself by let its button react on blur.
        // This would work in Firefox but unfortunately Safari doesn't fire blur events for buttons.
        $("body").click(function() {
            close_opened_menu()
        })
    })

    function close_opened_menu() {
        if (opened_menu) {
            opened_menu.close()
        }
    }

    /**
     * Internal class acting as the base for Menu and ContextMenu.
     *
     * @param   config  an object with these properties:
     *              on_close    Internal close handler (a function)
     *              on_select   Optional: internal select handler (a function). Receives the selected menu item.
     *              handler     Optional: user application select handler (a function). Receives the selected menu item
     *                          and the coordinates of the selecting mouse click.
     *              parent      Optional: the mouse coordinates passed to the user application select handler
     *                          are relative to this element (a jQuery object).
     *                          If not specified the coordinates are relative to the client window.
     */
    function BaseMenu(config) {

        var self = this

        // Model
        var items = []

        // GUI
        var menu = $("<ul>")

        // -------------------------------------------------------------------------------------------------- Public API

        this.dom = menu.menu()

        /**
         * @param   item    object with "label", "value" (optional), "icon" (optional), "is_trigger" (optional),
         *                  and "handler" (optional) properties.
         */
        this.add_item = function(item) {
            // 1) update GUI
            var anchor = $("<a>").attr("href", "#").click(create_selection_handler(item))
            if (item.icon) {
                anchor.append($("<img>").attr("src", item.icon).addClass("menu-icon"))
            }
            anchor.append(item.label)
            var item_dom = $("<li>").append(anchor)
            menu.append(item_dom)
            menu.menu("refresh")    // ### TODO: is refresh a cheap operation? It is called for every item.
            // 2) update model
            items.push(item)
        }

        this.add_separator = function() {
            // update GUI
            menu.append("<hr>")
        }

        this.empty = function() {
            // update GUI
            menu.empty()
            // update model
            items = []
        }

        // ---

        this.open = function(x, y) {
            menu.css({top: y, left: x}).show()      // must be visible for measurement
            trim_height()
            opened_menu = this                      // update global state

            function trim_height() {
                // measure
                menu.width("auto").height("auto")   // reset trim for proper measurement
                var menu_height = menu.outerHeight()
                var window_height = window.innerHeight
                // trim
                if (y + menu_height > window_height) {
                    var height_trim = y + menu_height - window_height
                    menu.height(menu.height() - height_trim)
                    menu.width(menu.width() + SCROLLBAR_WIDTH)
                }
            }
        }

        /**
         * Closes this menu. Updates global state.
         * <p>
         * Not meant to be called by the application developer.
         * It is a public method anyway to let an outside click perform the closing.
         */
        this.close = function() {
            config.on_close()
            opened_menu = null      // update global state
        }

        this.is_open = function() {
            return menu.css("display") != "none"
        }

        // ---

        this.get_item_count = function() {
            return items.length
        }

        /**
         * Finds a menu item by value.
         * If there is no such menu item undefined is returned.
         */
        this.find_item = function(value) {
            for (var i = 0, item; item = items[i]; i++) {
                if (item.value == value) {
                    return item
                }
            }
        }

        /**
         * Finds a menu item by label.
         * If there is no such menu item undefined is returned.
         */
        this.find_item_by_label = function(label) {
            for (var i = 0, item; item = items[i]; i++) {
                if (item.label == label) {
                    return item
                }
            }
        }

        // ---

        function create_selection_handler(item) {
            return function(event) {
                // 1) fire event
                config.on_select && config.on_select(item)
                // 2) close menu
                self.close()
                // 3) call handler
                var h = item.handler || config.handler     // individual item handler has precedence
                if (h) {
                    var p = pos(event)      // pass coordinates of selecting mouse click to handler
                    h(item, p.x, p.y)
                }
                //
                return false
            }

            function pos(event) {
                var pos = {
                    x: event.clientX,
                    y: event.clientY
                }
                if (config.parent) {
                    var pp = config.parent.offset()
                    pos.x -= pp.left
                    pos.y -= pp.top
                }
                return pos
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------ Public API

    /**
     * Creates and returns a menu.
     *
     * The menu's DOM structure is as follows:
     *      <span>              - the top-level container
     *          <button>        - the menu-triggering button
     *              <span>      - the button's icon (a triangle)
     *              <span>      - the button's label
     *          <div>           - the actual menu (hidden until triggered)
     *              <a>         - a menu item
     *
     * The menu's DOM structure is accessible through the menu's "dom" property (a jQuery object).
     *
     * @param   handler     Optional: The callback function. One argument is passed to it:
     *                      the selected menu item (an object with "label", "value", ... properties).
     *                      If not specified your application can not react on the menu selection, which is
     *                      reasonable in case of stateful select-like menus.
     * @param   menu_title  Optional: The menu title (string).
     *                      If specified (even if empty string) a stateless action-trigger menu with a static menu title
     *                      is created. If not specified (undefined or null) a stateful select-like menu is created
     *                      with the selected item as "menu title".
     *
     * @return              The created menu (a Menu object). The caller can add the menu to the page by accessing the
     *                      menu's "dom" property (a jQuery object).
     */
    this.menu = function(handler, menu_title) {

        return new Menu()

        function Menu() {

            var self = this

            // Model
            // Note: the surrounding "handler" and "menu_title" are also part of the menu's model.
            var stateful = menu_title == undefined
            var selection   // selected menu item (object with "label", "value", ... properties).
                            // Used only for stateful select-like menus.

            // GUI
            var button = gui.button(do_open_menu, menu_title, "triangle-1-s")
            var base_menu = new BaseMenu({
                handler: handler,
                on_select: select_item,
                on_close: on_close
            })
            //
            var dom = $("<span>").append(button).append(base_menu.dom)

            // ---------------------------------------------------------------------------------------------- Public API

            this.dom = dom

            /**
             * @param   item    The menu item to add. An object with these properties:
             *                      "label" - The label to be displayed in the menu.
             *                      "value" - Optional: the value to be examined by the caller.
             *                          Virtually the value can be any kind of object, but consider the note.
             *                          Note: if this item is about to be selected programatically (by calling select())
             *                          the value must be specified and it must be a simple value.
             *                          ### TODO: selection of items which have an object as value could be supported
             *                          e.g. by let the caller supply a indicator function. Currently the item to select
             *                          is identified simply by equality (==) check on the values (see find_item()).
             *                          ### Think about: is the caller allowed to change the value afterwards?
             *                      "icon" - Optional: the icon to decorate the item (relative or absolute URL).
             *                      "is_trigger" (boolean) - Optional: if true this item acts as stateless
             *                          action-trigger within an stateful select-like menu. Default is false.
             *                          Reasonable only for stateful select-like menus.
             *                          ### TODO: this property could possibly be dropped. Meanwhile we have optional
             *                          per-item event handlers (see "handler" property).
             *                      "handler" - Optional: the individual handler. One argument is passed to it:
             *                          the selected menu item (an object with "label", "value", ... properties).
             */
            this.add_item = function(item) {
                base_menu.add_item(item)
                //
                // select the item if there is no selection yet
                if (!selection) {
                    // Note: this sets also the button label (in case of stateful select-like menus)
                    select_item(item)
                }
            }

            this.add_separator = function() {
                base_menu.add_separator()
            }

            this.empty = function() {
                base_menu.empty()
                //
                reset_selection()
            }

            // ---

            /**
             * Sets the selected menu item by value.
             * Note: no handler is triggered.
             * <p>
             * Only applicable for stateful select-like menus.
             * (For stateless action-trigger menus nothing is performed.)
             *
             * @param   item_value      Value of the menu item to select.
             *                          If there is not such menu item nothing is performed.
             */
            this.select = function(item_value) {
                select_item(base_menu.find_item(item_value))
            }

            // ---

            /**
             * Returns the selected menu item (object with "label", "value", ... properties).
             * If the menu has no items, undefined/null is returned.
             * <p>
             * Only applicable for stateful select-like menus.
             * (Stateless action-trigger menus always return undefined.)
             */
            this.get_selection = function() {
                return selection
            }

            this.get_item_count = function() {
                return base_menu.get_item_count()
            }

            /**
             * Finds a menu item by label.
             * If there is no such menu item undefined is returned.
             */
            this.find_item_by_label = function(label) {
                return base_menu.find_item_by_label(label)
            }

            // --------------------------------------------------------------------------------------- Private Functions

            /**
             * Called when the menu-triggering button is clicked.
             */
            function do_open_menu() {
                if (base_menu.is_open()) {
                    base_menu.close()
                } else {
                    close_opened_menu()
                    open_menu()
                }
                return false
            }

            /**
             * Calculates the position of the menu and opens it.
             */
            function open_menu() {
                // fire event ### TODO: move to BaseMenu
                if (config.pre_open_menu) {
                    config.pre_open_menu(self)
                }
                //
                var button_pos = button.offset()
                base_menu.open(button_pos.left, button_pos.top + button.outerHeight())
            }

            function on_close() {
                base_menu.dom.hide()
            }

            // ---

            /**
             * Only applicable for stateful select-like menus.
             * (For stateless action-trigger menus nothing is performed.)
             *
             * @param   item    object with "label", "value", ... properties. If undefined nothing is performed.
             */
            function select_item(item) {
                // Note: only stateful select-like menus have selection state.
                if (stateful && item && !item.is_trigger) {
                    // update model
                    selection = item
                    // update GUI
                    set_button_label(item.label)
                }
            }

            function reset_selection() {
                selection = null
            }

            // ---

            function set_button_label(label) {
                // Note: we must set the "text" option to true.
                // It is false if the button had no label while creation.
                button.button("option", {label: label, text: true})
            }
        }
    }

    /**
     * @param   parent  The element the context menu is appended to (a jQuery object).
     *                  The mouse coordinates passed to the menu item handlers are relative to this element.
     */
    this.context_menu = function(parent) {

        return new ContextMenu()

        function ContextMenu() {

            var base_menu = new BaseMenu({
                on_close: on_close,
                parent: parent
            })

            this.add_item = function(item) {
                base_menu.add_item(item)
            }

            this.add_separator = function() {
                base_menu.add_separator()
            }

            this.open = function(x, y) {
                parent.append(base_menu.dom)
                base_menu.open(x, y)
            }

            // ---

            function on_close() {
                base_menu.dom.remove()
            }
        }
    }



    // === Combobox ===

    this.combobox = function() {

        return new Combobox()

        function Combobox() {
            var menu = gui.menu(do_select_item, "")
            var input = $("<input>").attr("type", "text").addClass("combobox")
            menu.dom.append(input)
            this.dom = menu.dom

            this.add_item = function(item) {
                menu.add_item(item)
            }

            this.select_by_label = function(item_label) {
                set_input_text(item_label)
            }

            /**
             * Returns either the selected menu item (object with "label", "value", ... properties)
             * or the text entered in the input field (string).
             * <p>
             * There are 2 cases:
             * 1) the user has choosen an item from the combobox's menu.
             * 2) the user entered a text which do *not* appear in the menu.
             * (If the user entered a text which *do* appear in the menu case 1 applies.)
             * <p>
             * To examine which case occured the caller uses "typeof" on the returned value.
             */
            this.get_selection = function() {
                var text = $.trim(input.val())
                var item = menu.find_item_by_label(text)
                return item || text
            }

            function do_select_item(item) {
                set_input_text(item.label)
            }

            function set_input_text(text) {
                input.val(text)
            }
        }
    }
}
