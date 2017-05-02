package de.deepamehta.topicmaps;

import de.deepamehta.websockets.WebSocketsService;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;



class Messenger {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private String pluginUri;
    private WebSocketsService webSocketsService;

    // ---------------------------------------------------------------------------------------------------- Constructors

    Messenger(String pluginUri, WebSocketsService webSocketsService) {
        this.pluginUri = pluginUri;
        this.webSocketsService = webSocketsService;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void setTopicPosition(long topicmapId, long topicId, int x, int y) throws JSONException {
        broadcast(new JSONObject()
            .put("type", "setTopicPosition")
            .put("args", new JSONObject()
                .put("topicmapId", topicmapId)
                .put("topicId", topicId)
                .put("x", x)
                .put("y", y)
            )
        );
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void broadcast(JSONObject message) {
        webSocketsService.broadcast(pluginUri, message.toString());
    }
}
