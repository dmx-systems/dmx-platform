package de.deepamehta.core.impl;

import de.deepamehta.core.service.PluginInfo;
import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import org.osgi.framework.Bundle;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;



class PluginInfoImpl implements PluginInfo {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String PLUGIN_FILE           = "/web/script/plugin.js";
    private static final String PLUGIN_RENDERERS_PATH = "/web/script/renderers/";
    private static final String PLUGIN_HELPER_PATH    = "/web/script/helper/";
    private static final String PLUGIN_STYLE_PATH     = "/web/style/";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Bundle pluginBundle;
    private JSONObject pluginInfo = new JSONObject();

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    PluginInfoImpl(String pluginUri, Bundle pluginBundle) {
        this.pluginBundle = pluginBundle;
        try {
            pluginInfo.put("plugin_uri", pluginUri);
            pluginInfo.put("has_plugin_file", hasPluginFile());
            pluginInfo.put("stylesheets", getStylesheets());
            pluginInfo.put("renderers", getRenderers());
            pluginInfo.put("helper", getHelper());
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        return pluginInfo;
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private boolean hasPluginFile() {
        return pluginBundle.getEntry(PLUGIN_FILE) != null;
    }

    private List<String> getStylesheets() {
        return getFilenames(PLUGIN_STYLE_PATH);
    }

    private JSONObject getRenderers() throws JSONException {
        JSONObject renderers = new JSONObject();
        renderers.put("page_renderers",   getRenderers("page_renderers"));
        renderers.put("simple_renderers", getRenderers("simple_renderers"));
        renderers.put("multi_renderers",  getRenderers("multi_renderers"));
        // ### TODO: renderers.put("topicmap_renderers", getRenderers("topicmap_renderers"));
        return renderers;
    }

    private List<String> getHelper() {
        return getFilenames(PLUGIN_HELPER_PATH);
    }

    // ---

    private List<String> getRenderers(String renderersDir) {
        return getFilenames(PLUGIN_RENDERERS_PATH + renderersDir);
    }

    private List<String> getFilenames(String path) {
        List<String> filenames = new ArrayList();
        Enumeration<String> e = pluginBundle.getEntryPaths(path);
        if (e != null) {
            while (e.hasMoreElements()) {
                String entryPath = e.nextElement();
                // ignore directories ### TODO: to be dropped? Use helper/ instead? See Webclient page_renderers
                if (entryPath.endsWith("/")) {
                    continue;
                }
                //
                filenames.add(JavaUtils.getFilename(entryPath));
            }
        }
        return filenames;
    }
}
