package systems.dmx.core.impl;

import systems.dmx.core.JSONEnabled;
import systems.dmx.core.service.ChangeReport;
import systems.dmx.core.util.DMXUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;



class ChangeReportImpl implements ChangeReport {

    private Map<String, List<Change>> changes = new HashMap();

    @Override
    public boolean hasChanges() {
        return !changes.isEmpty();
    }

    @Override
    public List<Change> getChanges(String compDefUri) {
        return changes.get(compDefUri);
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

    @Override
    public String toString() {
        return dump();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    // TODO: 2xRelatedTopicImpl
    void add(String compDefUri, TopicImpl newValue, RelatedTopicImpl oldValue) {
        List<Change> l = changes.get(compDefUri);
        if (l == null) {
            l = new ArrayList();
            changes.put(compDefUri, l);
        }
        l.add(new Change(newValue, oldValue));
    }
}
