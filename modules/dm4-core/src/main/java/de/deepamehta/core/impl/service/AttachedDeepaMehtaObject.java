package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.util.JavaUtils;
import de.deepamehta.core.service.ChangeReport;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;

import org.codehaus.jettison.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;



abstract class AttachedDeepaMehtaObject implements DeepaMehtaObject {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String LABEL_SEPARATOR = " ";
    private static final String REF_PREFIX = "ref_id:";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DeepaMehtaObjectModel model;
    protected final EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedDeepaMehtaObject(EmbeddedService dms) {
        this.model = null;      // ### initialized through fetch().
        this.dms = dms;
    }

    AttachedDeepaMehtaObject(DeepaMehtaObjectModel model, EmbeddedService dms) {
        // set default values
        if (model.getUri() == null) {
            model.setUri("");
        }
        if (model.getSimpleValue() == null) {
            model.setSimpleValue("");
        }
        //
        this.model = model;
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ***************************************
    // *** DeepaMehtaObject Implementation ***
    // ***************************************



    // === Model ===

    // --- ID ---

    @Override
    public long getId() {
        return model.getId();
    }

    // --- URI ---

    @Override
    public String getUri() {
        return model.getUri();
    }

    @Override
    public void setUri(String uri) {
        // update memory
        model.setUri(uri);
        // update DB
        storeUri(uri);      // abstract
    }

    // --- Type URI ---

    @Override
    public String getTypeUri() {
        return model.getTypeUri();
    }

    @Override
    public void setTypeUri(String typeUri) {
        // update memory
        model.setTypeUri(typeUri);
        // update DB
        storeTypeUri();     // abstract
    }

    // --- Simple Value ---

    @Override
    public SimpleValue getSimpleValue() {
        return model.getSimpleValue();
    }

    // ---

    @Override
    public void setSimpleValue(String value) {
        setSimpleValue(new SimpleValue(value));
    }

    @Override
    public void setSimpleValue(int value) {
        setSimpleValue(new SimpleValue(value));
    }

    @Override
    public void setSimpleValue(long value) {
        setSimpleValue(new SimpleValue(value));
    }

    @Override
    public void setSimpleValue(boolean value) {
        setSimpleValue(new SimpleValue(value));
    }

    @Override
    public void setSimpleValue(SimpleValue value) {
        if (value == null) {
            throw new IllegalArgumentException("Tried to set a null SimpleValue (" + this + ")");
        }
        // update memory
        model.setSimpleValue(value);
        // update DB
        storeAndIndexValue(value);
    }

    // --- Composite Value ---

    @Override
    public CompositeValue getCompositeValue() {
        return model.getCompositeValue();
    }

    @Override
    public void setCompositeValue(CompositeValue comp, ClientState clientState, Directives directives) {
        updateCompositeValue(comp, clientState, directives);
        refreshLabel();
    }



    // === Traversal ===

    @Override
    public SimpleValue getChildTopicValue(String assocDefUri) {
        return fetchChildTopicValue(getAssocDef(assocDefUri));
    }

    @Override
    public void setChildTopicValue(String assocDefUri, SimpleValue value) {
        // update memory
        getCompositeValue().put(assocDefUri, value.value());
        // update DB
        storeChildTopicValue(assocDefUri, value);
        //
        refreshLabel();
    }

    // --- Topic Retrieval ---

    @Override
    public ResultSet<RelatedTopic> getRelatedTopics(String assocTypeUri, int maxResultSize, ClientState clientState) {
        return getRelatedTopics(assocTypeUri, null, null, null, false, false, maxResultSize, clientState);
    }

    @Override
    public AttachedRelatedTopic getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                String othersTopicTypeUri, boolean fetchComposite,
                                                boolean fetchRelatingComposite, ClientState clientState) {
        ResultSet<RelatedTopic> topics = getRelatedTopics(assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri, fetchComposite, fetchRelatingComposite, 0, clientState);
        switch (topics.getSize()) {
        case 0:
            return null;
        case 1:
            return (AttachedRelatedTopic) topics.getIterator().next();
        default:
            throw new RuntimeException("Ambiguity: there are " + topics.getSize() + " related topics (object ID=" +
                getId() + ", assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri + "\", " +
                "othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersTopicTypeUri=\"" + othersTopicTypeUri + "\")");
        }
    }

    @Override
    public ResultSet<RelatedTopic> getRelatedTopics(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                    String othersTopicTypeUri, boolean fetchComposite, boolean fetchRelatingComposite,
                                    int maxResultSize, ClientState clientState) {
        List assocTypeUris = assocTypeUri != null ? Arrays.asList(assocTypeUri) : null;
        return getRelatedTopics(assocTypeUris, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri,
            fetchComposite, fetchRelatingComposite, maxResultSize, clientState);
    }

    // --- Association Retrieval ---

    @Override
    public Set<Association> getAssociations() {
        return getAssociations(null);
    }



    // === Deletion ===

    /**
     * Deletes all sub-topics of this DeepaMehta object (associated via "dm4.core.composition", recursively) and
     * deletes all the remaining direct associations of this DeepaMehta object.
     * <p>
     * Note: deletion of the object itself is up to the subclasses.
     */
    @Override
    public void delete(Directives directives) {
        // 1) recursively delete sub-topics
        ResultSet<RelatedTopic> partTopics = getRelatedTopics("dm4.core.composition",
            "dm4.core.whole", "dm4.core.part", null, false, false, 0, null);
        for (Topic partTopic : partTopics) {
            partTopic.delete(directives);
        }
        // 2) delete direct associations
        for (Association assoc : getAssociations()) {
            try {
                assoc.delete(directives);
            } catch (IllegalStateException e) {
                // Note: this can happen in a particular situation and is no problem: let A1 and A2 be direct
                // associations of this DeepaMehta object and let A2 point to A1. If A1 gets deleted first
                // (the association set order is non-deterministic), A2 is implicitely deleted with it
                // (because it is a direct association of A1 as well). Then when the loop comes to A2
                // "IllegalStateException: Node[1327] has been deleted in this tx" is thrown because A2
                // has been deleted already. (The Node appearing in the exception is the auxiliary node of A2.)
                // If, on the other hand, A2 gets deleted first no error would occur.
                //
                // This particular situation exists when e.g. a topicmap is deleted while one of its mapcontext
                // associations is also a part of the topicmap itself. This originates e.g. when the user reveals
                // a topicmap's mapcontext association and then deletes the topicmap.
                //
                if (e.getMessage().matches("Node\\[\\d+\\] has been deleted in this tx")) {
                    logger.info("### Association " + assoc.getId() + " has already been deleted in this transaction. " +
                        "This can happen while deleting a topic with direct associations A1 and A2 while A2 points " +
                        "to A1");
                } else {
                    throw e;
                }
            }
        }
    }



    // **********************************
    // *** JSONEnabled Implementation ***
    // **********************************



    @Override
    public JSONObject toJSON() {
        return model.toJSON();
    }



    // ****************
    // *** Java API ***
    // ****************



    @Override
    public boolean equals(Object o) {
        return ((AttachedDeepaMehtaObject) o).model.equals(model);
    }

    @Override
    public int hashCode() {
        return model.hashCode();
    }

    @Override
    public String toString() {
        return model.toString();
    }



    // ----------------------------------------------------------------------------------------------- Protected Methods

    // ### This is supposed to be protected, but doesn't compile!
    // ### It is called from the subclasses constructors, but on a differnt TopicBase instance.
    // ### See de.deepamehta.core.impl.storage.MGTopic and de.deepamehta.core.impl.service.AttachedTopic.
    public DeepaMehtaObjectModel getModel() {
        return model;
    }

    protected final void setModel(DeepaMehtaObjectModel model) {
        this.model = model;
    }

    // ---

    protected abstract String className();

    protected abstract void storeUri(String uri);

    protected abstract void storeTypeUri();

    protected abstract SimpleValue storeValue(SimpleValue value);

    protected abstract void indexValue(IndexMode indexMode, String indexKey, SimpleValue value, SimpleValue oldValue);

    protected abstract Type getType();

    protected abstract RoleModel getRoleModel(String roleTypeUri);



    // ----------------------------------------------------------------------------------------- Package Private Methods

    void store(ClientState clientState, Directives directives) {
        if (getType().getDataTypeUri().equals("dm4.core.composite")) {
            CompositeValue comp = getCompositeValue();
            model.setCompositeValue(new CompositeValue());
            updateCompositeValue(comp, clientState, directives);
            refreshLabel();
        } else {
            storeAndIndexValue(getSimpleValue());
        }
    }

    ChangeReport update(DeepaMehtaObjectModel model, ClientState clientState, Directives directives) {
        ChangeReport report = new ChangeReport();
        updateUri(model.getUri());
        updateTypeUri(model.getTypeUri(), report);
        // ### TODO: compare new model with current one and update only if changed.
        if (getType().getDataTypeUri().equals("dm4.core.composite")) {
            updateCompositeValue(model.getCompositeValue(), clientState, directives);
            refreshLabel();
        } else {
            updateValue(model.getSimpleValue());
        }
        //
        return report;
    }

    /**
     * Called from {@link EmbeddedService#attach} (indirectly)
     */
    void loadComposite() {
        // fetch from DB
        CompositeValue comp = fetchComposite();
        // update memory
        model.setCompositeValue(comp);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    // === Fetch ===

    private CompositeValue fetchComposite() {
        try {
            CompositeValue comp = new CompositeValue();
            for (AssociationDefinition assocDef : getType().getAssocDefs().values()) {
                AttachedTopic childTopic = fetchChildTopic(assocDef, true);     // fetchComposite=true
                if (childTopic != null) {
                    comp.put(assocDef.getUri(), childTopic.getModel());
                }
            }
            return comp;
        } catch (Exception e) {
            throw new RuntimeException("Fetching the " + className() + "'s composite failed (" + this + ")", e);
        }
    }

    private SimpleValue fetchChildTopicValue(AssociationDefinition assocDef) {
        Topic childTopic = fetchChildTopic(assocDef, false);                    // fetchComposite=false
        if (childTopic != null) {
            return childTopic.getSimpleValue();
        }
        return null;
    }

    // === Store ===

    private void updateCompositeValue(CompositeValue newComp, ClientState clientState, Directives directives) {
        try {
            for (AssociationDefinition assocDef : getType().getAssocDefs().values()) {
                TopicModel valueTopic = newComp.getTopic(assocDef.getUri(), null);    // defaultValue=null
                // skip if not contained in update request
                if (valueTopic == null) {
                    continue;
                }
                updateCompositeValue(assocDef, valueTopic, clientState, directives);
            }
        } catch (Exception e) {
            throw new RuntimeException("Updating the composite value of " + className() + " " + getId() +
                " failed (newComp=" + newComp + ")", e);
        }
    }

    // ### FIXME: Remove from interface. Make it private.
    @Override
    public void updateCompositeValue(AssociationDefinition assocDef, TopicModel valueTopic, ClientState clientState,
                                                                                            Directives directives) {
        CompositeValue comp = getCompositeValue();
        //
        String assocDefUri       = assocDef.getUri();
        String assocTypeUri      = assocDef.getTypeUri();
        String childTopicTypeUri = assocDef.getPartTopicTypeUri();
        TopicType childTopicType = dms.getTopicType(childTopicTypeUri, null);
        // Note: the type URI of a simplified topic model (as constructed
        // from update requests) is not initialzed.
        valueTopic.setTypeUri(childTopicTypeUri);
        //
        if (assocTypeUri.equals("dm4.core.composition_def")) {
            // Note: the child topic's composite must be fetched. It needs to be passed to the
            // POST_UPDATE_TOPIC hook as part of the "old model" (when the child topic is updated).
            Topic childTopic = fetchChildTopic(assocDef, true);             // fetchComposite=true
            if (childTopic != null) {
                // update existing child
                childTopic.update(valueTopic, clientState, directives);
            } else {
                // create new child
                childTopic = dms.createTopic(valueTopic, null);
                associateChildTopic(assocDef, childTopic.getId());
                // Note: the child topic must be created right with its composite value.
                // Otherwise its label can't be calculated. ### still true?
            }
            // update memory
            comp.put(assocDefUri, childTopic.getModel());
        } else if (assocTypeUri.equals("dm4.core.aggregation_def")) {
            if (childTopicType.getDataTypeUri().equals("dm4.core.composite")) {
                throw new RuntimeException("Aggregation of composite topic types not yet supported");
            } else {
                // remove current assignment
                RelatedTopic childTopic = fetchChildTopic(assocDef, false);     // fetchComposite=false
                if (childTopic != null) {
                    long assocId = childTopic.getAssociation().getId();
                    dms.deleteAssociation(assocId, null);  // clientState=null
                }
                //
                String value = valueTopic.getSimpleValue().toString();
                boolean assignExistingTopic = value.startsWith(REF_PREFIX);
                if (assignExistingTopic) {
                    // update DB
                    long childTopicId = Long.parseLong(value.substring(REF_PREFIX.length()));
                    associateChildTopic(assocDef, childTopicId);
                    // update memory
                    // Topic assignedTopic = dms.getTopic(childTopicId, false, null);  // fetchComposite=false
                    // comp.put(assocDefUri, assignedTopic.getSimpleValue().value());
                    SimpleValue childTopicValue = fetchChildTopicValue(assocDef);
                    comp.put(assocDefUri, childTopicValue.value());
                } else {
                    // create new child
                    Topic _childTopic = dms.createTopic(valueTopic, null);
                    associateChildTopic(assocDef, _childTopic.getId());
                    // update memory
                    comp.put(assocDefUri, _childTopic.getModel());
                }
            }
        } else {
            throw new RuntimeException("Association type \"" + assocTypeUri + "\" not supported");
        }
    }

    /**
     * Stores a child's topic value in the database. If the child topic does not exist it is created.
     *
     * @param   assocDefUri     The "axis" that leads to the child: the URI of an {@link AssociationDefinition}.
     * @param   value           The value to set. If <code>null</code> nothing is set. The child topic is potentially
     *                          created and returned anyway.
     *
     * @return  The child topic.
     */
    private Topic storeChildTopicValue(String assocDefUri, final SimpleValue value) {
        try {
            AssociationDefinition assocDef = getAssocDef(assocDefUri);
            Topic childTopic = fetchChildTopic(assocDef, false);    // fetchComposite=false
            if (childTopic != null) {
                if (value != null) {
                    childTopic.setSimpleValue(value);
                }
            } else {
                // create child topic
                String topicTypeUri = assocDef.getPartTopicTypeUri();
                childTopic = dms.createTopic(new TopicModel(topicTypeUri, value), null);
                // associate child topic
                associateChildTopic(assocDef, childTopic.getId());
            }
            return childTopic;
        } catch (Exception e) {
            throw new RuntimeException("Storing child topic value failed (parentTopic=" + this +
                ",\nassocDefUri=" + assocDefUri + ",\nvalue=\"" + value + "\")", e);
        }
    }

    private void storeAndIndexValue(SimpleValue value) {
        SimpleValue oldValue = storeValue(value);               // abstract
        indexValue(value, oldValue);
    }

    private void indexValue(SimpleValue value, SimpleValue oldValue) {
        Type type = getType();
        String indexKey = type.getUri();
        // strip HTML tags before indexing
        if (type.getDataTypeUri().equals("dm4.core.html")) {
            value = new SimpleValue(JavaUtils.stripHTML(value.toString()));
            if (oldValue != null) {
                oldValue = new SimpleValue(JavaUtils.stripHTML(oldValue.toString()));
            }
        }
        //
        for (IndexMode indexMode : type.getIndexModes()) {
            indexValue(indexMode, indexKey, value, oldValue);   // abstract
        }
    }

    // === Label ===

    /**
     * Prerequsite: this is a composite object.
     */
    private void refreshLabel() {
        try {
            String label;
            // does the type have a label configuration?
            if (getType().getLabelConfig().size() > 0) {
                label = buildLabel();
            } else {
                label = buildDefaultLabel();
            }
            //
            setSimpleValue(label);
        } catch (Exception e) {
            throw new RuntimeException("Refreshing the " + className() + "'s label failed", e);
        }
    }

    /**
     * Builds this object's label according to its type's label configuration.
     */
    private String buildLabel() {
        Type type = getType();
        if (type.getDataTypeUri().equals("dm4.core.composite")) {
            StringBuilder label = new StringBuilder();
            for (String assocDefUri : type.getLabelConfig()) {
                AttachedDeepaMehtaObject childTopic = fetchChildTopic(assocDefUri, false);  // fetchComposite=false
                // Note: topics just created have no child topics yet
                if (childTopic != null) {
                    String l = childTopic.buildLabel();
                    // add separator
                    if (label.length() > 0 && l.length() > 0) {
                        label.append(LABEL_SEPARATOR);
                    }
                    //
                    label.append(l);
                }
            }
            return label.toString();
        } else {
            return getSimpleValue().toString();
        }
    }

    private String buildDefaultLabel() {
        Type type = getType();
        if (type.getDataTypeUri().equals("dm4.core.composite")) {
            Iterator<AssociationDefinition> i = type.getAssocDefs().values().iterator();
            // Note: types just created might have no child types yet
            if (i.hasNext()) {
                AssociationDefinition assocDef = i.next();
                AttachedDeepaMehtaObject childTopic = fetchChildTopic(assocDef, false);     // fetchComposite=false
                // Note: topics just created have no child topics yet
                if (childTopic != null) {
                    return childTopic.buildDefaultLabel();
                }
            }
            return "";
        } else {
            return getSimpleValue().toString();
        }
    }

    // === Update ===

    private void updateUri(String newUri) {
        if (newUri != null) {
            String uri = getUri();
            if (!uri.equals(newUri)) {
                logger.info("### Changing URI from \"" + uri + "\" -> \"" + newUri + "\"");
                setUri(newUri);
            }
        }
    }

    private void updateTypeUri(String newTypeUri, ChangeReport report) {
        if (newTypeUri != null) {
            String typeUri = getTypeUri();
            if (!typeUri.equals(newTypeUri)) {
                logger.info("### Changing type URI from \"" + typeUri + "\" -> \"" + newTypeUri + "\"");
                report.typeUriChanged(typeUri, newTypeUri);
                setTypeUri(newTypeUri);
            }
        }
    }

    private void updateValue(SimpleValue newValue) {
        if (newValue != null) {
            SimpleValue value = getSimpleValue();
            if (!value.equals(newValue)) {
                logger.info("### Changing simple value from \"" + value + "\" -> \"" + newValue + "\"");
                setSimpleValue(newValue);
            }
        }
    }

    // === Helper ===

    /**
     * Fetches and returns a child topic or <code>null</code> if no such topic extists.
     */
    private AttachedRelatedTopic fetchChildTopic(String assocDefUri, boolean fetchComposite) {
        return fetchChildTopic(getAssocDef(assocDefUri), fetchComposite);
    }

    /**
     * Fetches and returns a child topic or <code>null</code> if no such topic extists.
     */
    private AttachedRelatedTopic fetchChildTopic(AssociationDefinition assocDef, boolean fetchComposite) {
        String assocTypeUri       = assocDef.getInstanceLevelAssocTypeUri();
        String myRoleTypeUri      = assocDef.getWholeRoleTypeUri();
        String othersRoleTypeUri  = assocDef.getPartRoleTypeUri();
        String othersTopicTypeUri = assocDef.getPartTopicTypeUri();
        //
        return getRelatedTopic(assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri,
            fetchComposite, false, null);
    }

    // ---

    private void associateChildTopic(AssociationDefinition assocDef, long childTopicId) {
        dms.createAssociation(assocDef.getInstanceLevelAssocTypeUri(),
            getRoleModel(assocDef.getWholeRoleTypeUri()),
            new TopicRoleModel(childTopicId, assocDef.getPartRoleTypeUri()));
    }

    // ---

    private AssociationDefinition getAssocDef(String assocDefUri) {
        return getType().getAssocDef(assocDefUri);
    }
}
