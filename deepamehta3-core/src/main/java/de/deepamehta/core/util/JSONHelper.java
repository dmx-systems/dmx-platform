package de.deepamehta.core.util;

import de.deepamehta.core.model.Association;
import de.deepamehta.core.model.AssociationTypeData;
import de.deepamehta.core.model.MetaTypeData;
import de.deepamehta.core.model.PluginInfo;
import de.deepamehta.core.model.RelatedTopic;
import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicData;
import de.deepamehta.core.model.TopicTypeData;
import de.deepamehta.core.service.CoreService;

import org.codehaus.jettison.json.JSONArray;
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
import java.util.Set;
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

    public static JSONArray stringsToJson(Set<String> strings) {
        JSONArray array = new JSONArray();
        for (String string : strings) {
            array.put(string);
        }
        return array;
    }

    // === DeepaMehta specific ===

    /**
     * Creates types and topics from a JSON formatted input stream.
     *
     * @param   migrationFileName   The origin migration file. Used for logging only.
     */
    public static void readMigrationFile(InputStream is, String migrationFileName, CoreService dms) {
        try {
            logger.info("Reading migration file \"" + migrationFileName + "\"");
            String fileContent = JavaUtils.readText(is);
            //
            JSONObject o = new JSONObject(fileContent);
            JSONArray metaTypes = o.optJSONArray("meta_types");
            if (metaTypes != null) {
                createMetaTypes(metaTypes, dms);
            }
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

    public static void createMetaTypes(JSONArray metaTypes, CoreService dms) throws Exception {
        for (int i = 0; i < metaTypes.length(); i++) {
            MetaTypeData metaTypeData = new MetaTypeData(metaTypes.getJSONObject(i));
            dms.createTopic(metaTypeData, null);        // clientContext=null
        }
    }

    public static void createTopicTypes(JSONArray topicTypes, CoreService dms) throws Exception {
        for (int i = 0; i < topicTypes.length(); i++) {
            TopicTypeData topicTypeData = new TopicTypeData(topicTypes.getJSONObject(i));
            dms.createTopicType(topicTypeData, null);   // clientContext=null
        }
    }

    public static void createAssociationTypes(JSONArray assocTypes, CoreService dms) throws Exception {
        for (int i = 0; i < assocTypes.length(); i++) {
            AssociationTypeData assocTypeData = new AssociationTypeData(assocTypes.getJSONObject(i));
            dms.createTopic(assocTypeData, null);       // clientContext=null
        }
    }

    public static void createTopics(JSONArray topics, CoreService dms) throws Exception {
        for (int i = 0; i < topics.length(); i++) {
            TopicData topicData = new TopicData(topics.getJSONObject(i));
            dms.createTopic(topicData, null);           // clientContext=null
        }
    }

    public static void createAssociations(JSONArray assocs, CoreService dms) throws Exception {
        for (int i = 0; i < assocs.length(); i++) {
            Association assoc = new Association(assocs.getJSONObject(i));
            dms.createAssociation(assoc, null);         // clientContext=null
        }
    }
}
