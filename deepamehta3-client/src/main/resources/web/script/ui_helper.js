function UIHelper() {



    /**************/
    /*** Button ***/
    /**************/



    /**
     * Creates and returns a button.
     *
     * The button's DOM structure is as follows:
     *      <button id="button_id">     - the top-level container (gets the provided menu ID)
     *          <span>                  - the button's icon (provided it has an icon)
     *          button_label            - the button's label (a text node)
     *
     * @param   id          Optional: ID of the <button> element that is transformed to a jQuery UI button.
     *                      If no such DOM element exists in the document (or if "id" is undefined), a button element
     *                      is created and the caller is responsible for adding the returned button to the DOM tree.
     * @param   handler     The callback function.
     *
     * @return              The button (a jQuery object).
     */
    this.button = function(id, handler, label, icon, is_submit) {
        //
        if (!handler) {
            alert("WARNING (UIHelper.button): No handler specified for button \"" + id + "\"");
        }
        //
        if (id) {
            var button = $("#" + id)
            if (button.length == 0) {
                // Note: type="button" is required. Otherwise the button acts as submit button (if contained in a form).
                // Update: type="button" moved into element because attr("type", ...) is ignored in jQuery 1.4/Safari.
                button = $("<button type='button'>").attr("id", id)
            }
        } else {
            button = $("<button type='button'>")
        }
        // Note: pseudo-attribute "submit" TODO: explain
        button.attr({submit: is_submit}).click(handler)
        button.button()
        if (label) {
            button.button("option", "label", label)
        } else {
            button.button("option", "text", false)
        }
        if (icon) {
            button.button("option", "icons", {primary: "ui-icon-" + icon})
        }
        return button
    }



    /******************/
    /*** Dialog Box ***/
    /******************/



    // Settings
    var DIALOG_WIDTH = 350   // in pixel

    this.dialog = function(id, title, button_label, button_handler) {
        var dialog = $("<div>", {id: id})
        var buttons = {}; buttons[button_label] = button_handler
        $("body").append(dialog)
        dialog.dialog({
            modal: true, autoOpen: false, draggable: false, resizable: false, width: DIALOG_WIDTH,
            title: title, buttons: buttons
        })
    }



    /************/
    /*** Menu ***/
    /************/



    var menus = {}          // key: menu ID, value: a Menu object

    $(function() {
        // Close open menus when clicked elsewhere.
        // Note: a neater approach would be to let the menu close itself by let its button react on blur.
        // This would work in Firefox but unfortunately Safari doesn't fire blur events for buttons.
        $("body").click(function() {
            if (dm3c.LOG_GUI) dm3c.log("Body clicked -- hide all menus")
            hide_all_menus()
        })
    })

    // ------------------------------------------------------------------------------------------------------ Public API

    /**
     * Creates and returns a menu.
     *
     * The menu's DOM structure is as follows:
     *      <span id="menu_id">     - the top-level container (gets the provided menu ID)
     *          <button>            - the menu-triggering button
     *              <span>          - the button's icon (a triangle)
     *              <span>          - the button's label
     *          <div>               - the actual menu (hidden until triggered)
     *              <a>             - a menu item
     *
     * The menu's DOM structure is accessible through the menu's "dom" attribute (a jQuery object).
     * Note: the top-level container's id attribute allows easy DOM selection of the menu, e.g. to replace it with
     * another menu.
     *
     * @param   menu_id     The menu ID. Can be used later on to identify the menu, e.g. for adding items to it.
     *                      If a DOM element with such an ID exists it is replaced by the menu.
     *                      If no such DOM element exists, the caller is responsible for adding the menu to the
     *                      DOM tree.
     * @param   handler     Optional: The callback function. 2 arguments are passed to it:
     *                      1) The selected menu item (an object with "value" and "label" properties).
     *                      2) The menu ID.
     *                      If not specified your application can not react on the menu selection, which is
     *                      reasonable in case of stateful select-like menus.
     * @param   items       Optional: The menu items (an array of objects with "value" and "label" properties).
     *                      If not specified the DOM element specified by menu_id is expected to be a <select> element.
     *                      Its <option> elements are taken as menu items. If there are no <select> <option> elements,
     *                      the menu will be created with no items (items are expected to be added later on).
     * @param   menu_title  Optional: The menu title (string).
     *                      If specified a stateless action-trigger menu with a static menu title is created.
     *                      If not specified a stateful select-like menu is created with the selected item as
     *                      "menu title".
     *
     * @return              The created menu (a Menu object). The caller can add the menu to the page by accessing the
     *                      menu's "dom" attribute (a jQuery object).
     */
    this.menu = function(menu_id, handler, items, menu_title) {

        return menus[menu_id] = new Menu()

        function Menu() {

            // Model
            // Note: the surrounding "menu_id", "handler", "items", and "menu_title" are also part of the menu's model.
            var stateful = !menu_title
            var selection   // selected item (object with "value" and "label" properties). Used only for stateful
                            // select-like menus.

            // GUI
            var menu        // the actual menu (jQuery <div> object)
            var button      // the menu-triggering button (jQuery <button> object)
            var dom         // the top-level container (jQuery <span> object)

            // Note: the button must be build _before_ the menu is build
            // because adding menu items might affect the button label (in case of a stateful select-like menu).
            build_button()
            build_menu()
            // Note: the button must be added to the page _after_ the menu ís build because the menu might rely on the
            // placeholder element (in case the menu is build from a <select> element).
            add_to_page()

            // ---------------------------------------------------------------------------------------------- Public API

            /**
             * @param   item    The menu item to add. An object with this properties:
             *                      "label" - The label to be displayed in the menu.
             *                      "value" - Optional: the value to be examined by the caller.
             *                          Note: if this item is about to be selected programatically or re-labeled
             *                          the value must be specified.
             *                      "icon" - Optional: the icon to decorate the item (relative or absolute URL).
             *                      "is_trigger" (boolean) - Optional: if true this item acts as stateless
             *                          action-trigger within an stateful select-like menu. Default is false.
             *                          Reasonable only for stateful select-like menus.
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

            /**
             * @param   item_value      Value of the menu item to select.
             *                          If there is not such menu item nothing is performed.
             */
            this.select = function(item_value) {
                select_item(find_item(item_value))
            }

            this.set_item_label = function(item_value, new_label) {
                find_item(item_value, function(item, item_id) {
                    // update model
                    item.label = new_label
                    // update GUI
                    $("#" + anchor_id(item_id)).text(new_label)
                })
            }

            /**
             * Returns the selected menu item.
             * If the menu has no items, undefined/null is returned.
             */
            this.get_selection = function() {
                return selection
            }

            this.get_item_count = function() {
                return items.length
            }

            this.hide = function() {
                hide_menu()
            }

            this.dom = dom

            // --------------------------------------------------------------------------------------- Private Functions



            /****************/
            /*** The Menu ***/
            /****************/



            function build_menu() {
                menu = $("<div>").addClass("contextmenu").css({position: "absolute"})
                if (items) {
                    $.each(items, function(i, item) {
                        add_item(item)
                    })
                } else {
                    //
                    items = []
                    //
                    $("#" + menu_id + " option").each(function() {
                        // Note 1: "this" references the <option> DOM element.
                        // Note 2: if there is no explicit "value" attribute the value equals the label.
                        add_item({label: $(this).text(), value: this.value})
                    })
                }
                hide_menu()
            }

            /**
             * @param   item    object with "value" (optional) and "label" properties.
             */
            function add_item(item) {
                // 1) update model
                items.push(item)
                // 2) update GUI
                var item_id = items.length - 1
                var anchor = $("<a>").attr({href: "#", id: anchor_id(item_id)}).click(item_selected)
                if (item.icon) {
                    anchor.append(dm3c.image_tag(item.icon, "menu-icon"))
                }
                anchor.append(item.label)
                menu.append(anchor)
                // select the item if there is no selection yet
                if (!selection) {
                    // Note: this sets also the button label (in case of stateful select-like menus)
                    select_item(item)
                }
            }

            /**
             * @param   item    object with "value" and "label" properties. If undefined nothing is performed.
             */
            function select_item(item) {
                // Note: only select-like menus have selection state.
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
             * @param   anchor      the <a> jQuery object
             * @return              the menu item (object with "value" and "label" properties)
             */
            function get_item(anchor) {
                return items[item_id(anchor.attr("id"))]
            }

            function item_selected() {
                // 1) remember selection
                // Note: "this" references the <a> DOM element.
                var item = get_item($(this))
                select_item(item)
                // 2) hide menu
                hide_menu()
                // 3) call handler
                if (handler) {
                     handler(item, menu_id)
                }
                return false
            }

            /**
             * Calculates the position of the menu and shows it.
             */
            function show_menu() {
                var pos = button.position()
                var height = button.outerHeight()
                menu.css({top: pos.top + height, left: pos.left})
                menu.show()
            }

            function hide_menu() {
                menu.hide()
            }



            /******************/
            /*** The Button ***/
            /******************/



            function build_button() {
                // Note: type="button" is required. Otherwise the button acts as submit button (if contained in a form).
                // Update: type="button" moved into element because attr("type", ...) is ignored in jQuery 1.4/Safari.
                button = $("<button type='button'>").click(button_clicked)
                button.button({icons: {primary: "ui-icon-triangle-1-s"}})
                // set button label
                if (menu_title) {
                    set_button_label(menu_title)
                }
            }

            function set_button_label(label) {
                button.button("option", "label", label)
            }

            function button_clicked() {
                if (dm3c.LOG_GUI) dm3c.log("Button of menu \"" + menu_id + "\" clicked")
                if (menu.css("display") == "none") {
                    hide_all_menus()
                    show_menu()
                } else {
                    hide_menu()
                }
                return false
            }



            /********************/
            /*** The Compound ***/
            /********************/



            function add_to_page() {
                dom = $("<span>").attr("id", menu_id).append(button).append(menu)
                $("#" + menu_id).replaceWith(dom)
            }



            /**************/
            /*** Helper ***/
            /**************/



            /**
             * Finds a menu item by value.
             * If there is no such menu item undefined is returned.
             */
            function find_item(value, func) {
                for (var i = 0, item; item = items[i]; i++) {
                    if (item.value == value) {
                        if (func) {
                            func(item, i)
                        } else {
                            return item
                        }
                    }
                }
            }

            function anchor_id(item_id) {
                return menu_id + "_item_" + item_id
            }

            function item_id(anchor_id) {
                return anchor_id.substring((menu_id + "_item_").length)
            }
        }
    }

    /**
     * @param   item    object with "value" and "label" properties.
     */
    this.add_menu_item = function(menu_id, item) {
        menus[menu_id].add_item(item)
    }

    this.add_menu_separator = function(menu_id) {
        menus[menu_id].add_separator()
    }

    this.empty_menu = function(menu_id) {
        menus[menu_id].empty()
    }

    this.select_menu_item = function(menu_id, item_value) {
        menus[menu_id].select(item_value)
    }

    this.set_menu_item_label = function(menu_id, item_value, new_label) {
        menus[menu_id].set_item_label(item_value, new_label)
    }

    /**
     * Returns the selected menu item.
     * If the menu has no items, undefined/null is returned.
     */
    this.menu_item = function(menu_id) {
        return menus[menu_id].get_selection()
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    function hide_all_menus() {
        for (var menu_id in menus) {
            menus[menu_id].hide()
        }
    }
}
