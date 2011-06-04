package de.deepamehta.core.impl.service;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.impl.model.TopicBase;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationDefinition;
import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.core.util.JavaUtils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;



/**
 * A topic that is attached to the {@link DeepaMehtaService}.
 */
class AttachedTopic extends TopicBase {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    protected final EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedTopic(Topic topic, EmbeddedService dms) {
        this(((TopicBase) topic).getModel(), dms);
    }

    AttachedTopic(TopicModel topicModel, EmbeddedService dms) {
        super(topicModel);
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Topic Overrides ===

    @Override
    public void setUri(String uri) {
        // update memory
        super.setUri(uri);
        // update DB
        storeTopicUri(uri);
    }

    @Override
    public void setValue(TopicValue value) {
        // update memory
        super.setValue(value);
        // update DB
        storeTopicValue(value);
    }

    @Override
    public void setComposite(Composite comp) {
        // update memory
        super.setComposite(comp);
        // update DB
        storeComposite(comp);
    }

    // ---

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
        updateTopicValue(comp);
    }

    @Override
    public Set<RelatedTopic> getRelatedTopics(String assocTypeUri) {
        Set<RelatedTopic> topics = dms.storage.getTopicRelatedTopics(getId(), assocTypeUri, null, null, null);
        //
        /* ### for (RelatedTopic topic : topics) {
            triggerHook(Hook.PROVIDE_TOPIC_PROPERTIES, relTopic.getTopic());
            triggerHook(Hook.PROVIDE_RELATION_PROPERTIES, relTopic.getRelation());
        } */
        //
        return topics;
    }

    @Override
    public AttachedRelatedTopic getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                           String othersTopicTypeUri,
                                                                                           boolean fetchComposite) {
        RelatedTopic topic = dms.storage.getTopicRelatedTopic(getId(), assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri);
        return topic != null ? dms.attach(topic, fetchComposite) : null;
    }

    @Override
    public Set<RelatedTopic> getRelatedTopics(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                         String othersTopicTypeUri,
                                                                                         boolean fetchComposite) {
        return dms.attach(dms.storage.getTopicRelatedTopics(getId(), assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri), fetchComposite);
    }

    @Override
    public Set<Association> getAssociations(String myRoleTypeUri) {
        return dms.getAssociations(getId(), myRoleTypeUri);
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * Called from {@link EmbeddedService#attach}
     */
    void loadComposite() {
        // fetch from DB
        Composite comp = fetchComposite();
        // update memory
        super.setComposite(comp);
    }

    void update(TopicModel topicModel) {
        if (getTopicType().getDataTypeUri().equals("dm3.core.composite")) {
            setComposite(topicModel.getComposite());    // setComposite() includes setValue()
        } else {
            setValue(topicModel.getValue());
        }
        setUri(topicModel.getUri());
    }

    TopicType getTopicType() {
        return dms.getTopicType(getTypeUri(), null);    // FIXME: clientContext=null
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Fetch ===

    private Composite fetchComposite() {
        try {
            Composite comp = new Composite();
            for (AssociationDefinition assocDef : getTopicType().getAssocDefs().values()) {
                String assocDefUri = assocDef.getUri();
                TopicType topicType2 = dms.getTopicType(assocDef.getTopicTypeUri2(), null); // FIXME: clientContext=null
                if (topicType2.getDataTypeUri().equals("dm3.core.composite")) {
                    AttachedTopic childTopic = fetchChildTopic(assocDef);
                    if (childTopic != null) {
                        comp.put(assocDefUri, childTopic.fetchComposite());
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

    private void storeTopicUri(String uri) {
        dms.storage.setTopicUri(getId(), uri);
    }

    private void storeTopicValue(TopicValue value) {
        TopicValue oldValue = dms.storage.setTopicValue(getId(), value);
        indexTopicValue(value, oldValue);
    }

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
                TopicType childTopicType = dms.getTopicType(assocDef.getTopicTypeUri2(), null);
                String assocTypeUri = assocDef.getAssocTypeUri();
                Object value = comp.get(key);
                if (assocTypeUri.equals("dm3.core.composition")) {
                    if (childTopicType.getDataTypeUri().equals("dm3.core.composite")) {
                        AttachedTopic childTopic = storeChildTopicValue(assocDef, null);
                        childTopic.storeComposite((Composite) value);
                    } else {
                        storeChildTopicValue(assocDef, new TopicValue(value));
                    }
                } else if (assocTypeUri.equals("dm3.core.aggregation")) {
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
                            long childTopicId = (Integer) value;   // Note: the JSON parser creates Integers (not Longs)
                            associateChildTopic(assocDef, childTopicId);
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
            updateTopicValue(comp);
        } catch (Exception e) {
            throw new RuntimeException("Storing the topic's composite failed (topic=" +
                this + ",\ncomposite=" + comp + ")", e);
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
                String topicTypeUri = assocDef.getTopicTypeUri2();
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

    private void indexTopicValue(TopicValue value, TopicValue oldValue) {
        TopicType topicType = getTopicType();
        String indexKey = topicType.getUri();
        // strip HTML tags before indexing
        if (topicType.getDataTypeUri().equals("dm3.core.html")) {
            value = new TopicValue(JavaUtils.stripHTML(value.toString()));
            if (oldValue != null) {
                oldValue = new TopicValue(JavaUtils.stripHTML(oldValue.toString()));
            }
        }
        //
        for (IndexMode indexMode : topicType.getIndexModes()) {
            dms.storage.indexTopicValue(getId(), indexMode, indexKey, value, oldValue);
        }
    }



    // === Helper ===

    private AttachedRelatedTopic fetchChildTopic(AssociationDefinition assocDef) {
        String assocTypeUri       = assocDef.getAssocTypeUri();
        String myRoleTypeUri      = assocDef.getRoleTypeUri1();
        String othersRoleTypeUri  = assocDef.getRoleTypeUri2();
        String othersTopicTypeUri = assocDef.getTopicTypeUri2();
        //
        return getRelatedTopic(assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri, true);
        // fetchComposite=true ### false sufficient?
    }

    private void associateChildTopic(AssociationDefinition assocDef, long childTopicId) {
        AssociationModel assocModel = new AssociationModel(assocDef.getAssocTypeUri());
        assocModel.setRoleModel1(new TopicRoleModel(getId(), assocDef.getRoleTypeUri1()));
        assocModel.setRoleModel2(new TopicRoleModel(childTopicId, assocDef.getRoleTypeUri2()));
        dms.createAssociation(assocModel, null);     // FIXME: clientContext=null
    }

    // ---

    private AssociationDefinition getAssocDef(String assocDefUri) {
        return getTopicType().getAssocDef(assocDefUri);
    }

    // ---

    private void updateTopicValue(Composite comp) {
        setValue(comp.getLabel());
    }
}
