dm4c.add_plugin("de.deepamehta.help", function() {

    dm4c.toolbar.special_menu.add_item({
        label: "About DeepaMehta", handler: open_about_dialog
    })

    function open_about_dialog() {
        dm4c.ui.dialog({
            id: "about-dialog", title: "About DeepaMehta", content: '<table>' +
                '<tr>' +
                    '<td>' +
                        '<img src="/de.deepamehta.webclient/images/deepamehta-logo.png">' +
                    '</td>' +
                    '<td>' +
                        '<div class="field-label">Version</div>' +
                        '<div>${project.version}</div>' +
                        '<div class="field-label">Release Date</div>' +
                        '<div>Apr 22, 2015</div>' +
                        '<div class="field-label">Copyright</div>' +
                        '<div>2000-2015 Jörg Richter et al.</div>' +
                        '<div class="field-label">License</div>' +
                        '<div>GNU General Public License, v3</div>' +
                        '<div class="field-label">Website</div>' +
                        '<div>' +
                            '<a href="http://www.deepamehta.de/" target="_blank">www.deepamehta.de</a>' +
                        '</div>' +
                    '</td>' +
                    '<td>' +
                        '<div class="field-label">Contributors</div>' +
                        '<div class="contributors">' +
                            'Martin Delius<br>' +
                            'Carolina García<br>' +
                            'Andreas Gebhard<br>' +
                            'Danny Gräf<br>' +
                            'Constantin Jucovschi<br>' +
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
                        '<div class="contributors">' +
                            'Jurij Poelchau<br>' +
                            'Ingo Rau<br>' +
                            'Malte Reißig<br>' +
                            'Jörg Richter<br>' +
                            'Vincent Scheffler<br>' +
                            'Enrico Schnepel<br>' +
                            'Thilo Schönnemann<br>' +
                            'Matthias Staps<br>' +
                            'Jörn Weißenborn<br>' +
                            'Andreas Wichmann<br>' +
                            'Torsten Ziegler<br>' +
                            '... <i>and many others</i>' +
                        '</div>' +
                    '</td>' +
                '</tr>' +
            '</table>'
        })
    }
})
