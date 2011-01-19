package de.deepamehta.core.util;

import de.deepamehta.core.model.RelatedTopic;
import de.deepamehta.core.model.Relation;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.service.CoreService;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



public class JSONHelper {

    private static Logger logger = Logger.getLogger("de.deepamehta.core.util.JSONHelper");

    // === Generic ===

    public static Map toMap(JSONObject o) {
        return toMap(o, new HashMap());
    }

    public static Map toMap(JSONObject o, Map map) {
        try {
            Iterator<String> i = o.keys();
            while (i.hasNext()) {
                String key = i.next();
                map.put(key, o.get(key));   // throws JSONException
            }
            return map;
        } catch (JSONException e) {
            throw new RuntimeException("Error while converting JSONObject to Map", e);
        }
    }

    // ---

    public static List toList(JSONArray o) {
        try {
            List list = new ArrayList();
            for (int i = 0; i < o.length(); i++) {
                list.add(o.get(i));         // throws JSONException
            }
            return list;
        } catch (JSONException e) {
            throw new RuntimeException("Error while converting JSONArray to Map", e);
        }
    }

    // === DeepaMehta specific ===

    public static JSONArray topicsToJson(List<Topic> topics) {
        JSONArray array = new JSONArray();
        for (Topic topic : topics) {
            array.put(topic.toJSON());
        }
        return array;
    }

    public static JSONArray relationsToJson(List<Relation> relations) {
        JSONArray array = new JSONArray();
        for (Relation relation : relations) {
            array.put(relation.toJSON());
        }
        return array;
    }

    // FIXME: for the moment it is sufficient to serialize the topics only. The respective relations are omitted.
    public static JSONArray relatedTopicsToJson(List<RelatedTopic> relTopics) {
        JSONArray array = new JSONArray();
        for (RelatedTopic relTopic : relTopics) {
            array.put(relTopic.getTopic().toJSON());
        }
        return array;
    }

    // ---

    /**
     * Creates types and topics from a JSON formatted input stream.
     *
     * @param   migrationFileName   The origin migration file. Used for logging only.
     */
    public static void readMigrationFile(InputStream is, String migrationFileName, CoreService dms) {
        try {
            logger.info("Reading migration file \"" + migrationFileName + "\"");
            String fileContent = JavaUtils.readTextFile(is);
            //
            JSONObject o = new JSONObject(fileContent);
            JSONArray types = o.optJSONArray("topic_types");
            if (types != null) {
                createTypes(types, dms);
            }
            JSONArray topics = o.optJSONArray("topics");
            if (topics != null) {
                createTopics(topics, dms);
            }
        } catch (Throwable e) {
            throw new RuntimeException("Error while reading migration file \"" + migrationFileName + "\"", e);
        }
    }

    public static void createTypes(JSONArray types, CoreService dms) throws JSONException {
        for (int i = 0; i < types.length(); i++) {
            TopicType topicType = new TopicType(types.getJSONObject(i));
            dms.createTopicType(topicType.getProperties(), topicType.getDataFields(), null);    // clientContext=null
        }
    }

    public static void createTopics(JSONArray topics, CoreService dms) throws JSONException {
        for (int i = 0; i < topics.length(); i++) {
            Topic topic = new Topic(topics.getJSONObject(i));
            dms.createTopic(topic.typeUri, topic.getProperties(), null);                        // clientContext=null
        }
    }
}
