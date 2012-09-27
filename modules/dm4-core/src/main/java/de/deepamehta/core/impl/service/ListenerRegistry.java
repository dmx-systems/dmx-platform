package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Listener;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.event.AllPluginsActiveListener;
import de.deepamehta.core.service.event.InitializePluginListener;
import de.deepamehta.core.service.event.IntroduceTopicTypeListener;
import de.deepamehta.core.service.event.PluginServiceArrivedListener;
import de.deepamehta.core.service.event.PluginServiceGoneListener;
import de.deepamehta.core.service.event.PostCreateAssociationListener;
import de.deepamehta.core.service.event.PostCreateTopicListener;
import de.deepamehta.core.service.event.PostDeleteAssociationListener;
import de.deepamehta.core.service.event.PostInstallPluginListener;
import de.deepamehta.core.service.event.PostRetypeAssociationListener;
import de.deepamehta.core.service.event.PostUpdateTopicListener;
import de.deepamehta.core.service.event.PreCreateAssociationListener;
import de.deepamehta.core.service.event.PreCreateTopicListener;
import de.deepamehta.core.service.event.PreDeleteAssociationListener;
import de.deepamehta.core.service.event.PreSendAssociationListener;
import de.deepamehta.core.service.event.PreSendTopicListener;
import de.deepamehta.core.service.event.PreSendTopicTypeListener;
import de.deepamehta.core.service.event.PreUpdateTopicListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



class ListenerRegistry {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    /**
     * The registered listeners, hashed by event name (name of CoreEvent enum constant, e.g. "POST_CREATE_TOPIC").
     */
    private Map<String, List<Listener>> listenerRegistry = new HashMap<String, List<Listener>>();

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void addListener(CoreEvent event, Listener listener) {
        getListeners(event).add(listener);
    }

    void removeListener(CoreEvent event, Listener listener) {
        if (getListeners(event).remove(listener) == false) {
            throw new RuntimeException("Removing " + listener + " from " +
                event + " listeners failed. Unregistered?");
        }
    }

    // ---

    void fireEvent(CoreEvent event, Object... params) {
        try {
            // ### FIXME: ConcurrentModificationException might occur
            for (Listener listener : getListeners(event)) {
                deliverEvent(listener, event, params);
            }
        } catch (Exception e) {
            throw new RuntimeException("Firing event " + event + " failed (params=" + params + ")", e);
        }
    }

    void deliverEvent(Listener listener, CoreEvent event, Object... params) {
        try {
            switch (event) {

            case ALL_PLUGINS_ACTIVE:
                ((AllPluginsActiveListener) listener).allPluginsActive();
                break;

            case INITIALIZE_PLUGIN:
                ((InitializePluginListener) listener).initializePlugin();
                break;

            case INTRODUCE_TOPIC_TYPE:
                ((IntroduceTopicTypeListener) listener).introduceTopicType(//
                        (TopicType) params[0],//
                        (ClientState) params[1]);
                break;

            case PLUGIN_SERVICE_ARRIVED:
                ((PluginServiceArrivedListener) listener).pluginServiceArrived(//
                        (PluginService) params[0]);
                break;

            case PLUGIN_SERVICE_GONE:
                ((PluginServiceGoneListener) listener).pluginServiceGone(//
                        (PluginService) params[0]);
                break;

            case POST_CREATE_ASSOCIATION:
                ((PostCreateAssociationListener) listener).postCreateAssociation(//
                        (Association) params[0],//
                        (ClientState) params[1],//
                        (Directives) params[2]);
                break;

            case POST_CREATE_TOPIC:
                ((PostCreateTopicListener) listener).postCreateTopic(//
                        (Topic) params[0],//
                        (ClientState) params[1],//
                        (Directives) params[2]);
                break;

            case POST_DELETE_ASSOCIATION:
                ((PostDeleteAssociationListener) listener).postDeleteAssociation(//
                        (Association) params[0],//
                        (Directives) params[1]);
                break;

            case POST_INSTALL_PLUGIN:
                ((PostInstallPluginListener) listener).postInstallPlugin();
                break;

            case POST_RETYPE_ASSOCIATION:
                ((PostRetypeAssociationListener) listener).postRetypeAssociation(//
                        (Association) params[0],//
                        (String) params[1],//
                        (Directives) params[2]);
                break;

            case POST_UPDATE_TOPIC:
                ((PostUpdateTopicListener) listener).postUpdateTopic(//
                        (Topic) params[0],//
                        (TopicModel) params[1],//
                        (TopicModel) params[2],//
                        (ClientState) params[3],//
                        (Directives) params[4]);
                break;

            case PRE_CREATE_ASSOCIATION:
                ((PreCreateAssociationListener) listener).preCreateAssociation(//
                        (AssociationModel) params[0],//
                        (ClientState) params[1]);
                break;

            case PRE_CREATE_TOPIC:
                ((PreCreateTopicListener) listener).preCreateTopic(//
                        (TopicModel) params[0],//
                        (ClientState) params[1]);
                break;

            case PRE_DELETE_ASSOCIATION:
                ((PreDeleteAssociationListener) listener).preDeleteAssociation(//
                        (Association) params[0],//
                        (Directives) params[1]);
                break;

            case PRE_SEND_ASSOCIATION:
                ((PreSendAssociationListener) listener).preSendAssociation(//
                        (Association) params[0],//
                        (ClientState) params[1]);
                break;

            case PRE_SEND_TOPIC:
                ((PreSendTopicListener) listener).preSendTopic(//
                        (Topic) params[0],//
                        (ClientState) params[1]);
                break;

            case PRE_SEND_TOPIC_TYPE:
                ((PreSendTopicTypeListener) listener).preSendTopicType(//
                        (TopicType) params[0],//
                        (ClientState) params[1]);
                break;

            case PRE_UPDATE_TOPIC:
                ((PreUpdateTopicListener) listener).preUpdateTopic(//
                        (Topic) params[0],//
                        (TopicModel) params[1],//
                        (Directives) params[2]);
                break;

            default:
                throw new RuntimeException("Unknown core event " + event);
            }
        } catch (Exception e) {     // NoSuchMethodException, IllegalAccessException, InvocationTargetException
            throw new RuntimeException("Delivering event " + event + " to " + listener + " failed", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private List<Listener> getListeners(CoreEvent event) {
        List<Listener> listeners = listenerRegistry.get(event.name());
        if (listeners == null) {
            listeners = new ArrayList<Listener>();
            listenerRegistry.put(event.name(), listeners);
        }
        return listeners;
    }

}
