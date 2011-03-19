package de.deepamehta.core.model.helper;

import de.deepamehta.core.model.Topic;
import org.codehaus.jettison.json.JSONArray;
import java.util.List;



public class ModelHelper {

    public static JSONArray topicsToJSON(List<Topic> topics) {
        JSONArray array = new JSONArray();
        for (Topic topic : topics) {
            array.put(topic.toJSON());
        }
        return array;
    }
}
