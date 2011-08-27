function GUIToolkit() {

    var gui = this


    // === Button ===

    /**
     * Creates and returns a jQuery UI button.
     *
     * @param   handler     The callback function. The generic JavaScript event arguments are passed to it.
     *
     * @return              The button (a jQuery object).
     */
    this.button = function(handler, label, icon, is_submit) {
        //
        if (!handler) {
            alert("WARNING (GUIToolkit.button): No handler specified for button");
        }
        //
        button = $('<button type="' + (is_submit ? "submit" : "button") + '">').click(handler)
        //
        var options = {}
        if (label) {
            options.label = label
        } else {
            options.text = false
        }
        if (icon) {
            options.icons = {primary: "ui-icon-" + icon}
        }
        //
        return button.button(options)
    }



    // === Dialog Box ===

    // Settings
    var DEFAULT_DIALOG_WIDTH = 350   // in pixel

    this.dialog = function(id, title, content, width, button_label, button_handler) {
        var dialog = $("<div>", {id: id}).append(content)
        width = width || DEFAULT_DIALOG_WIDTH
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
        // Firefox workaround, see http://bugs.jqueryui.com/ticket/3623
        options.open = function() {$("body").css("overflow", "hidden")}
        //
        $("body").append(dialog)
        dialog.dialog(options)
    }



    // === Menu ===

    // Settings
    var SCROLL_DISTANCE = 8     // distance of one scroll step in pixel
    var SCROLL_DELAY = 15       // delay between scroll steps in milliseconds

    var opened_menu = null      // global state: the menu currently open, or null if no menu is open

    $(function() {
        // Close the open menu when clicked elsewhere.
        // Note: a neater approach would be to let the menu close itself by let its button react on blur.
        // This would work in Firefox but unfortunately Safari doesn't fire blur events for buttons.
        $("body").click(function() {
            // ### if (dm4c.LOG_GUI) dm4c.log("Body clicked -- close opened menu")
            close_opened_menu()
        })
    })

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
     * The menu's DOM structure is accessible through the menu's "dom" attribute (a jQuery object).
     *
     * @param   handler     Optional: The callback function. One argument is passed to it:
     *                      the selected menu item (an object with "value" and "label" properties).
     *                      If not specified your application can not react on the menu selection, which is
     *                      reasonable in case of stateful select-like menus.
     * @param   menu_title  Optional: The menu title (string).
     *                      If specified a stateless action-trigger menu with a static menu title is created.
     *                      If not specified a stateful select-like menu is created with the selected item as
     *                      "menu title".
     *
     * @return              The created menu (a Menu object). The caller can add the menu to the page by accessing the
     *                      menu's "dom" attribute (a jQuery object).
     */
    this.menu = function(handler, menu_title) {

        return new Menu()

        function Menu() {

            var self = this

            // Model
            // Note: the surrounding "handler" and "menu_title" are also part of the menu's model.
            var items = []
            var stateful = !menu_title
            var selection   // selected item (object with "value" and "label" properties).
                            // Used only for stateful select-like menus.

            // short-term interaction state
            var menu_height
            var scroller_height
            var window_height
            var scroll_animation

            // GUI
            var button = gui.button(do_open_menu, menu_title, "triangle-1-s")
            var menu            = $("<div>").addClass("menu")
            var top_scroller    = $("<div>").addClass("scroll-area top")
            var bottom_scroller = $("<div>").addClass("scroll-area bottom")
            // Note: clicking a scroller is meant to have no effect. However, the event must be consumed by the
            // scroller in order to avoid it bubbling up to the body (where it would cause the menu to be closed).
            top_scroller   .hover(create_scroll_handler(+SCROLL_DISTANCE), do_end_scroll).click(consume)
            bottom_scroller.hover(create_scroll_handler(-SCROLL_DISTANCE), do_end_scroll).click(consume)
            //
            var dom = $("<span>").append(button).append(menu).append(top_scroller).append(bottom_scroller)

            // ---------------------------------------------------------------------------------------------- Public API

            this.dom = dom

            /**
             * @param   item    The menu item to add. An object with these properties:
             *                      "label" - The label to be displayed in the menu.
             *                      "value" - Optional: the value to be examined by the caller.
             *                          Note: if this item is about to be selected programatically or re-labeled
             *                          the value must be specified.
             *                      "icon" - Optional: the icon to decorate the item (relative or absolute URL).
             *                      "is_trigger" (boolean) - Optional: if true this item acts as stateless
             *                          action-trigger within an stateful select-like menu. Default is false.
             *                          Reasonable only for stateful select-like menus.
             *                      "handler" - Optional: the individual handler.
             */
            this.add_item = function(item) {
                add_item(item)
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
                remove_selection()
            }

            // ---

            /**
             * Selects a menu item by its value.
             * <p>
             * Only applicable for stateful select-like menus.
             * (For stateless action-trigger menus nothing is performed.)
             *
             * @param   item_value      Value of the menu item to select.
             *                          If there is not such menu item nothing is performed.
             */
            this.select = function(item_value) {
                select_item(find_item(item_value))
            }

            /**
             * Selects a menu item by its label.
             * <p>
             * Only applicable for stateful select-like menus.
             * (For stateless action-trigger menus nothing is performed.)
             *
             * @param   item_label      Label of the menu item to select.
             *                          If there is not such menu item nothing is performed.
             */
            this.select_by_label = function(item_label) {
                select_item(find_item_by_label(item_label))
            }

            // ---

            /**
             * Returns the selected menu item (object with "value" and "label" properties).
             * If the menu has no items, undefined/null is returned.
             * <p>
             * Only applicable for stateful select-like menus.
             * (Stateless action-trigger menus always return undefined.)
             */
            this.get_selection = function() {
                return selection
            }

            this.get_item_count = function() {
                return items.length
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

            /**
             * Closes this menu.
             * <p>
             * Not meant to be called by the application developer.
             * It is a public method anyway to let an outside click perform the closing.
             */
            this.close = function() {
                close_menu()
            }

            // --------------------------------------------------------------------------------------- Private Functions



            // === Event Handler ===

            function do_open_menu(event) {
                if (!is_visible(menu)) {
                    var mouse_y = event.layerY + $(this).position().top
                    close_opened_menu()
                    open_menu(mouse_y)
                } else {
                    close_menu()
                }
                return false
            }

            function create_selection_handler(item) {
                return function() {
                    // 1) remember selection
                    select_item(item)
                    // 2) close menu
                    close_menu()
                    // 3) call handler
                    var h = item.handler || handler     // individual item handler has precedence
                    if (h) {
                         h(item)
                    }
                    return false
                }
            }

            // ---

            function create_scroll_handler(distance) {
                return function() {
                    scroll_animation = setInterval(function() {
                        move_to(menu.position().top + distance)
                    }, SCROLL_DELAY)
                }
            }

            function do_end_scroll() {
                clearInterval(scroll_animation)
            }

            function consume() {
                return false
            }

            // === Helper ===

            /**
             * @param   item    object with "value" (optional) and "label" properties.
             */
            function add_item(item) {
                // 1) update GUI
                var item_dom = $("<a>").attr("href", "#").click(create_selection_handler(item))
                add_hovering()
                if (item.icon) {
                    item_dom.append(dm4c.render.image(item.icon, undefined, "menu-icon"))
                }
                item_dom.append(item.label)
                menu.append(item_dom)
                // 2) update model
                item.dom = item_dom
                items.push(item)
                // 3) adjust selection
                // select the item if there is no selection yet
                if (!selection) {
                    // Note: this sets also the button label (in case of stateful select-like menus)
                    select_item(item)
                }

                function add_hovering() {
                    item_dom.hover(
                        function() {$(this).addClass("hover")},
                        function() {$(this).removeClass("hover")}
                    )
                }
            }

            /**
             * Only applicable for stateful select-like menus.
             * (For stateless action-trigger menus nothing is performed.)
             *
             * @param   item    object with "value" and "label" properties. If undefined nothing is performed.
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

            function remove_selection() {
                selection = null
            }

            /**
             * Calculates the position of the menu and opens it. Updates global state.
             */
            function open_menu(mouse_y) {
                var button_pos = button.position()
                var button_x = button_pos.left
                var button_y = button_pos.top
                //
                menu           .css({left: button_x}).show()
                top_scroller   .css({left: button_x, width: menu.width()})
                bottom_scroller.css({left: button_x, width: menu.width()})
                // the menu might be trimmed from previous opening
                reset_top_trim()
                reset_bottom_trim()
                //
                menu_height = menu.outerHeight()
                scroller_height = top_scroller.outerHeight()
                window_height = window.innerHeight
                //
                if (selection) {
                    var item_height = selection.dom.outerHeight()
                    var menu_y = mouse_y - selection.dom.position().top - item_height / 2
                    selection.dom.addClass("hover")
                } else {
                    var menu_y = button_y + button.outerHeight()
                }
                //
                move_to(menu_y)
                // update global state
                opened_menu = self
            }

            /**
             * Closes this menu. Updates global state.
             */
            function close_menu() {
                menu.hide()
                top_scroller.hide()
                bottom_scroller.hide()
                // update global state
                opened_menu = null
            }

            function move_to(menu_y) {

                if (!is_visible(menu)) {
                    alert("WARNING: scroll thread is still running while menu is not shown")
                }

                trim_top()
                trim_bottom()
                menu.css("top", menu_y)

                // Note: we trim the menu top by cliping
                function trim_top() {
                    if (menu_y < 0) {
                        var top = -menu_y + scroller_height
                        menu.css("clip", "rect(" + top + "px, auto, auto, auto)")
                        // show top scroller
                        if (!is_visible(top_scroller)) {
                            top_scroller.show()
                        }
                    } else {
                        // hide top scroller
                        if (is_visible(top_scroller)) {
                            top_scroller.hide()
                            reset_top_trim()
                            clearInterval(scroll_animation)
                        }
                    }
                }

                // Note: we trim the menu bottom by reducing the menu height
                function trim_bottom() {
                    if (menu_y + menu_height > window_height) {
                        // 8px = menu's top/bottom padding (2 * 0.3em = 9px) - 1px scroller overlap
                        var height = window_height - menu_y - 8 - scroller_height
                        menu.css("height", height + "px")
                        // show bottom scroller
                        if (!is_visible(bottom_scroller)) {
                            bottom_scroller.show()
                        }
                    } else {
                        // hide bottom scroller
                        if (is_visible(bottom_scroller)) {
                            bottom_scroller.hide()
                            reset_bottom_trim()
                            clearInterval(scroll_animation)
                        }
                    }
                }
            }

            function reset_top_trim() {
                menu.css("clip", "rect(auto, auto, auto, auto)")
            }

            function reset_bottom_trim() {
                menu.css("height", "auto")
            }

            // ---

            function set_button_label(label) {
                // Note: we must set the "text" option to true.
                // It is false if the button had no label while creation.
                button.button("option", {label: label, text: true})
            }

            function is_visible(dom) {
                return dom.css("display") != "none"
            }

            // ---

            /**
             * Finds a menu item by value.
             * If there is no such menu item undefined is returned.
             */
            function find_item(value) {
                for (var i = 0, item; item = items[i]; i++) {
                    if (item.value == value) {
                        return item
                    }
                }
            }
        }
    }



    // === Combobox ===

    this.combobox = function() {

        return new Combobox()

        function Combobox() {
            var menu = gui.menu(item_selected, "Choose")
            var input = $("<input>").attr("type", "text").addClass("combobox")
            menu.dom.append(input)
            this.dom = menu.dom

            this.add_item = function(item) {
                menu.add_item(item)
            }

            this.select_by_label = function(item_label) {
                setInputText(item_label)
            }

            /**
             * Returns either the selected menu item (object with "value" and "label" properties)
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
                var text = input.val()
                var item = menu.find_item_by_label(text)
                return item || text
            }

            function item_selected(item) {
                setInputText(item.label)
            }

            function setInputText(text) {
                input.val(text)
            }
        }
    }



    // ----------------------------------------------------------------------------------------------- Private Functions

    function close_opened_menu() {
        if (opened_menu) {
            opened_menu.close()
        }
    }
}
