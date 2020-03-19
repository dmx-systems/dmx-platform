package systems.dmx.events;

import static systems.dmx.core.Constants.*;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.PlayerModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.event.PreCreateAssoc;
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
public class EventsPlugin extends PluginActivator implements EventsService, PreCreateAssoc {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private TimestampsService timestampsService;

    private static final Logger logger = Logger.getLogger(EventsPlugin.class.getName());

    // -------------------------------------------------------------------------------------------------- Public Methods

    // EventsService

    @GET
    @Path("/person/{id}")
    @Override
    public List<RelatedTopic> getEvents(@PathParam("id") long personId) {
        return dmx.getTopic(personId).getRelatedTopics("dmx.events.event_involvement",
            DEFAULT, DEFAULT, "dmx.events.event");
    }

    @GET
    @Path("/{id}/persons")
    @Override
    public List<RelatedTopic> getPersons(@PathParam("id") long eventId) {
        return dmx.getTopic(eventId).getRelatedTopics("dmx.events.event_involvement",
            DEFAULT, DEFAULT, "dmx.contacts.person");
    }

    // Listeners

    @Override
    public void preCreateAssoc(AssocModel assoc) {
        // Event <-> Person
        DMXUtils.associationAutoTyping(assoc, "dmx.events.event", "dmx.contacts.person",
            "dmx.events.event_involvement", DEFAULT, DEFAULT);
        // Event <-> Organization
        DMXUtils.associationAutoTyping(assoc, "dmx.events.event", "dmx.contacts.organization",
            "dmx.events.event_involvement", DEFAULT, DEFAULT);
        //
        // Event -> Address
        PlayerModel[] players = DMXUtils.associationAutoTyping(assoc, "dmx.events.event", "dmx.contacts.address",
            COMPOSITION, PARENT, CHILD);
        if (players != null) {
            long eventId = players[0].getId();
            Topic event = dmx.getTopic(eventId);
            event.getChildTopics().getTopic("dmx.contacts.address").getRelatingAssoc().delete();
            timestampsService.setModified(event);
        }
    }
}
