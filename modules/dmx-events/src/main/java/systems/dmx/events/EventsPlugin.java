package systems.dmx.events;

import static systems.dmx.contacts.Constants.*;
import static systems.dmx.core.Constants.*;
import static systems.dmx.events.Constants.*;
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



@Path("/events")
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
        return dmx.getTopic(personId).getRelatedTopics(EVENT_INVOLVEMENT, DEFAULT, DEFAULT, EVENT);
    }

    @GET
    @Path("/{id}/persons")
    @Override
    public List<RelatedTopic> getPersons(@PathParam("id") long eventId) {
        return dmx.getTopic(eventId).getRelatedTopics(EVENT_INVOLVEMENT, DEFAULT, DEFAULT, PERSON);
    }

    // Listeners

    @Override
    public void preCreateAssoc(AssocModel assoc) {
        // Event <-> Person
        DMXUtils.assocAutoTyping(assoc, EVENT, PERSON, EVENT_INVOLVEMENT, DEFAULT, DEFAULT);
        // Event <-> Organization
        DMXUtils.assocAutoTyping(assoc, EVENT, ORGANIZATION, EVENT_INVOLVEMENT, DEFAULT, DEFAULT);
        //
        // Event -> Address
        PlayerModel[] players = DMXUtils.assocAutoTyping(assoc, EVENT, ADDRESS, COMPOSITION, PARENT, CHILD);
        if (players != null) {
            long eventId = players[0].getId();
            Topic event = dmx.getTopic(eventId);
            event.getChildTopics().getTopic(ADDRESS).getRelatingAssoc().delete();
            timestampsService.setModified(event);
        }
    }
}
