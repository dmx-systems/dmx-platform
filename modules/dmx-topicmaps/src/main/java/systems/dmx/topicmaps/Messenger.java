package systems.dmx.topicmaps;

import systems.dmx.core.Topic;
import systems.dmx.core.model.topicmaps.ViewAssoc;
import systems.dmx.core.model.topicmaps.ViewTopic;
import systems.dmx.core.service.websocket.WebSocketService;

import org.codehaus.jettison.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;



class Messenger {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String pluginUri = "systems.dmx.webclient";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private WebSocketService wss;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    Messenger(WebSocketService wss) {
        this.wss = wss;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void newTopicmap(Topic topicmapTopic) {
        try {
            // FIXME: send message only to users who have READ permission for the topicmap topic.
            // Unfortunately we can't just use sendToReadAllowed() as the permission check is performed in another thread
            // (WebSocketService's SendMessageWorker), and the create-topicmap transaction is not yet committed.
            // The result would be "org.neo4j.graphdb.NotFoundException: 'typeUri' property not found for NodeImpl#1234"
            // (where 1234 is the ID of the just created topicmap).
            sendToAllButOrigin(new JSONObject()
                .put("type", "newTopicmap")
                .put("args", new JSONObject()
                    .put("topicmapTopic", topicmapTopic.toJSON())
                )
            );
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error while sending a \"newTopicmap\" message:", e);
        }
    }

    void addTopicToTopicmap(long topicmapId, ViewTopic topic) {
        try {
            sendToReadAllowed(new JSONObject()
                .put("type", "addTopicToTopicmap")
                .put("args", new JSONObject()
                    .put("topicmapId", topicmapId)
                    .put("viewTopic", topic.toJSON())
                ), topic.getId()
            );
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error while sending a \"addTopicToTopicmap\" message:", e);
        }
    }

    void addAssocToTopicmap(long topicmapId, ViewAssoc assoc) {
        try {
            sendToReadAllowed(new JSONObject()
                .put("type", "addAssocToTopicmap")
                .put("args", new JSONObject()
                    .put("topicmapId", topicmapId)
                    .put("viewAssoc", assoc.toJSON())
                ), assoc.getId()
            );
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error while sending a \"addAssocToTopicmap\" message:", e);
        }
    }

    void setTopicPosition(long topicmapId, long topicId, int x, int y) {
        try {
            sendToAllButOrigin(new JSONObject()
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
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error while sending a \"setTopicPosition\" message:", e);
        }
    }

    void setTopicVisibility(long topicmapId, long topicId, boolean visibility) {
        try {
            sendToAllButOrigin(new JSONObject()
                .put("type", "setTopicVisibility")
                .put("args", new JSONObject()
                    .put("topicmapId", topicmapId)
                    .put("topicId", topicId)
                    .put("visibility", visibility)
                )
            );
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error while sending a \"setTopicVisibility\" message:", e);
        }
    }

    void setAssocVisibility(long topicmapId, long assocId, boolean visibility) {
        try {
            sendToAllButOrigin(new JSONObject()
                .put("type", "setAssocVisibility")
                .put("args", new JSONObject()
                    .put("topicmapId", topicmapId)
                    .put("assocId", assocId)
                    .put("visibility", visibility)
                )
            );
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error while sending a \"setAssocVisibility\" message:", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void sendToAllButOrigin(JSONObject message) {
        wss.sendToAllButOrigin(message.toString());
    }

    private void sendToReadAllowed(JSONObject message, long objectId) {
        wss.sendToReadAllowed(message.toString(), objectId);
    }
}
