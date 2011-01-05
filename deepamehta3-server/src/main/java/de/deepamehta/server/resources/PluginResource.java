package de.deepamehta.server.resources;

import de.deepamehta.core.model.Topic;
import de.deepamehta.core.osgi.Activator;
import de.deepamehta.core.util.JSONHelper;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



@Path("/plugin")
@Consumes("application/json")
@Produces("application/json")
public class PluginResource {

    private Logger logger = Logger.getLogger(getClass().getName());

    @GET
    public JSONArray listPlugins() throws JSONException {
        JSONArray plugins = new JSONArray();
        for (String pluginId : Activator.getService().getPluginIds()) {
            String pluginFile = Activator.getService().getPlugin(pluginId).getConfigProperty("clientSidePluginFile");
            JSONObject plugin = new JSONObject();
            plugin.put("plugin_id", pluginId);
            plugin.put("plugin_file", pluginFile);
            plugins.put(plugin);
        }
        return plugins;
    }
}
