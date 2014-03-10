package de.deepamehta.plugins.facets;

import de.deepamehta.plugins.facets.model.FacetValues;
import de.deepamehta.plugins.facets.service.FacetsService;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;

import java.util.List;
import java.util.logging.Logger;



@Path("/facet")
@Consumes("application/json")
@Produces("application/json")
public class FacetsPlugin extends PluginActivator implements FacetsService {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************************
    // *** FacetsService Implementation ***
    // ************************************



    @GET
    @Path("/{facet_type_uri}/topic/{id}")
    @Override
    public Topic getFacet(@PathParam("id") long topicId, @PathParam("facet_type_uri") String facetTypeUri) {
        return getFacet(dms.getTopic(topicId, false), facetTypeUri);        // fetchComposite=false
    }

    @Override
    public Topic getFacet(DeepaMehtaObject object, String facetTypeUri) {
        // ### TODO: integrity check: is the object an instance of that facet type?
        return fetchChildTopic(object, getAssocDef(facetTypeUri), true);    // fetchComposite=true
    }

    // ---

    @GET
    @Path("/multi/{facet_type_uri}/topic/{id}")
    @Override
    public List<RelatedTopic> getFacets(@PathParam("id") long topicId, @PathParam("facet_type_uri") String facetTypeUri)
                                                                                                                {
        return getFacets(dms.getTopic(topicId, false), facetTypeUri);       // fetchComposite=false
    }

    @Override
    public List<RelatedTopic> getFacets(DeepaMehtaObject object, String facetTypeUri) {
        // ### TODO: integrity check: is the object an instance of that facet type?
        return fetchChildTopics(object, getAssocDef(facetTypeUri), true);   // fetchComposite=true
    }

    // ---

    @POST
    @Path("/{facet_type_uri}/topic/{id}")
    @Override
    public void addFacetTypeToTopic(@PathParam("id") long topicId, @PathParam("facet_type_uri") String facetTypeUri) {
        dms.createAssociation(new AssociationModel("dm4.core.instantiation",
            new TopicRoleModel(topicId,      "dm4.core.instance"),
            new TopicRoleModel(facetTypeUri, "dm4.facets.facet")), null);   // clientState=null
    }

    // ---

    @PUT
    @Path("/{facet_type_uri}/topic/{id}")
    @Override
    public void updateFacet(@PathParam("id") long topicId, @PathParam("facet_type_uri") String facetTypeUri,
                                     TopicModel facetValue, @HeaderParam("Cookie") ClientState clientState) {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            updateFacet(dms.getTopic(topicId, false), facetTypeUri, facetValue, clientState, new Directives());
            //
            tx.success();
        } catch (Exception e) {
            logger.warning("ROLLBACK!");
            throw new RuntimeException("Updating facet \"" + facetTypeUri + "\" of topic " + topicId +
                " failed (facetValue=" + facetValue + ")", e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public void updateFacet(DeepaMehtaObject object, String facetTypeUri, TopicModel facetValue,
                                                     ClientState clientState, Directives directives) {
        object.updateChildTopic(facetValue, getAssocDef(facetTypeUri), clientState, directives);
    }

    // ---

    @PUT
    @Path("/multi/{facet_type_uri}/topic/{id}")
    @Override
    public void updateFacets(@PathParam("id") long topicId, @PathParam("facet_type_uri") String facetTypeUri,
                                   FacetValues facetValues, @HeaderParam("Cookie") ClientState clientState) {
        updateFacets(dms.getTopic(topicId, false), facetTypeUri, facetValues, clientState, new Directives());
    }

    @Override
    public void updateFacets(DeepaMehtaObject object, String facetTypeUri, FacetValues facetValues,
                                                      ClientState clientState, Directives directives) {
        object.updateChildTopics(facetValues.getValues(), getAssocDef(facetTypeUri), clientState, directives);
    }

    // ---

    // Note: there is a similar private method in AttachedDeepaMehtaObject:
    // fetchChildTopic(AssociationDefinition assocDef, long childTopicId, boolean fetchComposite)
    // ### TODO: Extend DeepaMehtaObject interface by hasChildTopic()?
    @Override
    public boolean hasFacet(long topicId, String facetTypeUri, long facetTopicId) {
        String assocTypeUri = getAssocDef(facetTypeUri).getInstanceLevelAssocTypeUri();
        Association assoc = dms.getAssociation(assocTypeUri, topicId, facetTopicId, "dm4.core.parent", "dm4.core.child",
            false);     // fetchComposite=false
        return assoc != null;
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Fetches and returns a child topic or <code>null</code> if no such topic extists.
     * <p>
     * Note: There is a principal copy in AttachedDeepaMehtaObject but here the precondition is different:
     * The given association definition must not necessarily originate from the given object's type definition.
     * ### TODO: meanwhile we have the ValueStorage. Can we use its method instead?
     */
    private RelatedTopic fetchChildTopic(DeepaMehtaObject object, AssociationDefinition assocDef,
                                                                  boolean fetchComposite) {
        String assocTypeUri  = assocDef.getInstanceLevelAssocTypeUri();
        String othersTypeUri = assocDef.getChildTypeUri();
        return object.getRelatedTopic(assocTypeUri, "dm4.core.parent", "dm4.core.child", othersTypeUri, fetchComposite,
            false);
    }

    /**
     * Fetches and returns child topics.
     * <p>
     * Note: There is a principal copy in AttachedDeepaMehtaObject but here the precondition is different:
     * The given association definition must not necessarily originate from the given object's type definition.
     * ### TODO: meanwhile we have the ValueStorage. Can we use its method instead?
     */
    private List<RelatedTopic> fetchChildTopics(DeepaMehtaObject object, AssociationDefinition assocDef,
                                                                         boolean fetchComposite) {
        String assocTypeUri  = assocDef.getInstanceLevelAssocTypeUri();
        String othersTypeUri = assocDef.getChildTypeUri();
        return object.getRelatedTopics(assocTypeUri, "dm4.core.parent", "dm4.core.child", othersTypeUri, fetchComposite,
            false, 0).getItems();
    }

    // ---

    private AssociationDefinition getAssocDef(String facetTypeUri) {
        // Note: a facet type has exactly *one* association definition
        return dms.getTopicType(facetTypeUri).getAssocDefs().iterator().next();
    }
}
