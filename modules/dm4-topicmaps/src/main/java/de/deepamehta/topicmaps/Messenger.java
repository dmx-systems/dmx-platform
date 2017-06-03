package de.deepamehta.topicmaps;

import de.deepamehta.core.model.topicmaps.TopicViewModel;
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

    void addTopicToTopicmap(long topicmapId, TopicViewModel topic) throws JSONException {
        messageToAllButOne(new JSONObject()
            .put("type", "addTopicToTopicmap")
            .put("args", new JSONObject()
                .put("topicmapId", topicmapId)
                .put("viewTopic", topic.toJSON())
            )
        );
    }

    void setTopicPosition(long topicmapId, long topicId, int x, int y) throws JSONException {
        messageToAllButOne(new JSONObject()
            .put("type", "setTopicPosition")
            .put("args", new JSONObject()
                .put("topicmapId", topicmapId)
                .put("topicId", topicId)
                .put("pos", new JSONObject()
                    .put("x", x)
                    .put("y", y)
                )
            )
        );
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void messageToAllButOne(JSONObject message) {
        webSocketsService.messageToAllButOne(pluginUri, message.toString());
    }
}
