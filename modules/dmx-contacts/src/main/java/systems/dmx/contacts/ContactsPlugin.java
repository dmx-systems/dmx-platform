package systems.dmx.contacts;

import static systems.dmx.contacts.Constants.*;
import static systems.dmx.core.Constants.*;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.event.PreCreateAssoc;
import systems.dmx.core.util.DMXUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import java.util.List;



@Path("/contacts")
@Produces("application/json")
public class ContactsPlugin extends PluginActivator implements ContactsService, PreCreateAssoc {

    // -------------------------------------------------------------------------------------------------- Public Methods

    // ContactsService

    @GET
    @Path("/person/{id}/organizations")
    @Override
    public List<RelatedTopic> getOrganizations(@PathParam("id") long personId) {
        return dmx.getTopic(personId).getRelatedTopics(ORGANIZATION_INVOLVEMENT, DEFAULT, DEFAULT, ORGANIZATION);
    }

    @GET
    @Path("/organization/{id}/persons")
    @Override
    public List<RelatedTopic> getPersons(@PathParam("id") long organizationId) {
        return dmx.getTopic(organizationId).getRelatedTopics(ORGANIZATION_INVOLVEMENT, DEFAULT, DEFAULT, PERSON);
    }

    // Listeners

    @Override
    public void preCreateAssoc(AssocModel assoc) {
        // Person <-> Organization
        DMXUtils.assocAutoTyping(assoc, PERSON, ORGANIZATION, ORGANIZATION_INVOLVEMENT, DEFAULT, DEFAULT);
    }
}
