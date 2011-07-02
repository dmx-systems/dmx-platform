package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONObject;

import java.util.Set;
import java.util.logging.Logger;



/**
 * Collection of the data that makes up a {@link TopicType}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public class TopicTypeModel extends TypeModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicTypeModel(String uri, String value, String dataTypeUri) {
        this(uri, value, "dm3.core.topic_type", dataTypeUri);
    }

    public TopicTypeModel(String uri, String value, String topicTypeUri, String dataTypeUri) {
        super(uri, new TopicValue(value), topicTypeUri, dataTypeUri);
    }

    public TopicTypeModel(TopicModel model) {
        super(model);
    }

    public TopicTypeModel(TopicTypeModel model) {
        super(model);
    }

    public TopicTypeModel(JSONObject topicTypeModel) {
        super(topicTypeModel, "dm3.core.topic_type");
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String toString() {
        return "topic type model (" + super.toString() + ")";
    }
}
