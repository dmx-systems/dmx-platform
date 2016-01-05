package de.deepamehta.plugins.events;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ResultList;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;

import java.util.logging.Logger;



@Path("/event")
@Consumes("application/json")
@Produces("application/json")
public class EventsPlugin extends PluginActivator implements EventsService {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************************
    // *** EventsService Implementation ***
    // ************************************



    @GET
    @Path("/participant/{id}")
    @Override
    public ResultList<RelatedTopic> getEvents(@PathParam("id") long personId) {
        return dms.getTopic(personId).getRelatedTopics("dm4.events.participant", "dm4.core.default", "dm4.core.default",
            "dm4.events.event");
    }

    @GET
    @Path("/{id}/participants")
    @Override
    public ResultList<RelatedTopic> getParticipants(@PathParam("id") long eventId) {
        return dms.getTopic(eventId).getRelatedTopics("dm4.events.participant", "dm4.core.default", "dm4.core.default",
            "dm4.contacts.person");
    }
}
