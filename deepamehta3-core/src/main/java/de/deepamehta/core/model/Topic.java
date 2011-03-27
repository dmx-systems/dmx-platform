package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;
import java.util.Set;



/**
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface Topic {

    long getId();

    String getUri();

    TopicValue getValue();

    String getTypeUri();

    Composite getComposite();

    // ---

    void setValue(String value);

    void setValue(int value);

    void setValue(long value);

    void setValue(boolean value);

    void setValue(TopicValue value);

    // ---

    TopicValue getValue(String assocDefUri);

    void setValue(String assocDefUri, TopicValue value);

    Topic getRelatedTopic(String assocTypeUri, String myRoleType, String othersRoleType);

    Set<Topic> getRelatedTopics(String assocTypeUri, String myRoleType, String othersRoleType);

    // ---

    JSONObject toJSON();
}
