package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.AssociationData;
import de.deepamehta.core.model.AssociationDefinition;
import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRole;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.core.model.impl.TopicBase;
import de.deepamehta.core.util.JavaUtils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;



/**
 * A topic that is attached to the {@link CoreService}.
 */
class AttachedTopic extends TopicBase {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedTopic(Topic topic, EmbeddedService dms) {
        super(new TopicModel(topic));
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // === Topic Overrides ===

    @Override
    public void setValue(TopicValue value) {
        // update memory
        super.setValue(value);
        // update DB
        storeTopicValue(value);
    }

    // ---

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
        return fetchChildTopicValue(assocDefUri);
    }

    @Override
    public void setChildTopicValue(String assocDefUri, TopicValue value) {
        Composite comp = getComposite();
        // update memory
        comp.put(assocDefUri, value.value());
        // update DB
        storeChildTopicValue(assocDefUri, value);
        //
        updateTopicValue(comp);
    }

    public Set<Topic> getRelatedTopics(String assocTypeUri) {
        return dms.getRelatedTopics(getId(), assocTypeUri);
    }

    @Override
    public AttachedTopic getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                            String othersTopicTypeUri) {
        Topic topic = dms.storage.getRelatedTopic(getId(), assocTypeUri, myRoleTypeUri, othersRoleTypeUri,
            othersTopicTypeUri);
        return topic != null ? dms.buildTopic(topic, true) : null;
    }

    @Override
    public Set<Topic> getRelatedTopics(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                  String othersTopicTypeUri,
                                                                                  boolean fetchComposite) {
        return dms.getRelatedTopics(getId(), assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri,
            fetchComposite);
    }

    @Override
    public Set<Association> getAssociations(String myRoleTypeUri) {
        return dms.getAssociations(getId(), myRoleTypeUri);
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * Called from {@link EmbeddedService#buildTopic}
     */
    void loadComposite() {
        // fetch from DB
        Composite comp = fetchComposite();
        // update memory
        super.setComposite(comp);
    }

    void update(TopicModel topicModel) {
        if (getTopicType().getDataTypeUri().equals("dm3.core.composite")) {
            setComposite(topicModel.getComposite());
        } else {
            setValue(topicModel.getValue());
        }
    }

    TopicType getTopicType() {
        return dms.getTopicType(getTypeUri(), null);      // FIXME: clientContext=null
    }

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Fetch ===

    private Composite fetchComposite() {
        Composite comp = new Composite();
        for (AssociationDefinition assocDef : getTopicType().getAssocDefs().values()) {
            String assocDefUri = assocDef.getUri();
            TopicType topicType2 = dms.getTopicType(assocDef.getTopicTypeUri2(), null);   // FIXME: clientContext=null
            if (topicType2.getDataTypeUri().equals("dm3.core.composite")) {
                AttachedTopic childTopic = new ChildTopicEvaluator(assocDefUri).getChildTopic();
                if (childTopic != null) {
                    comp.put(assocDefUri, childTopic.fetchComposite());
                }
            } else {
                TopicValue value = fetchChildTopicValue(assocDefUri);
                if (value != null) {
                    comp.put(assocDefUri, value.value());
                }
            }
        }
        return comp;
    }

    private TopicValue fetchChildTopicValue(String assocDefUri) {
        Topic childTopic = new ChildTopicEvaluator(assocDefUri).getChildTopic();
        if (childTopic != null) {
            return childTopic.getValue();
        }
        return null;
    }



    // === Store ===

    private void storeTopicValue(TopicValue value) {
        TopicValue oldValue = dms.storage.setTopicValue(getId(), value);
        indexTopicValue(value, oldValue);
    }

    private void storeComposite(Composite comp) {
        Iterator<String> i = comp.keys();
        while (i.hasNext()) {
            String assocDefUri = i.next();
            Object value = comp.get(assocDefUri);
            if (value instanceof Composite) {
                AttachedTopic childTopic = storeChildTopicValue(assocDefUri, null);
                childTopic.storeComposite((Composite) value);
            } else {
                storeChildTopicValue(assocDefUri, new TopicValue(value));
            }
        }
        //
        updateTopicValue(comp);
    }

    /**
     * Set a child's topic value. If the child topic does not exist it is created.
     *
     * @param   parentTopic     The parent topic.
     * @param   assocDefUri     The "axis" that leads to the child. The URI of an {@link AssociationDefinition}.
     * @param   value           The value to set. If <code>null</code> nothing is set. The child topic is potentially
     *                          created and returned anyway.
     *
     * @return  The child topic.
     */
    private AttachedTopic storeChildTopicValue(String assocDefUri, final TopicValue value) {
        try {
            return new ChildTopicEvaluator(assocDefUri) {
                @Override
                void evaluate(AttachedTopic childTopic, AssociationDefinition assocDef) {
                    if (childTopic != null) {
                        if (value != null) {
                            childTopic.setValue(value);
                        }
                    } else {
                        // create child topic
                        String topicTypeUri = assocDef.getTopicTypeUri2();
                        childTopic = dms.createTopic(new TopicModel(null, value, topicTypeUri, null), null);
                        setChildTopic(childTopic);
                        // associate child topic
                        AssociationData assocData = new AssociationData(assocDef.getAssocTypeUri());
                        assocData.addTopicRole(new TopicRole(getId(), assocDef.getRoleTypeUri1()));
                        assocData.addTopicRole(new TopicRole(childTopic.getId(), assocDef.getRoleTypeUri2()));
                        dms.createAssociation(assocData, null);     // FIXME: clientContext=null
                    }
                }
            }.getChildTopic();
        } catch (Exception e) {
            throw new RuntimeException("Setting child topic value failed (parentTopic=" + this +
                ", assocDefUri=\"" + assocDefUri + "\", value=" + value + ")", e);
        }
    }

    /**
     * @param   topic   Hint: only the topic ID and type URI are evaluated
     */
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

    private class ChildTopicEvaluator {

        private AttachedTopic childTopic;
        private AssociationDefinition assocDef;

        // ---

        private ChildTopicEvaluator(String assocDefUri) {
            fetchChildTopic(assocDefUri);
            evaluate(childTopic, assocDef);
        }

        // ---

        /**
         * Note: if the caller uses evaluate() to create a missing child topic
         * he must not forget to call setChildTopic().
         */
        void evaluate(AttachedTopic childTopic, AssociationDefinition assocDef) {
        }

        // ---

        AttachedTopic getChildTopic() {
            return childTopic;
        }

        void setChildTopic(AttachedTopic childTopic) {
            this.childTopic = childTopic;
        }

        // ---

        private void fetchChildTopic(String assocDefUri) {
            this.assocDef = getTopicType().getAssocDef(assocDefUri);
            String assocTypeUri = assocDef.getAssocTypeUri();
            String roleTypeUri1 = assocDef.getRoleTypeUri1();
            String roleTypeUri2 = assocDef.getRoleTypeUri2();
            //
            this.childTopic = getRelatedTopic(assocTypeUri, roleTypeUri1, roleTypeUri2, assocDefUri);
        }
    }

    // ---

    private void updateTopicValue(Composite comp) {
        setValue(comp.getLabel());
    }
}
