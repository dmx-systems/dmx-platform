package systems.dmx.contacts;

import systems.dmx.core.RelatedTopic;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.event.PreCreateAssocListener;
import systems.dmx.core.util.DMXUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import java.util.List;



@Path("/contacts")
@Produces("application/json")
public class ContactsPlugin extends PluginActivator implements ContactsService, PreCreateAssocListener {

    // -------------------------------------------------------------------------------------------------- Public Methods

    // ContactsService

    @GET
    @Path("/person/{id}/organizations")
    @Override
    public List<RelatedTopic> getOrganizations(@PathParam("id") long personId) {
        return dmx.getTopic(personId).getRelatedTopics("dmx.contacts.organization_involvement",
            "dmx.core.default", "dmx.core.default", "dmx.contacts.organization");
    }

    @GET
    @Path("/organization/{id}/persons")
    @Override
    public List<RelatedTopic> getPersons(@PathParam("id") long organizationId) {
        return dmx.getTopic(organizationId).getRelatedTopics("dmx.contacts.organization_involvement",
            "dmx.core.default", "dmx.core.default", "dmx.contacts.person");
    }

    // Listeners

    @Override
    public void preCreateAssociation(AssocModel assoc) {
        // Person <-> Organization
        DMXUtils.associationAutoTyping(assoc, "dmx.contacts.person", "dmx.contacts.organization",
            "dmx.contacts.organization_involvement", "dmx.core.default", "dmx.core.default");
    }
}
