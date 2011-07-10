package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.RoleModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;



abstract class AttachedDeepaMehtaObject implements DeepaMehtaObject {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DeepaMehtaObjectModel model;
    protected final EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedDeepaMehtaObject(EmbeddedService dms) {
        this(null, dms);    // ### The model remain uninitialized.
                            // ### They are initialized later on through fetch().
    }

    AttachedDeepaMehtaObject(DeepaMehtaObjectModel model, EmbeddedService dms) {
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
        storeUri(uri);
    }

    // --- Value ---

    @Override
    public TopicValue getValue() {
        return model.getValue();
    }

    // ---

    @Override
    public void setValue(String value) {
        setValue(new TopicValue(value));
    }

    @Override
    public void setValue(int value) {
        setValue(new TopicValue(value));
    }

    @Override
    public void setValue(long value) {
        setValue(new TopicValue(value));
    }

    @Override
    public void setValue(boolean value) {
        setValue(new TopicValue(value));
    }

    @Override
    public void setValue(TopicValue value) {
        // update memory
        model.setValue(value);
        // update DB
        storeTopicValue(value);
    }

    // --- Type URI ---

    @Override
    public String getTypeUri() {
        return model.getTypeUri();
    }

    @Override
    public void setTypeUri(String assocTypeUri) {
        // update memory
        model.setTypeUri(assocTypeUri);
        // Note: updating the DB is up to the subclasses
    }

    // --- Composite ---

    @Override
    public Composite getComposite() {
        return model.getComposite();
    }

    @Override
    public void setComposite(Composite comp) {
        // update memory
        model.setComposite(comp);
        // update DB
        storeComposite(comp);
    }


    // === Traversal ===

    @Override
    public TopicValue getChildTopicValue(String assocDefUri) {
        return fetchChildTopicValue(getAssocDef(assocDefUri));
    }

    @Override
    public void setChildTopicValue(String assocDefUri, TopicValue value) {
        Composite comp = getComposite();
        // update memory
        comp.put(assocDefUri, value.value());
        // update DB
        storeChildTopicValue(getAssocDef(assocDefUri), value);
        //
        updateValue(comp);
    }

    // --- Topic Retrieval ---

    @Override
    public Set<RelatedTopic> getRelatedTopics(String assocTypeUri) {
        return getRelatedTopics(assocTypeUri, null, null, null, false, false);   // fetchComposite=false
    }

    @Override
    public AttachedRelatedTopic getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                    String othersTopicTypeUri, boolean fetchComposite, boolean fetchRelatingComposite) {
        Set<RelatedTopic> topics = getRelatedTopics(assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri,
            fetchComposite, fetchRelatingComposite);
        switch (topics.size()) {
        case 0:
            return null;
        case 1:
            return (AttachedRelatedTopic) topics.iterator().next();
        default:
            throw new RuntimeException("Ambiguity: there are " + topics.size() + " related topics (object ID=" +
                getId() + ", assocTypeUri=\"" + assocTypeUri + "\", myRoleTypeUri=\"" + myRoleTypeUri + "\", " +
                "othersRoleTypeUri=\"" + othersRoleTypeUri + "\", othersTopicTypeUri=\"" + othersTopicTypeUri + "\")");
        }
    }

    @Override
    public Set<RelatedTopic> getRelatedTopics(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                    String othersTopicTypeUri, boolean fetchComposite, boolean fetchRelatingComposite) {
        List assocTypeUris = assocTypeUri != null ? Arrays.asList(assocTypeUri) : null;
        return getRelatedTopics(assocTypeUris, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri,
            fetchComposite, fetchRelatingComposite);
    }

    // --- Association Retrieval ---

    @Override
    public Set<Association> getAssociations() {
        return getAssociations(null);
    }



    // === Deletion ===

    /**
     * Deletes all sub-topics of this DeepaMehta object (associated via "dm3.core.composition", recursively) and
     * deletes all the remaining direct associations of this DeepaMehta object.
     * <p>
     * Note: deletion of the object itself is up to the subclasses.
     */
    @Override
    public void delete() {
        // 1) recursively delete sub-topics
        Set<RelatedTopic> partTopics = getRelatedTopics("dm3.core.composition",
            "dm3.core.whole", "dm3.core.part", null, false, false);
        for (Topic partTopic : partTopics) {
            partTopic.delete();
        }
        // 2) delete direct associations
        //
        // Note: with each loop we refetch the whole association set. This is a simple (but ineffective) workaround
        // to deal with a particular situation: let A1 and A2 be direct associations of this DeepaMehta object and
        // let A2 point to A1. If A1 gets deleted first (the association set order is nondeterministic), A2 is
        // implicitely deleted with it (because it is a direct association of A1 as well). Then when the loop comes
        // to A2 "IllegalStateException: Node[1327] has been deleted in this tx" is thrown because A2 has been deleted
        // already. (The Node appearing in the exception is the auxiliary node of A2.) If on the other hand A2 gets
        // deleted first no error would occur.
        //
        // This particular situation exists when e.g. a topicmap is deleted while one of its mapcontext associations
        // is also a part of the topicmap itself. This originates e.g. when the user reveals a topicmap's mapcontext
        // association and then deletes the topicmap.
        //
        // This workaround constantly creates new iterations and processes just their first element.
        Set<Association> assocs;
        while (!(assocs = getAssociations()).isEmpty()) {
            assocs.iterator().next().delete();
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

    protected abstract void storeUri(String uri);

    protected abstract TopicValue storeValue(TopicValue value);

    protected abstract void indexValue(IndexMode indexMode, String indexKey, TopicValue value, TopicValue oldValue);

    protected abstract Type getType();

    protected abstract RoleModel getRoleModel(String roleTypeUri);



    // ----------------------------------------------------------------------------------------- Package Private Methods

    void store() {
        if (getType().getDataTypeUri().equals("dm3.core.composite")) {
            storeComposite(getComposite());         // setComposite() includes setValue()
        } else {
            storeTopicValue(getValue());
        }
    }

    void update(DeepaMehtaObjectModel model) {
        // ### TODO: compare new model with current one and update only if changed. See AttachedAssociation.update()
        if (getType().getDataTypeUri().equals("dm3.core.composite")) {
            setComposite(model.getComposite());     // setComposite() includes setValue()
        } else {
            setValue(model.getValue());
        }
        setUri(model.getUri());
    }

    /**
     * Called from {@link EmbeddedService#attach}
     */
    void loadComposite() {
        // fetch from DB
        Composite comp = fetchComposite();
        // update memory
        model.setComposite(comp);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    // === Fetch ===

    private Composite fetchComposite() {
        try {
            Composite comp = new Composite();
            for (AssociationDefinition assocDef : getType().getAssocDefs().values()) {
                String assocDefUri = assocDef.getUri();
                TopicType partTopicType = dms.getTopicType(assocDef.getPartTopicTypeUri(), null);  // clientContext=null
                if (partTopicType.getDataTypeUri().equals("dm3.core.composite")) {
                    AttachedTopic childTopic = fetchChildTopic(assocDef);
                    if (childTopic != null) {
                        // Note: cast required because private method is called on a subclass's instance
                        comp.put(assocDefUri, ((AttachedDeepaMehtaObject) childTopic).fetchComposite());
                    }
                } else {
                    TopicValue value = fetchChildTopicValue(assocDef);
                    if (value != null) {
                        comp.put(assocDefUri, value.value());
                    }
                }
            }
            return comp;
        } catch (Exception e) {
            throw new RuntimeException("Fetching the topic's composite failed (" + this + ")", e);
        }
    }

    private TopicValue fetchChildTopicValue(AssociationDefinition assocDef) {
        Topic childTopic = fetchChildTopic(assocDef);
        if (childTopic != null) {
            return childTopic.getValue();
        }
        return null;
    }

    // === Store ===

    // TODO: factorize this method
    private void storeComposite(Composite comp) {
        try {
            Iterator<String> i = comp.keys();
            while (i.hasNext()) {
                String key = i.next();
                String[] t = key.split("\\$");
                //
                if (t.length < 1 || t.length > 2 || t.length == 2 && !t[1].equals("id")) {
                    throw new RuntimeException("Invalid composite key (\"" + key + "\")");
                }
                //
                String assocDefUri = t[0];
                AssociationDefinition assocDef = getAssocDef(assocDefUri);
                TopicType childTopicType = dms.getTopicType(assocDef.getPartTopicTypeUri(), null);
                String assocTypeUri = assocDef.getTypeUri();
                Object value = comp.get(key);
                if (assocTypeUri.equals("dm3.core.composition_def")) {
                    if (childTopicType.getDataTypeUri().equals("dm3.core.composite")) {
                        AttachedTopic childTopic = storeChildTopicValue(assocDef, null);
                        // Note: cast required because private method is called on a subclass's instance
                        ((AttachedDeepaMehtaObject) childTopic).storeComposite((Composite) value);
                    } else {
                        storeChildTopicValue(assocDef, new TopicValue(value));
                    }
                } else if (assocTypeUri.equals("dm3.core.aggregation_def")) {
                    if (childTopicType.getDataTypeUri().equals("dm3.core.composite")) {
                        throw new RuntimeException("Aggregation of composite topic types not yet supported");
                    } else {
                        // remove current assignment
                        RelatedTopic childTopic = fetchChildTopic(assocDef);
                        if (childTopic != null) {
                            long assocId = childTopic.getAssociation().getId();
                            dms.deleteAssociation(assocId, null);  // clientContext=null
                        }
                        //
                        boolean assignExistingTopic = t.length == 2;
                        if (assignExistingTopic) {
                            // update DB
                            long childTopicId = (Integer) value;   // Note: the JSON parser creates Integers (not Longs)
                            associateChildTopic(assocDef, childTopicId);
                            // adjust memory
                            // ### FIXME: ConcurrentModificationException
                            // ### current workaround: refetch after update, see EmbeddedService.updateTopic()
                            // Topic assignedTopic = dms.getTopic(childTopicId, false, null);  // fetchComposite=false
                            // comp.remove(key);
                            // comp.put(assocDefUri, assignedTopic.getValue().value());
                        } else {
                            // create new child topic
                            storeChildTopicValue(assocDef, new TopicValue(value));
                        }
                    }
                } else {
                    throw new RuntimeException("Association type \"" + assocTypeUri + "\" not supported");
                }
            }
            //
            updateValue(comp);
        } catch (Exception e) {
            throw new RuntimeException("Storing the " + getClass().getSimpleName() + "'s composite failed (" + this +
                ",\ncomposite=" + comp + ")", e);
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
    private AttachedTopic storeChildTopicValue(AssociationDefinition assocDef, final TopicValue value) {
        try {
            AttachedTopic childTopic = fetchChildTopic(assocDef);
            if (childTopic != null) {
                if (value != null) {
                    childTopic.setValue(value);
                }
            } else {
                // create child topic
                String topicTypeUri = assocDef.getPartTopicTypeUri();
                childTopic = dms.createTopic(new TopicModel(null, value, topicTypeUri, null), null);
                // associate child topic
                associateChildTopic(assocDef, childTopic.getId());
            }
            return childTopic;
        } catch (Exception e) {
            throw new RuntimeException("Storing child topic value failed (parentTopic=" + this +
                ", assocDef=" + assocDef + ", value=" + value + ")", e);
        }
    }

    private void storeTopicValue(TopicValue value) {
        TopicValue oldValue = storeValue(value);
        indexTopicValue(value, oldValue);
    }

    private void indexTopicValue(TopicValue value, TopicValue oldValue) {
        Type type = getType();
        String indexKey = type.getUri();
        // strip HTML tags before indexing
        if (type.getDataTypeUri().equals("dm3.core.html")) {
            value = new TopicValue(JavaUtils.stripHTML(value.toString()));
            if (oldValue != null) {
                oldValue = new TopicValue(JavaUtils.stripHTML(oldValue.toString()));
            }
        }
        //
        for (IndexMode indexMode : type.getIndexModes()) {
            indexValue(indexMode, indexKey, value, oldValue);
        }
    }

    private void updateValue(Composite comp) {
        setValue(comp.getLabel());
    }

    // === Helper ===

    private AttachedRelatedTopic fetchChildTopic(AssociationDefinition assocDef) {
        String assocTypeUri       = assocDef.getInstanceLevelAssocTypeUri();
        String myRoleTypeUri      = assocDef.getWholeRoleTypeUri();
        String othersRoleTypeUri  = assocDef.getPartRoleTypeUri();
        String othersTopicTypeUri = assocDef.getPartTopicTypeUri();
        //
        return getRelatedTopic(assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri, true, false);
        // fetchComposite=true ### false sufficient?
    }

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
