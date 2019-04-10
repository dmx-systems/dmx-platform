package systems.dmx.topicmaps;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



public class TopicCoords implements Iterable<TopicCoords.Entry> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private List<Entry> entries = new ArrayList();

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    public TopicCoords(JSONObject topicCoords) {
        try {
            JSONArray entries = topicCoords.getJSONArray("topicCoords");
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.getJSONObject(i);
                this.entries.add(new Entry(
                    entry.getLong("topicId"),
                    entry.getInt("x"),
                    entry.getInt("y")
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing TopicCoords failed (JSONObject=" + topicCoords + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public Iterator<Entry> iterator() {
        return entries.iterator();
    }

    public class Entry {

        public long topicId;
        public int x;
        public int y;

        private Entry(long topicId, int x, int y) {
            this.topicId = topicId;
            this.x = x;
            this.y = y;
        }
    }
}
