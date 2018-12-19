package systems.dmx.events;

import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.model.RoleModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.event.PreCreateAssociationListener;
import systems.dmx.core.util.DMXUtils;
import systems.dmx.timestamps.TimestampsService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;

import java.util.List;
import java.util.logging.Logger;



@Path("/event")
@Consumes("application/json")
@Produces("application/json")
public class EventsPlugin extends PluginActivator implements EventsService, PreCreateAssociationListener {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private TimestampsService timestampsService;

    private static final Logger logger = Logger.getLogger(EventsPlugin.class.getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************************
    // *** EventsService Implementation ***
    // ************************************



    @GET
    @Path("/participant/{id}")
    @Override
    public List<RelatedTopic> getEvents(@PathParam("id") long personId) {
        return dmx.getTopic(personId).getRelatedTopics("dmx.events.participation",
            "dmx.core.default", "dmx.core.default", "dmx.events.event");
    }

    @GET
    @Path("/{id}/participants")
    @Override
    public List<RelatedTopic> getParticipants(@PathParam("id") long eventId) {
        return dmx.getTopic(eventId).getRelatedTopics("dmx.events.participation",
            "dmx.core.default", "dmx.core.default", "dmx.contacts.person");
    }



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void preCreateAssociation(AssociationModel assoc) {
        // Event <-> Person
        DMXUtils.associationAutoTyping(assoc, "dmx.events.event", "dmx.contacts.person",
            "dmx.events.participation", "dmx.core.default", "dmx.core.default");
        //
        // Event -> Address
        RoleModel[] roles = DMXUtils.associationAutoTyping(assoc, "dmx.events.event", "dmx.contacts.address",
            "dmx.core.composition", "dmx.core.parent", "dmx.core.child");
        if (roles != null) {
            long eventId = roles[0].getPlayerId();
            Topic event = dmx.getTopic(eventId);
            event.getChildTopics().getTopic("dmx.contacts.address").getRelatingAssociation().delete();
            timestampsService.setModified(event);
        }
    }
}
