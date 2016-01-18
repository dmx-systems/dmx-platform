package de.deepamehta.plugins.contacts;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.service.event.PreCreateAssociationListener;
import de.deepamehta.core.util.DeepaMehtaUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;



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
    public ResultList<RelatedTopic> getInstitutions(@PathParam("id") long personId) {
        return dms.getTopic(personId).getRelatedTopics("dm4.contacts.organization_association", "dm4.core.default",
            "dm4.core.default", "dm4.contacts.institution");
    }

    @GET
    @Path("/{id}/persons")
    @Override
    public ResultList<RelatedTopic> getPersons(@PathParam("id") long instId) {
        return dms.getTopic(instId).getRelatedTopics("dm4.contacts.organization_association", "dm4.core.default",
            "dm4.core.default", "dm4.contacts.person");
    }



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void preCreateAssociation(AssociationModel assoc) {
        // Person <-> Institution
        DeepaMehtaUtils.associationAutoTyping(assoc, "dm4.contacts.person", "dm4.contacts.institution",
            "dm4.contacts.organization_association", "dm4.core.default", "dm4.core.default", dms);
    }    
}
