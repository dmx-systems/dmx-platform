dm4c.add_simple_renderer("dm4.webclient.html_renderer", new function() {

    this.render_info = function(page_model, parent_element) {
        dm4c.render.field_label(page_model, parent_element)
        parent_element.append(page_model.value)
    }

    this.render_form = function(page_model, parent_element) {
        dm4c.render.field_label(page_model, parent_element)
        var textarea = $("<textarea>")
            .attr("rows", page_model.input_field_rows)
            .text(page_model.value)
        parent_element.append(textarea)
        var editor = CKEDITOR.replace(textarea.get(0), CKEDITOR_CONFIG)
        //
        return function() {
            return editor.getData()
        }
    }

    var CKEDITOR_CONFIG = {

        // Avoid loading a config file
        customConfig: "",

        // The toolbar groups arrangement, optimized for a single toolbar row.
        toolbarGroups: [
            {name: "document",    groups: ["mode", "document", "doctools"]},
            {name: "clipboard",   groups: ["clipboard", "undo"]},
            {name: "editing",     groups: ["find", "selection", "spellchecker"]},
            {name: "forms"},
            {name: "basicstyles", groups: ["basicstyles", "cleanup"]},
            {name: "paragraph",   groups: ["list", "blocks", "align", "bidi"]},
            {name: "links"},
            {name: "insert"},
            {name: "styles"},
            {name: "colors"},
            {name: "tools"},
            {name: "others"},
            {name: "about"}
        ],

        // The default plugins included in the basic setup define some buttons that
        // are not needed in a basic editor. They are removed here.
        removeButtons: "Cut,Copy,Paste,Undo,Redo,Anchor,Underline,Strike,Subscript,Superscript",

        // Remove the "advanced" tab from the "link" dialog
        removeDialogTabs: "link:advanced",

        contentsCss: "/de.deepamehta.webclient/css/ckeditor-contents.css",

        stylesSet: [
            // Block styles
            {name: "Paragraph",   element: "p"},
            {name: "Heading 1",   element: "h1"},
            {name: "Heading 2",   element: "h2"},
            {name: "Heading 3",   element: "h3"},
            {name: "Heading 4",   element: "h4"},
            {name: "Code Block",  element: "pre",  attributes: {class: "code-block"}},
            {name: "Block Quote", element: "div",  attributes: {class: "blockquote"}},
            // Inline Styles
            {name: "Code",        element: "code"},
            {name: "Marker",      element: "span", attributes: {class: "marker"}}
        ],

        autoGrow_onStartup: true
    }
})
