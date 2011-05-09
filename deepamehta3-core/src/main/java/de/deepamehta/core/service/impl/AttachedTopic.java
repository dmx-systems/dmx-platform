package de.deepamehta.core.service.impl;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.AssociationData;
import de.deepamehta.core.model.AssociationDefinition;
import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRole;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.core.model.impl.TopicBase;

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

    @Override
    public void setValue(TopicValue value) {
        // update memory
        super.setValue(value);
        // update DB
        dms.setTopicValue(this, value);
    }

    // ---

    @Override
    public void setComposite(Composite comp) {
        // update memory
        super.setComposite(comp);
        // update DB
        storeComposite(this, comp);
    }

    void fetchComposite() {
        super.setComposite(fetchComposite(this));
    }

    // ---

    @Override
    public TopicValue getChildTopicValue(String assocDefUri) {
        return fetchChildTopicValue(this, assocDefUri);
    }

    @Override
    public void setChildTopicValue(String assocDefUri, TopicValue value) {
        Composite comp = getComposite();
        // update memory
        comp.put(assocDefUri, value.value());
        // update DB
        storeChildTopicValue(this, assocDefUri, value);
        //
        updateTopicValue(this, comp);
    }

    public Set<Topic> getRelatedTopics(String assocTypeUri) {
        return dms.getRelatedTopics(getId(), assocTypeUri);
    }

    @Override
    public Topic getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                            String othersTopicTypeUri) {
        return dms.getRelatedTopic(getId(), assocTypeUri, myRoleTypeUri, othersRoleTypeUri, othersTopicTypeUri);
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

    // ------------------------------------------------------------------------------------------------- Private Methods



    // === Fetch ===

    private Composite fetchComposite(Topic topic) {
        Composite comp = new Composite();
        for (AssociationDefinition assocDef : getTopicType(topic).getAssocDefs().values()) {
            String assocDefUri = assocDef.getUri();
            TopicType topicType2 = dms.getTopicType(assocDef.getTopicTypeUri2(), null);   // FIXME: clientContext=null
            if (topicType2.getDataTypeUri().equals("dm3.core.composite")) {
                Topic childTopic = new ChildTopicEvaluator(topic, assocDefUri).getChildTopic();
                if (childTopic != null) {
                    comp.put(assocDefUri, fetchComposite(childTopic));
                }
            } else {
                TopicValue value = fetchChildTopicValue(topic, assocDefUri);
                if (value != null) {
                    comp.put(assocDefUri, value.value());
                }
            }
        }
        return comp;
    }

    private TopicValue fetchChildTopicValue(Topic parentTopic, String assocDefUri) {
        Topic childTopic = new ChildTopicEvaluator(parentTopic, assocDefUri).getChildTopic();
        if (childTopic != null) {
            return childTopic.getValue();
        }
        return null;
    }



    // === Store ===

    private void storeComposite(Topic topic, Composite comp) {
        Iterator<String> i = comp.keys();
        while (i.hasNext()) {
            String assocDefUri = i.next();
            Object value = comp.get(assocDefUri);
            if (value instanceof Composite) {
                Topic childTopic = storeChildTopicValue(topic, assocDefUri, null);
                storeComposite(childTopic, (Composite) value);
            } else {
                storeChildTopicValue(topic, assocDefUri, new TopicValue(value));
            }
        }
        //
        updateTopicValue(topic, comp);
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
    private Topic storeChildTopicValue(final Topic parentTopic, String assocDefUri, final TopicValue value) {
        try {
            return new ChildTopicEvaluator(parentTopic, assocDefUri) {
                @Override
                void evaluate(Topic childTopic, AssociationDefinition assocDef) {
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
                        assocData.addTopicRole(new TopicRole(parentTopic.getId(), assocDef.getRoleTypeUri1()));
                        assocData.addTopicRole(new TopicRole(childTopic.getId(), assocDef.getRoleTypeUri2()));
                        dms.createAssociation(assocData, null);     // FIXME: clientContext=null
                    }
                }
            }.getChildTopic();
        } catch (Exception e) {
            throw new RuntimeException("Setting child topic value failed (parentTopic=" + parentTopic +
                ", assocDefUri=\"" + assocDefUri + "\", value=" + value + ")", e);
        }
    }



    // === Helper ===

    private class ChildTopicEvaluator {

        private Topic childTopic;   // is attached
        private AssociationDefinition assocDef;

        // ---

        /**
         * @param   parentTopic     Hint: only the topic ID and type URI are evaluated
         */
        private ChildTopicEvaluator(Topic parentTopic, String assocDefUri) {
            getChildTopic(parentTopic, assocDefUri);
            evaluate(childTopic, assocDef);
        }

        // ---

        /**
         * Note: if the caller uses evaluate() to create a missing child topic
         * he must not forget to call setChildTopic().
         */
        void evaluate(Topic childTopic, AssociationDefinition assocDef) {
        }

        // ---

        Topic getChildTopic() {
            return childTopic;
        }

        void setChildTopic(Topic childTopic) {
            this.childTopic = childTopic;
        }

        // ---

        private void getChildTopic(Topic parentTopic, String assocDefUri) {
            this.assocDef = getTopicType(parentTopic).getAssocDef(assocDefUri);
            String assocTypeUri = assocDef.getAssocTypeUri();
            String roleTypeUri1 = assocDef.getRoleTypeUri1();
            String roleTypeUri2 = assocDef.getRoleTypeUri2();
            //
            this.childTopic = dms.getRelatedTopic(parentTopic.getId(), assocTypeUri,
                roleTypeUri1, roleTypeUri2, assocDefUri);
        }
    }

    // ---

    private void updateTopicValue(Topic topic, Composite comp) {
        String label = comp.getLabel();
        if (label != null) {
            topic.setValue(label);
        }
    }

    // FIXME: copy in EmbeddedService
    private TopicType getTopicType(Topic topic) {
        return dms.getTopicType(topic.getTypeUri(), null);      // FIXME: clientContext=null
    }
}
