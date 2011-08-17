function help_plugin() {

    dm4c.register_css_stylesheet("/de.deepamehta.help/style/help.css")



    // ***********************************************************
    // *** Webclient Hooks (triggered by deepamehta-webclient) ***
    // ***********************************************************



    this.init = function() {
        dm4c.toolbar.special_menu.add_item({label: "About DeepaMehta", handler: do_about})
        dm4c.ui.dialog("about-dialog", "About DeepaMehta", build_dialog(), "auto")
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    function do_about() {
        \$("#about-dialog").dialog("open")  // Note: $ is escaped for Maven resource filtering
    }

    function build_dialog() {
        return $(
            '<table>' +
                '<tr>' +
                    '<td>' +
                        '<img src="/images/deepamehta-logo.png">' +
                    '</td>' +
                    '<td>' +
                        '<div class="field-label">Version</div>' +
                        '<div class="field-value">${project.version}</div>' +
                        '<div class="field-label">Developers</div>' +
                        '<div class="field-value">' +
                            'Jörg Richter<br>' +
                            'Danny Gräf<br>' +
                            'Malte Reißig<br>' +
                            'Torsten Ziegler<br>' +
                            'Enrico Schnepel<br>' +
                        '</div>' +
                        '<div class="field-label">License</div>' +
                        '<div class="field-value">GNU General Public License, v3</div>' +
                        '<div class="field-label">Website</div>' +
                        '<div class="field-value">' +
                            '<a href="http://www.deepamehta.de/" target="_blank">www.deepamehta.de</a>' +
                        '</div>' +
                    '</td>' +
                '</tr>' +
            '</table>'
        )
    }
}
