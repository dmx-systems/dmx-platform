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
        return dm4.getTopic(personId).getRelatedTopics("dm4.contacts.organization_association", "dm4.core.default",
            "dm4.core.default", "dm4.contacts.institution");
    }

    @GET
    @Path("/{id}/persons")
    @Override
    public List<RelatedTopic> getPersons(@PathParam("id") long instId) {
        return dm4.getTopic(instId).getRelatedTopics("dm4.contacts.organization_association", "dm4.core.default",
            "dm4.core.default", "dm4.contacts.person");
    }



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void preCreateAssociation(AssociationModel assoc) {
        // Person <-> Institution
        DMXUtils.associationAutoTyping(assoc, "dm4.contacts.person", "dm4.contacts.institution",
            "dm4.contacts.organization_association", "dm4.core.default", "dm4.core.default", dm4);
    }
}
