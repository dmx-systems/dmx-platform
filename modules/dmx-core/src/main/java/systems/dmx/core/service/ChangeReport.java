package systems.dmx.core.service;

import systems.dmx.core.JSONEnabled;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.RelatedTopicModel;

import java.util.List;

import org.codehaus.jettison.json.JSONObject;



public interface ChangeReport extends JSONEnabled {

    List<Change> getChanges(String compDefUri);

    class Change implements JSONEnabled {

        // TODO: type newValue RelatedTopicModel.
        // At the moment in case of a facet update we have just a TopicModel though.
        public TopicModel newValue;
        public RelatedTopicModel oldValue;

        public Change(TopicModel newValue, RelatedTopicModel oldValue) {
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
