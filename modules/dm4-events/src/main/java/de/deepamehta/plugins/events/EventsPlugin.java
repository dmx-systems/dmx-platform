package de.deepamehta.plugins.events;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.service.event.PreCreateAssociationListener;
import de.deepamehta.plugins.time.TimeService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;

import java.util.logging.Logger;



@Path("/event")
@Consumes("application/json")
@Produces("application/json")
public class EventsPlugin extends PluginActivator implements EventsService, PreCreateAssociationListener {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject
    private TimeService timeService;

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



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void preCreateAssociation(AssociationModel assoc) {
        if (!assoc.getTypeUri().equals("dm4.core.association")) {
            return;
        }
        RoleModel[] roles = getRoleModels(assoc, "dm4.events.event", "dm4.contacts.address");
        if (roles != null) {
            logger.info("############################################################################################");
            assoc.setTypeUri("dm4.core.aggregation");
            roles[0].setRoleTypeUri("dm4.core.parent");
            roles[1].setRoleTypeUri("dm4.core.child");
            //
            long eventId = roles[0].getPlayerId();
            Topic event = dms.getTopic(eventId);
            event.getChildTopics().getTopic("dm4.contacts.address").getRelatingAssociation().delete();
            timeService.setModified(event);
        }
    }    

    // ------------------------------------------------------------------------------------------------- Private Methods

    private RoleModel[] getRoleModels(AssociationModel assoc, String topicTypeUri1, String topicTypeUri2) {
        RoleModel rm1 = assoc.getRoleModel1();
        RoleModel rm2 = assoc.getRoleModel2();
        String t1 = (String) dms.getProperty(rm1.getPlayerId(), "type_uri");
        String t2 = (String) dms.getProperty(rm2.getPlayerId(), "type_uri");
        RoleModel roleModel1 = getRoleModel(rm1, rm2, t1, t2, topicTypeUri1, 1);
        RoleModel roleModel2 = getRoleModel(rm1, rm2, t1, t2, topicTypeUri2, 2);
        if (roleModel1 != null && roleModel2 != null) {
            return new RoleModel[] {roleModel1, roleModel2};
        }
        return null;
    }

    private RoleModel getRoleModel(RoleModel rm1, RoleModel rm2, String t1, String t2, String topicTypeUri, int nr) {
        boolean m1 = t1.equals(topicTypeUri);
        boolean m2 = t2.equals(topicTypeUri);
        if (m1 && m2) {
            return nr == 1 ? rm1 : rm2;
        }
        return m1 ? rm1 : m2 ? rm2 : null;
    }
}
