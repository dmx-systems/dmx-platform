package de.deepamehta.plugins.time;

import de.deepamehta.plugins.time.service.TimeService;

import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.event.PostCreateAssociationListener;
import de.deepamehta.core.service.event.PostCreateTopicListener;
import de.deepamehta.core.service.event.PostUpdateAssociationListener;
import de.deepamehta.core.service.event.PostUpdateTopicListener;
import de.deepamehta.core.service.event.PreSendResponseListener;

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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
                                                                        PreSendResponseListener {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static String URI_MODIFIED = "dm4.time.modified";

    private static String PROP_CREATED = "created";
    private static String PROP_MODIFIED = "modified";

    private static String HEADER_LAST_MODIFIED = "Last-Modified";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DateFormat rfc2822;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **********************************
    // *** TimeService Implementation ***
    // **********************************



    @Override
    public long getTimeCreated(long objectId) {
        return dms.hasProperty(objectId, PROP_CREATED) ? (Long) dms.getProperty(objectId, PROP_CREATED) : -1;
    }

    @Override
    public long getTimeModified(long objectId) {
        return dms.hasProperty(objectId, PROP_MODIFIED) ? (Long) dms.getProperty(objectId, PROP_MODIFIED) : -1;
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
        storeTimestamps(topic.getId());
    }

    @Override
    public void postCreateAssociation(Association assoc, ClientState clientState, Directives directives) {
        storeTimestamps(assoc.getId());
    }

    @Override
    public void postUpdateTopic(Topic topic, TopicModel newModel, TopicModel oldModel, ClientState clientState,
                                                                                       Directives directives) {
        storeTimestamp(topic.getId());
    }

    @Override
    public void postUpdateAssociation(Association assoc, AssociationModel oldModel, ClientState clientState,
                                                                                    Directives directives) {
        storeTimestamp(assoc.getId());
    }

    // ---

    @Override
    public void preSendResponse(ContainerResponse response) {
        DeepaMehtaObject object = responseObject(response);
        if (object != null) {
            long modified = getTimeModified(object.getId());
            enrichWithTimestamp(object, modified);
            if (modified != -1) {
                setLastModifiedHeader(response, modified);
            }
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private void storeTimestamps(long objectId) {
        long time = System.currentTimeMillis();
        storeTimeCreated(objectId, time);
        storeTimeModified(objectId, time);
    }

    private void storeTimestamp(long objectId) {
        long time = System.currentTimeMillis();
        storeTimeModified(objectId, time);
    }

    // ---

    private void storeTimeCreated(long objectId, long time) {
        storeTime(objectId, PROP_CREATED, time);
    }

    private void storeTimeModified(long objectId, long time) {
        storeTime(objectId, PROP_MODIFIED, time);
    }

    // ---

    private void storeTime(long objectId, String propName, long time) {
        dms.setProperty(objectId, propName, time);
    }

    // ===

    // ### FIXME: copy in CachingPlugin
    private DeepaMehtaObject responseObject(ContainerResponse response) {
        Object entity = response.getEntity();
        return entity instanceof DeepaMehtaObject ? (DeepaMehtaObject) entity : null;
    }

    private void enrichWithTimestamp(DeepaMehtaObject object, long time) {
        object.getCompositeValue().getModel().put(URI_MODIFIED, time);
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
