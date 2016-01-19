/**
 * A generic (DeepaMehta independent) GUI toolkit to create buttons, menus, context menus, combo boxes,
 * dialog boxes, and prompts. Based on jQuery UI.
 *
 * The DeepaMehta Webclient's GUIToolkit instance is accessible as dm4c.ui
 *
 * @param   config  an object with these properties:
 *                      on_open_menu        - Optional: the open menu handler (a function). Invoked before a menu is
 *                                            opened. Receives the menu (a BaseMenu) to be opened.
 */
function GUIToolkit(config) {

    var self = this



    // === Button ===

    /**
     * Creates and returns a jQuery UI button.
     *
     * @param   config  an object with these properties:
     *              on_click
     *                  Optional: the handler invoked on click (function).
     *              on_mousedown
     *                  Optional: the handler invoked on mousedown (function).
     *              on_mouseup
     *                  Optional: the handler invoked on mouseup (function).
     *              label
     *                  Optional: the button label (string).
     *                  If not specified the button has no label. It still can have an icon.
     *              icon
     *                  Optional: the button icon (string). The value must match a jQuery UI icon class name
     *                  without the "ui-icon-" prefix. See http://api.jqueryui.com/theming/icons/.
     *                  If not specified the button has no icon. It still can have a label.
     *              is_submit
     *                  Optional: if true a submit button is created (boolean). Default is false.
     *
     * @return  The button (a jQuery object).
     */
    this.button = function(config) {
        var button = $('<button type="' + (config.is_submit ? "submit" : "button") + '">')
            .click(config.on_click)
            .mousedown(config.on_mousedown)
            .mouseup(config.on_mouseup)
        // build options
        var options = {}
        if (config.label) {
            options.label = config.label
        } else {
            options.text = false
        }
        if (config.icon) {
            options.icons = {primary: "ui-icon-" + config.icon}
        }
        // create button
        return button.button(options)
    }



    // === Checkbox ===

    this.checkbox = function(checked) {
        var checkbox = $("<input type='checkbox'>")
        if (checked) {
            checkbox.attr("checked", "checked")
        }
        return {
            dom: checkbox,

            get checked() {
                return checkbox.get(0).checked
            }
        }
    }



    // === Dialog Box ===

    /**
     * @param   config  an object with these properties:
     *                      id              - Optional: the dialog content div will get this ID. Useful to style the
     *                                        dialog with CSS. If not specified no "id" attribute is set.
     *                      title           - Optional: the dialog title. If not specified an empty string is used.
     *                      content         - Optional: the dialog content (HTML or jQuery objects). If not specified
     *                                        the dialog will be empty. It still can have a title and/or a button.
     *                      width           - Optional: the dialog width (CSS value). If not specified "auto" is used.
     *                      button_label    - Optional: the button label.
     *                                        If not specified no button appears.
     *                                        Note: the button label and button handler must be set together.
     *                      button_handler  - Optional: the button handler function. Nothing is passed to it.
     *                                        If not specified no button appears.
     *                                        Note: the button label and button handler must be set together.
     *                      auto_close      - Optional: controls if the button closes the dialog (boolean). Default is
     *                                        true. If false is specified the caller is responsible for closing the
     *                                        dialog.
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
            var auto_close = config.auto_close || config.auto_close == undefined    // default is true
            //
            var dialog = $("<div>", {id: id}).append(content)
            //
            var buttons = {}
            if (button_label && button_handler) {
                buttons[button_label] = invoke_handler
                dm4c.on_return_key(dialog, invoke_handler)
            }
            //
            var options = {
                title: title, buttons: buttons, width: width,
                modal: true, draggable: false, resizable: false,
                close: destroy
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

            // --- Public API ---

            /**
             * @paran   duration    Optional: determines how long the fade out animation will run (in milliseconds).
             *                      If not specified (or 0) no animation is run (the dialog disappears immediately).
             */
            this.close = function(duration) {
                dialog.parent().fadeOut(duration || 0, close)
            }

            // ---

            function invoke_handler() {
                auto_close && close()
                button_handler()
            }

            function close() {
                dialog.dialog("close")
            }

            function destroy() {
                // Note: "destroy" leaves the sole dialog content element in the DOM and returns it.
                // We remove it afterwards. We want the dialog to be completely removed from the DOM.
                dialog.dialog("destroy").remove()
            }
        }
    }



    // === Prompt ===

    this.prompt = function(title, input_label, button_label, callback) {
        var input = dm4c.render.input(undefined, 30)    // ### TODO: remove dm4c dependency
        var dialog = this.dialog({
            title: title,
            content: $("<div>").addClass("field-label").text(input_label).add(input),
            button_label: button_label,
            button_handler: function() {
                callback(input.val())   // Note: obviously the value can still be the read after destroying the dialog
            }
        })
    }



    // === Menu ===

    // constant
    var SCROLLBAR_WIDTH = 15    // ### FIXME: suitable for all OS's?

    // global state
    var opened_menu = null      // the menu currently open (a BaseMenu), or null if no menu is open
    var tx, ty                  // tracks movement

    $(function() {
        // close open menu when clicked somewhere
        $("body")
            .mousedown(function(event) {
                close_opened_menu()
                tx = event.clientX
                ty = event.clientY
            })
            .mouseup(function(event) {
                // context menus stay open if mouse not moved between mouseup and mousedown
                if (event.clientX != tx || event.clientY != ty) {
                    close_opened_menu()
                }
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
     * @param   _config  an object with these properties:
     *              on_close    Internal close handler (a function)
     *              on_select   Optional: internal select handler (a function). Receives the selected menu item.
     *              handler     Optional: user application item handler (a function). Receives the selected menu item
     *                          and the coordinates of the selecting mouse click.
     *              parent      Optional: the mouse coordinates passed to the user application item handler
     *                          are relative to this element (a jQuery object).
     *                          If not specified the coordinates are relative to the client window.
     */
    function BaseMenu(_config) {

        var self = this

        // Model
        var items = []

        // GUI
        var menu = $("<ul>").menu({
            select: function(event, ui) {
                // Note: we invoke the item handler by triggering a "mouseup" event on the selected menu item.
                // We must only invoke the handler in case of keyboard selection because the "menuselect" event
                // is fired for mouse based selection as well, in which case the item handler is already invoked.
                // (BTW: regarding mouse based selection the "menuselect" event is fired only for stationary menus,
                // not for context menus. I really don't know why not for context menus.)
                if (event.keyCode) {
                    ui.item.mouseup()
                }
            }
        })
        .off("focus")               // unbinding "focus" prevents auto focusing the first item when the menu gets focus
        .mousedown(consume_event)   // avoid mousedown on scrollbar parts to close the menu prematurely
        .mouseup(consume_event)     // avoid mouseup   on scrollbar parts to close the menu prematurely

        // We use a container element for absolute menu positioning, which we need.
        // The menu itself (the <ul>) can't be positioned absolute in combination with overflow "auto" or "scroll".
        // In this case submenus would not layout properly: they would be contrained to the main menu's box and a
        // horizontal scrollbar would appear. The submenu would only be visible after scrolling to the right.
        var menu_container = $("<div>").addClass("menu-container")

        // ---

        this.dom = menu_container.append(menu)

        /**
         * @param   item    The menu item to add. An object with these properties:
         *                      "label"
         *                      "value" (optional)
         *                      "icon" (optional)
         *                      "disabled" (optional)
         *                      "is_trigger" (optional)
         *                      "handler" (optional)
         *                      "submenu_items" (optional)
         *                  See Menu.add_item() below for a detailed property description.
         */
        this.add_item = function(item) {
            // 1) update GUI
            var item_dom = menu_item_dom(item)
            if (item.submenu_items) {
                item_dom.append(submenu_dom(item.submenu_items))
            }
            menu.append(item_dom).menu("refresh")
            // 2) update model
            items.push(item)
        }

        this.add_separator = function() {
            // update GUI
            menu.append($("<li>").text("-"))
        }

        this.empty = function() {
            // update GUI
            menu.empty()
            // update model
            items = []
        }

        // ---

        this.open = function(x, y) {
            // invoke callback
            if (config.on_open_menu) {
                config.on_open_menu(this)
            }
            //
            menu_container.css({top: y, left: x}).show()    // must be visible for measurement
            menu.focus()                                    // focus enables keyboard control
            trim_height()
            opened_menu = this                              // update global state

            function trim_height() {
                // measure
                menu.width("auto").height("auto")           // reset trim for proper measurement
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
            _config.on_close()
            opened_menu = null      // update global state
        }

        this.is_open = function() {
            return menu_container.css("display") != "none"
        }

        this.hide = function() {
            menu_container.hide()
        }

        this.destroy = function() {
            menu.menu("destroy")
            menu_container.remove()
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
            // Note: if there is an item with label 0 (a number) combobox.get_selection() would mistakenly return that
            // item when nothing is entered in the input field (string). This is because a find_item_by_label() search
            // is performed and 0 == "" is true. So, we don't let empty input match any item.
            if (label == "") {
                return
            }
            //
            for (var i = 0, item; item = items[i]; i++) {
                if (item.label == label) {
                    return item
                }
            }
        }

        // ---

        function submenu_dom(items) {
            var submenu = $("<ul>")
            for (var i = 0, item; item = items[i]; i++) {
                submenu.append(menu_item_dom(item))
            }
            return submenu
        }

        function menu_item_dom(item) {
            var menu_item = $("<li>")
                .toggleClass("ui-state-disabled", item.disabled == true)
                .mouseup(item_handler(item))
                .mousedown(consume_event)   // a bubbled up mousedown event would close the menu prematurely
                .click(consume_event)       // prevent default, that is don't invoke href "#"
                .append(item_anchor_dom(item))
            return menu_item
        }

        function item_anchor_dom(item) {
            var anchor = $("<a>").attr("href", "#").append(item.label)
            if (item.icon) {
                anchor.prepend($("<img>").attr("src", item.icon).addClass("icon"))
            }
            return anchor
        }

        function item_handler(item) {
            return function(event) {
                //
                self.close()
                //
                if (!item.disabled) {
                    // invoke internal callback
                    _config.on_select && _config.on_select(item)
                    // invoke application handler
                    var h = item.handler || _config.handler // individual item handler overrides global menu handler
                    if (h) {
                        var p = pos(event)                  // pass coordinates of selecting mouse click to handler
                        h(item, p.x, p.y)
                    }
                }
            }

            function pos(event) {
                var pos = {
                    x: event.clientX,
                    y: event.clientY
                }
                if (_config.parent) {
                    var pp = _config.parent.offset()
                    pos.x -= pp.left
                    pos.y -= pp.top
                }
                return pos
            }
        }
    }

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
     *                      reasonable in case of stateful menus.
     * @param   menu_title  Optional: The menu title (string).
     *                      If specified (even if empty string) a stateless action-trigger menu with a static menu title
     *                      is created. If not specified (undefined or null) a stateful menu is created with the
     *                      selected item as "menu title".
     *
     * @return              The created menu (a Menu object). The caller can add the menu to the page by accessing the
     *                      menu's "dom" property (a jQuery object).
     */
    this.menu = function(handler, menu_title) {

        return new Menu()

        function Menu() {

            // Model
            // Note: the surrounding "handler" and "menu_title" are also part of the menu's model.
            var stateful = menu_title == undefined
            var selection   // selected menu item (object with "label", "value", ... properties).
                            // Used only for stateful menus.

            // GUI
            var button = self.button({
                on_click:     on_button_click,
                on_mousedown: on_button_mousedown,
                on_mouseup:   consume_event,
                label: menu_title,
                icon: "triangle-1-s"
            })
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
             *                      "disabled" (boolean) - Optional: if true the item appears as disabled.
             *                      "is_trigger" (boolean) - Optional: if true the item is not regarded as a state
             *                          in a stateful menu. Default is false. Meaningless in stateless menus.
             *                          ### TODO: consider renaming to "not_a_state".
             *                      "handler" - Optional: the individual handler. One argument is passed to it:
             *                          the selected menu item (an object with "label", "value", ... properties).
             *                      "submenu_items" - Optional: an array of items to be displayed as a submenu.
             */
            this.add_item = function(item) {
                base_menu.add_item(item)
                //
                // select the item if there is no selection yet
                if (!selection) {
                    // Note: this sets also the button label (in case of stateful menus)
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
             * If there is not such menu item an exception is thrown.
             * <p>
             * Note: no handler is triggered.
             * <p>
             * Only applicable for stateful menus.
             * (For stateless action-trigger menus nothing is performed.)
             *
             * @param   item_value      Value of the menu item to select.
             */
            this.select = function(item_value) {
                var item = base_menu.find_item(item_value)
                if (!item) {
                    throw "MenuError: item with value \"" + item_value + "\" not found in menu (select() method)"
                }
                select_item(item)
            }

            // ---

            /**
             * Returns the selected menu item (object with "label", "value", ... properties).
             * If the menu has no items, undefined/null is returned.
             * <p>
             * Only applicable for stateful menus.
             * (Stateless action-trigger menus always return undefined.)
             */
            this.get_selection = function() {
                return selection
            }

            this.get_item_count = function() {
                return base_menu.get_item_count()
            }

            /**
             * Finds a menu item by value.
             * If there is no such menu item undefined is returned.
             */
            this.find_item = function(value) {
                return base_menu.find_item(value)
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
             * Called when the button fires mousedown.
             */
            function do_open_menu() {
                if (!base_menu.is_open()) {
                    close_opened_menu()
                    open_menu()
                    return false    // a bubbled up mousedown event would close the opened menu immediately
                }
                // Note: if this button's menu is open already the mousedown event bubbles up and closes it
            }

            /**
             * Calculates the position of the menu and opens it.
             */
            function open_menu() {
                var button_pos = button.offset()
                base_menu.open(button_pos.left, button_pos.top + button.outerHeight())
            }

            function on_close() {
                base_menu.hide()
            }

            // ---

            var track_mousedown

            function on_button_mousedown() {
                track_mousedown = true
                return do_open_menu()
            }

            /**
             * Enables menu opening via keyboard.
             */
            function on_button_click() {
                // Note: a focused button fires a "click" event when Enter/Space is pressed. There are no prior
                // "mousedown" and "mouseup" events in this case.
                // Note: when using the mouse instead we must not invoke the menu twice. We do so by tracking
                // weather the "click" event is preceded by a "mousedown" event.
                if (!track_mousedown) {
                    return do_open_menu()
                } else {
                    track_mousedown = false
                }
            }

            // ---

            /**
             * Only applicable for stateful menus.
             * (For stateless action-trigger menus nothing is performed.)
             *
             * @param   item    object with "label", "value", ... properties.
             */
            function select_item(item) {
                // Note: only stateful menus have selection state.
                if (stateful && !item.is_trigger) {
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
     * @param   parent  The element the context menu is appended to (a jQuery object). ### FIXDOC
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
                // Note: we append not to parent but to body. This works around a Safari problem where the
                // topicmap panel is accidentally translated in case the context menu is height trimmed.
                $("body").append(base_menu.dom)
                base_menu.open(x, y)
            }

            // ---

            function on_close() {
                base_menu.destroy()
            }
        }
    }



    // === Combobox ===

    this.combobox = function() {

        return new Combobox()

        function Combobox() {
            var menu = self.menu(do_select_item, "")
            var input = $("<input>").attr("type", "text").addClass("combobox")
            menu.dom.append(input)
            this.dom = menu.dom

            this.add_item = function(item) {
                menu.add_item(item)
            }

            /**
             * @param   item_value
             *              The value of the menu item to select.
             *              If no menu item with this value exists an exception is thrown.
             *              Unless the value is null or undefined; in that case the input field is cleared.
             */
            this.select = function(item_value) {
                // Note: menu.select() is not called here as the combobox menu is stateless (it has no selection)
                if (item_value != null) {
                    var item = menu.find_item(item_value)
                    if (!item) {
                        throw "ComboboxError: item with value \"" + item_value +
                            "\" not found in menu (select() method)"
                    }
                    var text = item.label
                } else {
                    var text = ""
                }
                set_input_text(text)
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
                var text = this.val()
                var item = menu.find_item_by_label(text)
                return item || text
            }

            this.val = function() {
                return $.trim(input.val())
            }

            function do_select_item(item) {
                set_input_text(item.label)
            }

            function set_input_text(text) {
                input.val(text)
            }
        }
    }

    // ---

    function consume_event() {
        return false
    }
}
