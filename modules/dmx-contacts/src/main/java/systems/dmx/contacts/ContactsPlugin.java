package systems.dmx.contacts;

import systems.dmx.core.RelatedTopic;
import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.event.PreCreateAssociationListener;
import systems.dmx.core.util.DMXUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import java.util.List;



@Path("/contact")
@Produces("application/json")
public class ContactsPlugin extends PluginActivator implements ContactsService, PreCreateAssociationListener {

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************
    // *** ContactsService Implementation ***
    // **************************************



    @GET
    @Path("/{id}/institutions")
    @Override
    public List<RelatedTopic> getInstitutions(@PathParam("id") long personId) {
        return dmx.getTopic(personId).getRelatedTopics("dmx.contacts.organization_association", "dmx.core.default",
            "dmx.core.default", "dmx.contacts.institution");
    }

    @GET
    @Path("/{id}/persons")
    @Override
    public List<RelatedTopic> getPersons(@PathParam("id") long instId) {
        return dmx.getTopic(instId).getRelatedTopics("dmx.contacts.organization_association", "dmx.core.default",
            "dmx.core.default", "dmx.contacts.person");
    }



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void preCreateAssociation(AssociationModel assoc) {
        // Person <-> Institution
        DMXUtils.associationAutoTyping(assoc, "dmx.contacts.person", "dmx.contacts.institution",
            "dmx.contacts.organization_association", "dmx.core.default", "dmx.core.default", dm4);
    }
}
