package de.deepamehta.core.util;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.Identifiable;
import de.deepamehta.core.JSONEnabled;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.service.DeepaMehtaService;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



public class DeepaMehtaUtils {

    private static Logger logger = Logger.getLogger("de.deepamehta.core.util.DeepaMehtaUtils");

    private static final String DM4_HOST_URL = System.getProperty("dm4.host.url");
    static {
        logger.info("Host setting:\n    dm4.host.url=\""+ DM4_HOST_URL + "\"");
    }



    // ************
    // *** URLs ***
    // ************



    /**
     * Checks if an URL refers to this DeepaMehta installation.
     * The check relies on the "dm4.host.url" system property.
     */
    public static boolean isDeepaMehtaURL(URL url) {
        try {
            return url.toString().startsWith(DM4_HOST_URL);
        } catch (Exception e) {
            throw new RuntimeException("Checking for DeepaMehta URL failed (url=\"" + url + "\")", e);
        }
    }



    // *******************
    // *** Collections ***
    // *******************



    public static List<Long> idList(Iterable<? extends Identifiable> items) {
        List<Long> ids = new ArrayList();
        for (Identifiable item : items) {
            ids.add(item.getId());
        }
        return ids;
    }

    public static List<TopicModel> toTopicModels(Iterable<? extends Topic> topics) {
        List<TopicModel> topicModels = new ArrayList();
        for (Topic topic : topics) {
            topicModels.add(topic.getModel());
        }
        return topicModels;
    }

    public static String topicNames(Iterable<? extends Topic> topics) {
        StringBuilder names = new StringBuilder();
        Iterator<? extends Topic> i = topics.iterator();
        while (i.hasNext()) {
            Topic topic = i.next();
            names.append('"').append(topic.getSimpleValue()).append('"');
            if (i.hasNext()) {
                names.append(", ");
            }
        }
        return names.toString();
    }



    // ************
    // *** JSON ***
    // ************



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
        } catch (Exception e) {
            throw new RuntimeException("Converting JSONObject to Map failed", e);
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
        } catch (Exception e) {
            throw new RuntimeException("Converting JSONArray to List failed", e);
        }
    }

    // ---

    // ### TODO: drop this method? There is a JSONArray(Collection) constructor.
    public static JSONArray stringsToJson(Collection<String> strings) {
        JSONArray array = new JSONArray();
        for (String string : strings) {
            array.put(string);
        }
        return array;
    }

    // === DeepaMehta specific ===

    // ### TODO: rename to toJSONArray()
    public static JSONArray objectsToJSON(Iterable<? extends JSONEnabled> items) {
        JSONArray array = new JSONArray();
        for (JSONEnabled item : items) {
            array.put(item.toJSON());
        }
        return array;
    }

    // ---

    /**
     * Creates types and topics from a JSON formatted input stream.
     *
     * @param   migrationFileName   The origin migration file. Used for logging only.
     */
    public static void readMigrationFile(InputStream is, String migrationFileName, DeepaMehtaService dms) {
        try {
            logger.info("Reading migration file \"" + migrationFileName + "\"");
            String fileContent = JavaUtils.readText(is);
            //
            JSONObject o = new JSONObject(fileContent);
            JSONArray topicTypes = o.optJSONArray("topic_types");
            if (topicTypes != null) {
                createTopicTypes(topicTypes, dms);
            }
            JSONArray assocTypes = o.optJSONArray("assoc_types");
            if (assocTypes != null) {
                createAssociationTypes(assocTypes, dms);
            }
            JSONArray topics = o.optJSONArray("topics");
            if (topics != null) {
                createTopics(topics, dms);
            }
            JSONArray assocs = o.optJSONArray("associations");
            if (assocs != null) {
                createAssociations(assocs, dms);
            }
        } catch (Exception e) {
            throw new RuntimeException("Reading migration file \"" + migrationFileName + "\" failed", e);
        }
    }

    public static void createTopicTypes(JSONArray topicTypes, DeepaMehtaService dms) throws Exception {
        for (int i = 0; i < topicTypes.length(); i++) {
            TopicTypeModel topicTypeModel = new TopicTypeModel(topicTypes.getJSONObject(i));
            dms.createTopicType(topicTypeModel);
        }
    }

    public static void createAssociationTypes(JSONArray assocTypes, DeepaMehtaService dms) throws Exception {
        for (int i = 0; i < assocTypes.length(); i++) {
            AssociationTypeModel assocTypeModel = new AssociationTypeModel(assocTypes.getJSONObject(i));
            dms.createAssociationType(assocTypeModel);
        }
    }

    public static void createTopics(JSONArray topics, DeepaMehtaService dms) throws Exception {
        for (int i = 0; i < topics.length(); i++) {
            TopicModel topicModel = new TopicModel(topics.getJSONObject(i));
            dms.createTopic(topicModel);
        }
    }

    public static void createAssociations(JSONArray assocs, DeepaMehtaService dms) throws Exception {
        for (int i = 0; i < assocs.length(); i++) {
            AssociationModel assocModel = new AssociationModel(assocs.getJSONObject(i));
            dms.createAssociation(assocModel);
        }
    }
}
