package de.deepamehta.plugins.events;

// import de.deepamehta.core.Association;
// import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
// import de.deepamehta.core.Role;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.osgi.PluginActivator;
// import de.deepamehta.core.service.event.PostCreateAssociationListener;
import de.deepamehta.core.service.event.PreCreateAssociationListener;
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
public class EventsPlugin extends PluginActivator implements EventsService, PreCreateAssociationListener {

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
            dms.getTopic(eventId).getChildTopics().getTopic("dm4.contacts.address").getRelatingAssociation().delete();
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

    /* private Role[] getRoles(Association assoc, String topicTypeUri1, String topicTypeUri2) {
        Role r1 = assoc.getRole1();
        Role r2 = assoc.getRole2();
        DeepaMehtaObject p1 = r1.getPlayer();
        DeepaMehtaObject p2 = r2.getPlayer();
        Role role1 = getRole(r1, r2, p1, p2, topicTypeUri1);
        Role role2 = getRole(r1, r2, p1, p2, topicTypeUri2);
        if (role1 != null && role2 != null) {
            return new Role[] {role1, role2};
        }
        return null;
    }

    private Role getRole(Role r1, Role r2, DeepaMehtaObject p1, DeepaMehtaObject p2, String topicTypeUri) {
        boolean m1 = p1.getTypeUri().equals(topicTypeUri);
        boolean m2 = p2.getTypeUri().equals(topicTypeUri);
        if (m1 && m2) {
            throw new RuntimeException("Ambiguity in association: both topics are of type \"" + topicTypeUri + "\"");
        }
        return m1 ? r1 : m2 ? r2 : null;
    } */
}
