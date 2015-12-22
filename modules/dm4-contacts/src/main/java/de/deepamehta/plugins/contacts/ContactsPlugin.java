package de.deepamehta.plugins.contacts;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ResultList;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;



@Path("/contact")
@Produces("application/json")
public class ContactsPlugin extends PluginActivator implements ContactsService {

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
    public ResultList<RelatedTopic> getPersons(@PathParam("id") long institutionId) {
        return dms.getTopic(institutionId).getRelatedTopics("dm4.contacts.organization_association", "dm4.core.default",
            "dm4.core.default", "dm4.contacts.person");
    }
}
