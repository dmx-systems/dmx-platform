package systems.dmx.topicmaps;



public interface TopicmapsConstants {

    // topic type
    static final String TOPICMAP           = "dmx.topicmaps.topicmap";

    // content assoc
    static final String TOPICMAP_CONTEXT   = "dmx.topicmaps.topicmap_context";
    static final String ROLE_TYPE_TOPICMAP = "dmx.core.default";
    static final String ROLE_TYPE_CONTENT  = "dmx.topicmaps.topicmap_content";

    // topicmap props
    static final String PROP_PAN_X         = "dmx.topicmaps.pan_x";
    static final String PROP_PAN_Y         = "dmx.topicmaps.pan_y";
    static final String PROP_ZOOM          = "dmx.topicmaps.zoom";

    // topic/assoc props
    static final String PROP_X             = "dmx.topicmaps.x";    // topic only
    static final String PROP_Y             = "dmx.topicmaps.y";    // topic only
    static final String PROP_VISIBILITY    = "dmx.topicmaps.visibility";
    static final String PROP_PINNED        = "dmx.topicmaps.pinned";
}
