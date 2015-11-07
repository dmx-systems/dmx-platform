package de.deepamehta.plugins.files.migrations;

import de.deepamehta.core.service.Migration;



/**
 * Installs the file size renderer.
 * Runs ALWAYS.
 * <p>
 * Part of DM 4.8-SNAPSHOT
 */
public class Migration5 extends Migration {

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void run() {
        addTopicTypeSetting("dm4.files.size", "simple_renderer_uri", "dm4.files.file_size_renderer");
    }
}
