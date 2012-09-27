package de.deepamehta.core.impl.service;

import de.deepamehta.core.service.PluginInfo;
import de.deepamehta.core.util.DeepaMehtaUtils;
import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import org.osgi.framework.Bundle;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;



class PluginInfoImpl implements PluginInfo {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String PLUGIN_FILE            = "/web/script/plugin.js";
    private static final String PLUGIN_RENDERERS_PATH  = "/web/script/renderers/";
    private static final String PLUGIN_STYLE_PATH      = "/web/style/";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Bundle pluginBundle;
    private JSONObject pluginInfo = new JSONObject();

    // ---------------------------------------------------------------------------------------------------- Constructors

    PluginInfoImpl(String pluginUri, Bundle pluginBundle) {
        this.pluginBundle = pluginBundle;
        try {
            pluginInfo.put("plugin_uri", pluginUri);
            pluginInfo.put("has_plugin_file", hasPluginFile());
            pluginInfo.put("stylesheets", getStylesheets());
            pluginInfo.put("renderers", getRenderers());
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

    private List<String> getRenderers(String renderersDir) {
        return getFilenames(PLUGIN_RENDERERS_PATH + renderersDir);
    }

    private JSONObject getRenderers() throws JSONException {
        JSONObject renderers = new JSONObject();
        renderers.put("page_renderers",   getRenderers("page_renderers"));
        renderers.put("simple_renderers", getRenderers("simple_renderers"));
        renderers.put("multi_renderers",  getRenderers("multi_renderers"));
        // ### renderers.put("topicmap_renderers", getRenderers("topicmap_renderers"));
        return renderers;
    }

    // ---

    private List<String> getFilenames(String path) {
        List<String> filenames = new ArrayList<String>();
        Enumeration<String> e = DeepaMehtaUtils.cast(pluginBundle.getEntryPaths(path));
        if (e != null) {
            while (e.hasMoreElements()) {
                String entryPath = e.nextElement();
                filenames.add(JavaUtils.getFilename(entryPath));
            }
        }
        return filenames;
    }
}
