package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;



/**
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface Topic {

    public long getId();

    public String getUri();

    public TopicValue getValue();

    public String getTypeUri();

    public Composite getComposite();

    // ---

    public void setValue(String value);

    public void setValue(int value);

    public void setValue(long value);

    public void setValue(boolean value);

    public void setValue(TopicValue value);

    // ---

    TopicValue getValue(String assocDefUri);

    void setValue(String assocDefUri, TopicValue value);

    Topic getRelatedTopic(String assocTypeUri, String myRoleType, String othersRoleType);

    // ---

    public JSONObject toJSON();
}
