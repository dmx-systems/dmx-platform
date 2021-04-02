package systems.dmx.core.service;

import systems.dmx.core.JSONEnabled;
import systems.dmx.core.Topic;
import systems.dmx.core.RelatedTopic;

import java.util.List;

import org.codehaus.jettison.json.JSONObject;



public interface ChangeReport extends JSONEnabled {

    boolean hasChanges();

    List<Change> getChanges(String compDefUri);

    class Change implements JSONEnabled {

        // TODO: type newValue RelatedTopic.
        // At the moment in case of a facet update we have just a Topic though.
        public Topic newValue;
        public RelatedTopic oldValue;

        public Change(Topic newValue, RelatedTopic oldValue) {
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
