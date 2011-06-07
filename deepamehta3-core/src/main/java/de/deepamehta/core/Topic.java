package de.deepamehta.core;

import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.TopicValue;

import org.codehaus.jettison.json.JSONObject;
import java.util.Set;



/**
 * Specification of a topic -- DeepaMehta's central data object.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface Topic {

    long getId();

    String getUri();

    TopicValue getValue();

    String getTypeUri();

    Composite getComposite();

    // ---

    void setUri(String uri);

    // ---

    void setValue(String value);

    void setValue(int value);

    void setValue(long value);

    void setValue(boolean value);

    void setValue(TopicValue value);

    // ---

    void setComposite(Composite comp);

    // === Traversal ===

    TopicValue getChildTopicValue(String assocDefUri);

    void setChildTopicValue(String assocDefUri, TopicValue value);

    Set<RelatedTopic> getRelatedTopics(String assocTypeUri);

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    RelatedTopic getRelatedTopic(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                            String othersTopicTypeUri,
                                                                            boolean fetchComposite);

    /**
     * @param   assocTypeUri        may be null
     * @param   myRoleTypeUri       may be null
     * @param   othersRoleTypeUri   may be null
     * @param   othersTopicTypeUri  may be null
     */
    Set<RelatedTopic> getRelatedTopics(String assocTypeUri, String myRoleTypeUri, String othersRoleTypeUri,
                                                                                  String othersTopicTypeUri,
                                                                                  boolean fetchComposite);

    Set<Association> getAssociations(String myRoleTypeUri);

    // === Serialization ===

    JSONObject toJSON();
}
