package de.deepamehta.plugins.time;

import de.deepamehta.plugins.time.service.TimeService;

import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.event.PostCreateAssociationListener;
import de.deepamehta.core.service.event.PostCreateTopicListener;
import de.deepamehta.core.service.event.PostUpdateAssociationListener;
import de.deepamehta.core.service.event.PostUpdateTopicListener;
import de.deepamehta.core.service.event.PreSendAssociationListener;
import de.deepamehta.core.service.event.PreSendResponseListener;
import de.deepamehta.core.service.event.PreSendTopicListener;

// ### TODO: remove Jersey dependency. Move to JAX-RS 2.0.
import com.sun.jersey.spi.container.ContainerResponse;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MultivaluedMap;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;



@Path("/time")
@Consumes("application/json")
@Produces("application/json")
public class TimePlugin extends PluginActivator implements TimeService, PostCreateTopicListener,
                                                                        PostCreateAssociationListener,
                                                                        PostUpdateTopicListener,
                                                                        PostUpdateAssociationListener,
                                                                        PreSendTopicListener,
                                                                        PreSendAssociationListener,
                                                                        PreSendResponseListener {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static String URI_CREATED  = "dm4.time.created";
    private static String URI_MODIFIED = "dm4.time.modified";

    private static String HEADER_LAST_MODIFIED = "Last-Modified";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DateFormat rfc2822;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **********************************
    // *** TimeService Implementation ***
    // **********************************



    // === Timestamps ===

    // Note: the timestamp getters must return 0 as default. Before we used -1 but Jersey's evaluatePreconditions()
    // does not work as expected when called with a negative value which is not dividable by 1000.

    @Override
    public long getTopicCreationTime(long topicId) {
        return dms.hasTopicProperty(topicId, URI_CREATED) ? (Long) dms.getTopicProperty(topicId, URI_CREATED) : 0;
    }

    @Override
    public long getTopicModificationTime(long topicId) {
        return dms.hasTopicProperty(topicId, URI_MODIFIED) ? (Long) dms.getTopicProperty(topicId, URI_MODIFIED) : 0;
    }

    @Override
    public long getAssociationCreationTime(long assocId) {
        return dms.hasAssociationProperty(assocId, URI_CREATED) ? (Long) dms.getAssociationProperty(assocId,
            URI_CREATED) : 0;
    }

    @Override
    public long getAssociationModificationTime(long assocId) {
        return dms.hasAssociationProperty(assocId, URI_MODIFIED) ? (Long) dms.getAssociationProperty(assocId,
            URI_MODIFIED) : 0;
    }

    // === Retrieval ===

    @GET
    @Path("/from/{from}/to/{to}/topics/created")
    @Override
    public Collection<Topic> getTopicsByCreationTime(@PathParam("from") long from,
                                                     @PathParam("to") long to) {
        return dms.getTopicsByPropertyRange(URI_CREATED, from, to);
    }

    @GET
    @Path("/from/{from}/to/{to}/topics/modified")
    @Override
    public Collection<Topic> getTopicsByModificationTime(@PathParam("from") long from,
                                                         @PathParam("to") long to) {
        return dms.getTopicsByPropertyRange(URI_MODIFIED, from, to);
    }

    @GET
    @Path("/from/{from}/to/{to}/assocs/created")
    @Override
    public Collection<Association> getAssociationsByCreationTime(@PathParam("from") long from,
                                                                 @PathParam("to") long to) {
        return dms.getAssociationsByPropertyRange(URI_CREATED, from, to);
    }

    @GET
    @Path("/from/{from}/to/{to}/assocs/modified")
    @Override
    public Collection<Association> getAssociationsByModificationTime(@PathParam("from") long from,
                                                                     @PathParam("to") long to) {
        return dms.getAssociationsByPropertyRange(URI_MODIFIED, from, to);
    }



    // ****************************
    // *** Hook Implementations ***
    // ****************************



    @Override
    public void init() {
        // create the date format used in HTTP date/time headers, see:
        // http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3
        rfc2822 = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.ENGLISH);
        rfc2822.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        ((SimpleDateFormat) rfc2822).applyPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
    }



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void postCreateTopic(Topic topic, ClientState clientState, Directives directives) {
        storeTimestamps(topic);
    }

    @Override
    public void postCreateAssociation(Association assoc, ClientState clientState, Directives directives) {
        storeTimestamps(assoc);
    }

    @Override
    public void postUpdateTopic(Topic topic, TopicModel newModel, TopicModel oldModel, ClientState clientState,
                                                                                       Directives directives) {
        storeTimestamp(topic);
    }

    @Override
    public void postUpdateAssociation(Association assoc, AssociationModel oldModel, ClientState clientState,
                                                                                    Directives directives) {
        storeTimestamp(assoc);
    }

    // ---

    @Override
    public void preSendTopic(Topic topic, ClientState clientState) {
        enrichWithTimestamp(topic);
    }

    @Override
    public void preSendAssociation(Association assoc, ClientState clientState) {
        enrichWithTimestamp(assoc);
    }

    // ---

    @Override
    public void preSendResponse(ContainerResponse response) {
        DeepaMehtaObject object = responseObject(response);
        if (object != null) {
            long modified = enrichWithTimestamp(object);
            setLastModifiedHeader(response, modified);
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private void storeTimestamps(DeepaMehtaObject object) {
        long time = System.currentTimeMillis();
        storeCreationTime(object, time);
        storeModificationTime(object, time);
    }

    private void storeTimestamp(DeepaMehtaObject object) {
        long time = System.currentTimeMillis();
        storeModificationTime(object, time);
    }

    // ---

    private void storeCreationTime(DeepaMehtaObject object, long time) {
        storeTime(object, URI_CREATED, time);
    }

    private void storeModificationTime(DeepaMehtaObject object, long time) {
        storeTime(object, URI_MODIFIED, time);
    }

    // ---

    private void storeTime(DeepaMehtaObject object, String propUri, long time) {
        if (object instanceof Topic) {
            dms.setTopicProperty(object.getId(), propUri, time, true);          // addToIndex=true
        } else if (object instanceof Association) {
            dms.setAssociationProperty(object.getId(), propUri, time, true);    // addToIndex=true
        } else {
            throw new RuntimeException("Unexpected object: " + object);
        }
    }

    // ===

    // ### FIXME: copy in CachingPlugin
    private DeepaMehtaObject responseObject(ContainerResponse response) {
        Object entity = response.getEntity();
        return entity instanceof DeepaMehtaObject ? (DeepaMehtaObject) entity : null;
    }

    private long enrichWithTimestamp(DeepaMehtaObject object) {
        long created = getCreationTime(object);
        long modified = getModificationTime(object);
        CompositeValueModel comp = object.getCompositeValue().getModel();
        comp.put(URI_CREATED, created);
        comp.put(URI_MODIFIED, modified);
        return modified;
    }

    // ---

    private long getCreationTime(DeepaMehtaObject object) {
        if (object instanceof Topic) {
            return getTopicCreationTime(object.getId());
        } else if (object instanceof Association) {
            return getAssociationCreationTime(object.getId());
        } else {
            throw new RuntimeException("Unexpected object: " + object);
        }
    }

    private long getModificationTime(DeepaMehtaObject object) {
        if (object instanceof Topic) {
            return getTopicModificationTime(object.getId());
        } else if (object instanceof Association) {
            return getAssociationModificationTime(object.getId());
        } else {
            throw new RuntimeException("Unexpected object: " + object);
        }
    }

    // ---

    private void setLastModifiedHeader(ContainerResponse response, long time) {
        setHeader(response, HEADER_LAST_MODIFIED, rfc2822.format(time));
    }

    // ### FIXME: copy in CachingPlugin
    private void setHeader(ContainerResponse response, String header, String value) {
        MultivaluedMap headers = response.getHttpHeaders();
        //
        if (headers.containsKey(header)) {
            throw new RuntimeException("Response already has a \"" + header + "\" header");
        }
        //
        headers.putSingle(header, value);
    }
}
