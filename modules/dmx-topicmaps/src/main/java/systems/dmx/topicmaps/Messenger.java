package systems.dmx.topicmaps;

import systems.dmx.core.Topic;
import systems.dmx.core.model.topicmaps.ViewAssoc;
import systems.dmx.core.model.topicmaps.ViewTopic;
import systems.dmx.core.service.CoreService;

import org.codehaus.jettison.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;



class Messenger {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String pluginUri = "systems.dmx.webclient";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private CoreService dmx;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    Messenger(CoreService dmx) {
        this.dmx = dmx;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void newTopicmap(Topic topicmapTopic) {
        try {
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
            // FIXME: per connection check read access
            sendToAllButOrigin(new JSONObject()
                .put("type", "addTopicToTopicmap")
                .put("args", new JSONObject()
                    .put("topicmapId", topicmapId)
                    .put("viewTopic", topic.toJSON())
                )
            );
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error while sending a \"addTopicToTopicmap\" message:", e);
        }
    }

    void addAssocToTopicmap(long topicmapId, ViewAssoc assoc) {
        try {
            // FIXME: per connection check read access
            sendToAllButOrigin(new JSONObject()
                .put("type", "addAssocToTopicmap")
                .put("args", new JSONObject()
                    .put("topicmapId", topicmapId)
                    .put("viewAssoc", assoc.toJSON())
                )
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
        dmx.getWebSocketService().sendToAllButOrigin(message.toString());
    }
}
