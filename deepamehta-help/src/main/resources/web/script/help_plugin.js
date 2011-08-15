function help_plugin() {

    dm4c.register_css_stylesheet("/de.deepamehta.help/style/help.css")



    // ***********************************************************
    // *** Webclient Hooks (triggered by deepamehta-webclient) ***
    // ***********************************************************



    this.init = function() {
        dm4c.toolbar.special_menu.add_item({label: "About DeepaMehta", handler: do_about})
        dm4c.ui.dialog("about-dialog", "About DeepaMehta", build_dialog(), "28em")
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    function do_about() {
        $("#about-dialog").dialog("open")
    }

    function build_dialog() {
        return $(
            '<img src="/de.deepamehta.webclient/images/deepamehta-logo.png">' +
            '<p>Developed by</p>' +
            '<p>' +
                'Jörg Richter<br>' +
                'Danny Gräf<br>' +
                'Malte Reißig<br>' +
                'Torsten Ziegler<br>' +
                'Enrico Schnepel<br>' +
            '</p>'
        )
    }
}
