package de.deepamehta.core.impl.service;

import de.deepamehta.core.service.PluginInfo;
import org.codehaus.jettison.json.JSONObject;
import org.osgi.framework.Bundle;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;



class PluginInfoImpl implements PluginInfo {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String PLUGIN_JAVASCRIPT_FILE = "/web/script/plugin.js";
    private static final String PLUGIN_RENDERERS_PATH  = "/web/script/renderers/";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Bundle pluginBundle;
    private JSONObject pluginInfo = new JSONObject();

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    PluginInfoImpl(String pluginUri, Bundle pluginBundle) {
        this.pluginBundle = pluginBundle;
        try {
            pluginInfo.put("plugin_uri", pluginUri);
            pluginInfo.put("has_client_component", pluginBundle.getEntry(PLUGIN_JAVASCRIPT_FILE) != null);
            //
            JSONObject renderers = new JSONObject();
            renderers.put("page_renderers",   getRenderers("page_renderers"));
            renderers.put("simple_renderers", getRenderers("simple_renderers"));
            renderers.put("multi_renderers",  getRenderers("multi_renderers"));
            // ### renderers.put("canvas_renderers", getRenderers("canvas_renderers"));
            pluginInfo.put("renderers", renderers);
            //
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

    private List<String> getRenderers(String renderersDir) {
        List<String> renderers = new ArrayList();
        Enumeration<String> e = pluginBundle.getEntryPaths(PLUGIN_RENDERERS_PATH + renderersDir);
        if (e != null) {
            while (e.hasMoreElements()) {
                String entryPath = e.nextElement();
                String renderer = entryPath.substring(entryPath.lastIndexOf('/') + 1);
                renderers.add(renderer);
            }
        }
        return renderers;
    }
}
