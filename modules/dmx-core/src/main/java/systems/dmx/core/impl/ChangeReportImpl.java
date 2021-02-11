package systems.dmx.core.impl;

import systems.dmx.core.service.ChangeReport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class ChangeReportImpl implements ChangeReport {

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

    class Change {

        TopicModelImpl newValue;
        RelatedTopicModelImpl oldValue;

        Change(TopicModelImpl newValue, RelatedTopicModelImpl oldValue) {
            this.newValue = newValue;
            this.oldValue = oldValue;
        }
    }
}
