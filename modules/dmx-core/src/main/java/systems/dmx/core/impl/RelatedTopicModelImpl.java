package systems.dmx.core.impl;

import systems.dmx.core.RelatedTopic;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.TopicModel;

import org.codehaus.jettison.json.JSONObject;



class RelatedTopicModelImpl extends TopicModelImpl implements RelatedTopicModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private AssocModelImpl relatingAssoc;

    // ---------------------------------------------------------------------------------------------------- Constructors

    RelatedTopicModelImpl(TopicModelImpl topic, AssocModelImpl relatingAssoc) {
        super(topic);
        this.relatingAssoc = relatingAssoc;
    }

    RelatedTopicModelImpl(RelatedTopicModelImpl relatedTopic) {
        super(relatedTopic);
        this.relatingAssoc = relatedTopic.getRelatingAssoc();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public AssocModelImpl getRelatingAssoc() {
        return relatingAssoc;
    }



    // === Serialization ===

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject o = super.toJSON();
            // Note: the relating association might be uninitialized and thus not serializable.
            // This is the case at least for enrichments which have no underlying topics (e.g. timestamps).
            // ### TODO: remodel enrichments? Don't put them in a child topics model but in a proprietary field?
            if (relatingAssoc.getPlayer1() != null) {
                o.put("assoc", relatingAssoc.toJSON());
            }
            //
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }



    // === Java API ===

    @Override
    public RelatedTopicModel clone() {
        try {
            return (RelatedTopicModel) super.clone();
        } catch (Exception e) {
            throw new RuntimeException("Cloning a RelatedTopicModel failed", e);
        }
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    @Override
    String className() {
        return "related topic";
    }

    @Override
    RelatedTopicImpl instantiate() {
        return new RelatedTopicImpl(this, pl);
    }
}
