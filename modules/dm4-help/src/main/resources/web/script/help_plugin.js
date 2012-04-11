function help_plugin() {

    dm4c.load_stylesheet("/de.deepamehta.help/style/help.css")

    // === Webclient Listeners ===

    dm4c.register_listener("init", function() {
        dm4c.toolbar.special_menu.add_item({label: "About DeepaMehta", handler: do_about})
        dm4c.ui.dialog("about-dialog", "About DeepaMehta", dialog_content(), "auto")
    })

    // ----------------------------------------------------------------------------------------------- Private Functions

    function do_about() {
        \$("#about-dialog").dialog("open")  // Note: $ is escaped from Maven resource filtering
    }

    function dialog_content() {
        return $(
            '<table>' +
                '<tr>' +
                    '<td>' +
                        '<img src="/images/deepamehta-logo.png">' +
                    '</td>' +
                    '<td>' +
                        '<div class="field-label">Version</div>' +
                        '<div class="field-value">${project.version}</div>' +
                        '<div class="field-label">Build Date</div>' +
                        '<div class="field-value">April 10, 2012</div>' +
                        '<div class="field-label">Copyright</div>' +
                        '<div class="field-value">2000-2012 Jörg Richter</div>' +
                        '<div class="field-label">License</div>' +
                        '<div class="field-value">GNU General Public License, v3</div>' +
                        '<div class="field-label">Website</div>' +
                        '<div class="field-value">' +
                            '<a href="http://www.deepamehta.de/" target="_blank">www.deepamehta.de</a>' +
                        '</div>' +
                    '</td>' +
                    '<td>' +
                        '<div class="field-label">Contributors</div>' +
                        '<div class="field-value">' +
                            'Martin Delius<br>' +
                            'Andreas Gebhard<br>' +
                            'Danny Gräf<br>' +
                            'Rolf Kutz<br>' +
                            'Annette Leeb<br>' +
                            'Urs Lerch<br>' +
                            'Matthias Melcher<br>' +
                            'Silke Meyer<br>' +
                            'Christiane Müller<br>' +
                            'Jürgen Neumann<br>' +
                        '</div>' +
                    '</td>' +
                    '<td>' +
                        '<div class="field-label" style="visibility: hidden">Contributors</div>' +
                        '<div class="field-value">' +
                            'Jurij Poelchau<br>' +
                            'Ingo Rau<br>' +
                            'Malte Reißig<br>' +
                            'Jörg Richter<br>' +
                            'Vincent Scheffler<br>' +
                            'Enrico Schnepel<br>' +
                            'Thilo Schönnemann<br>' +
                            'Matthias Staps<br>' +
                            'Andreas Wichmann<br>' +
                            'Torsten Ziegler<br>' +
                            '... <i>and many others</i>' +
                        '</div>' +
                    '</td>' +
                '</tr>' +
            '</table>'
        )
    }
}
