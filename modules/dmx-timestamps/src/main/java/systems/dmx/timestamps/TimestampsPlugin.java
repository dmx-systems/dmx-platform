package systems.dmx.timestamps;

import systems.dmx.core.Assoc;
import systems.dmx.core.DMXObject;
import systems.dmx.core.Topic;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.event.PostCreateAssoc;
import systems.dmx.core.service.event.PostCreateTopic;
import systems.dmx.core.service.event.PostUpdateAssoc;
import systems.dmx.core.service.event.PostUpdateTopic;
import systems.dmx.core.service.event.PreSendAssoc;
import systems.dmx.core.service.event.PreSendTopic;
import systems.dmx.core.service.event.ServiceResponseFilter;

// ### TODO: hide Jersey internals. Upgrade to JAX-RS 2.0.
import com.sun.jersey.spi.container.ContainerResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MultivaluedMap;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;



@Path("/timestamps")
@Consumes("application/json")
@Produces("application/json")
public class TimestampsPlugin extends PluginActivator implements TimestampsService, PostCreateTopic,
                                                                                    PostCreateAssoc,
                                                                                    PostUpdateTopic,
                                                                                    PostUpdateAssoc,
                                                                                    PreSendTopic,
                                                                                    PreSendAssoc,
                                                                                    ServiceResponseFilter {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static String PROP_CREATED  = "dmx.timestamps.created";
    private static String PROP_MODIFIED = "dmx.timestamps.modified";

    private static String HEADER_LAST_MODIFIED = "Last-Modified";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DateFormat rfc2822;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // *************************
    // *** TimestampsService ***
    // *************************



    // === Timestamps ===

    // Note: the timestamp getters must return 0 as default. Before we used -1 but Jersey's evaluatePreconditions()
    // does not work as expected when called with a negative value which is not dividable by 1000.

    @GET
    @Path("/object/{id}/created")
    @Override
    public long getCreationTime(@PathParam("id") long objectId) {
        try {
            return dmx.hasProperty(objectId, PROP_CREATED) ? (Long) dmx.getProperty(objectId, PROP_CREATED) : 0;
        } catch (Exception e) {
            throw new RuntimeException("Fetching creation time of object " + objectId + " failed", e);
        }
    }

    @GET
    @Path("/object/{id}/modified")
    @Override
    public long getModificationTime(@PathParam("id") long objectId) {
        try {
            return dmx.hasProperty(objectId, PROP_MODIFIED) ? (Long) dmx.getProperty(objectId, PROP_MODIFIED) : 0;
        } catch (Exception e) {
            throw new RuntimeException("Fetching modification time of object " + objectId + " failed", e);
        }
    }

    // ---

    @Override
    public void setModified(DMXObject object) {
        storeTimestamp(object);
    }



    // === Retrieval ===

    @GET
    @Path("/from/{from}/to/{to}/topics/created")
    @Override
    public Collection<Topic> getTopicsByCreationTime(@PathParam("from") long from, @PathParam("to") long to) {
        return dmx.getTopicsByPropertyRange(PROP_CREATED, from, to);
    }

    @GET
    @Path("/from/{from}/to/{to}/topics/modified")
    @Override
    public Collection<Topic> getTopicsByModificationTime(@PathParam("from") long from, @PathParam("to") long to) {
        return dmx.getTopicsByPropertyRange(PROP_MODIFIED, from, to);
    }

    @GET
    @Path("/from/{from}/to/{to}/assocs/created")
    @Override
    public Collection<Assoc> getAssocsByCreationTime(@PathParam("from") long from, @PathParam("to") long to) {
        return dmx.getAssocsByPropertyRange(PROP_CREATED, from, to);
    }

    @GET
    @Path("/from/{from}/to/{to}/assocs/modified")
    @Override
    public Collection<Assoc> getAssocsByModificationTime(@PathParam("from") long from, @PathParam("to") long to) {
        return dmx.getAssocsByPropertyRange(PROP_MODIFIED, from, to);
    }



    // *************
    // *** Hooks ***
    // *************



    @Override
    public void init() {
        // create the date format used in HTTP date/time headers, see:
        // http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3
        rfc2822 = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.ENGLISH);
        rfc2822.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        ((SimpleDateFormat) rfc2822).applyPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
    }



    // *****************
    // *** Listeners ***
    // *****************



    @Override
    public void postCreateTopic(Topic topic) {
        storeTimestamps(topic);
    }

    @Override
    public void postCreateAssoc(Assoc assoc) {
        storeTimestamps(assoc);
    }

    @Override
    public void postUpdateTopic(Topic topic, TopicModel updateModel, TopicModel oldTopic) {
        storeTimestamp(topic);
    }

    @Override
    public void postUpdateAssoc(Assoc assoc, AssocModel updateModel, AssocModel oldAssoc) {
        storeTimestamp(assoc);
    }

    // ---

    @Override
    public void preSendTopic(Topic topic) {
        enrichWithTimestamp(topic);
    }

    @Override
    public void preSendAssoc(Assoc assoc) {
        enrichWithTimestamp(assoc);
    }

    // ---

    @Override
    public void serviceResponseFilter(ContainerResponse response) {
        DMXObject object = responseObject(response);
        if (object != null) {
            setLastModifiedHeader(response, getModificationTime(object.getId()));
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private void storeTimestamps(DMXObject object) {
        long time = System.currentTimeMillis();
        storeCreationTime(object, time);
        storeModificationTime(object, time);
    }

    private void storeTimestamp(DMXObject object) {
        long time = System.currentTimeMillis();
        storeModificationTime(object, time);
    }

    // ---

    private void storeCreationTime(DMXObject object, long time) {
        storeTime(object, PROP_CREATED, time);
    }

    private void storeModificationTime(DMXObject object, long time) {
        storeTime(object, PROP_MODIFIED, time);
    }

    // ---

    private void storeTime(DMXObject object, String propUri, long time) {
        object.setProperty(propUri, time, true);    // addToIndex=true
    }

    // ===

    // ### FIXME: copy in CachingPlugin
    private DMXObject responseObject(ContainerResponse response) {
        Object entity = response.getEntity();
        return entity instanceof DMXObject ? (DMXObject) entity : null;
    }

    private void enrichWithTimestamp(DMXObject object) {
        long objectId = object.getId();
        ChildTopicsModel childTopics = object.getChildTopics().getModel()
            .set(PROP_CREATED, getCreationTime(objectId))
            .set(PROP_MODIFIED, getModificationTime(objectId));
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
