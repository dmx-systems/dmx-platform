package systems.dmx.topicmaps;

import systems.dmx.core.Topic;
import systems.dmx.core.model.topicmaps.ViewAssoc;
import systems.dmx.core.model.topicmaps.ViewTopic;
import systems.dmx.core.service.Messages;
import systems.dmx.core.service.Messages.Dest;
import systems.dmx.core.service.websocket.WebSocketService;

import org.codehaus.jettison.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;



class Messenger {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void newTopicmap(Topic topicmapTopic) {
        try {
            sendToReadAllowed(new JSONObject()
                .put("type", "newTopicmap")
                .put("args", new JSONObject()
                    .put("topicmapTopic", topicmapTopic.toJSON())
                ), topicmapTopic.getId()
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
        Messages.get().add(Dest.ALL_BUT_ORIGIN, message.toString());
    }

    private void sendToReadAllowed(JSONObject message, long objectId) {
        Messages.get().add(Dest.READ_ALLOWED, message.toString(), objectId);
    }
}
