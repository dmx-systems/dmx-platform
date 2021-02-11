package systems.dmx.core.impl;

import systems.dmx.core.JSONEnabled;
import systems.dmx.core.service.ChangeReport;
import systems.dmx.core.util.DMXUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;



class ChangeReportImpl implements ChangeReport, JSONEnabled {

    Map<String, List<Change>> changes = new HashMap();

    // TODO: both RelatedTopicModelImpl
    void add(String compDefUri, TopicModelImpl newValue, RelatedTopicModelImpl oldValue) {
        List<Change> l = changes.get(compDefUri);
        if (l == null) {
            l = new ArrayList();
            changes.put(compDefUri, l);
        }
        l.add(new Change(newValue, oldValue));
    }

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            for (String compDefUri : changes.keySet()) {
                o.put(compDefUri, DMXUtils.toJSONArray(changes.get(compDefUri)));
            }
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    // TODO: copy in DMXObjectModelImpl
    // Can we use Java 8 and put this in the JSONEnabled interface?
    @Override
    public String toString() {
        try {
            return getClass().getSimpleName() + " " + toJSON().toString(4);
        } catch (Exception e) {
            throw new RuntimeException("Prettyprinting failed", e);
        }
    }

    class Change implements JSONEnabled {

        TopicModelImpl newValue;
        RelatedTopicModelImpl oldValue;

        Change(TopicModelImpl newValue, RelatedTopicModelImpl oldValue) {
            this.newValue = newValue;
            this.oldValue = oldValue;
        }

        @Override
        public JSONObject toJSON() {
            try {
                return new JSONObject()
                    .put("newValue", newValue != null ? newValue.toJSON() : JSONObject.NULL)
                    .put("oldValue", oldValue != null ? oldValue.toJSON() : JSONObject.NULL);
            } catch (Exception e) {
                throw new RuntimeException("Serialization failed", e);
            }
        }
    }
}
