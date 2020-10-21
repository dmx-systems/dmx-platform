package systems.dmx.core.impl;

import systems.dmx.core.model.DMXObjectModel;
import systems.dmx.core.model.RelatedTopicModel;

import org.codehaus.jettison.json.JSONObject;



public class RelatedTopicModelImpl extends TopicModelImpl implements RelatedTopicModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private AssocModelImpl relatingAssoc;
    private DMXObjectModelImpl otherObject;

    // ---------------------------------------------------------------------------------------------------- Constructors

    RelatedTopicModelImpl(TopicModelImpl topic, AssocModelImpl relatingAssoc) {
        this(topic, relatingAssoc, null);
    }

    RelatedTopicModelImpl(TopicModelImpl topic, AssocModelImpl relatingAssoc, DMXObjectModelImpl otherObject) {
        super(topic);
        this.relatingAssoc = relatingAssoc;
        this.otherObject = otherObject;
    }

    RelatedTopicModelImpl(RelatedTopicModelImpl relatedTopic) {
        super(relatedTopic);
        this.relatingAssoc = relatedTopic.getRelatingAssoc();
        this.otherObject = relatedTopic.getOtherDMXObject();
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public AssocModelImpl getRelatingAssoc() {
        return relatingAssoc;
    }

    @Override
    public <M extends DMXObjectModel> M getOtherDMXObject() {
        return (M) otherObject;
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
                if (otherObject != null) {
                    o.put("player", otherObject.toJSON());
                }
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

    /**
     * Overrides DMXObjectModelImpl
     * ### Copy in RelatedAssocModelImpl
     */
    @Override
    boolean isReadable() {
        try {
            return super.isReadable() && getRelatingAssoc().isReadable();
        } catch (Exception e) {
            throw new RuntimeException("Checking related topic READability failed (" + this + ")", e);
        }
    }

    @Override
    String className() {
        return "related topic";
    }

    @Override
    RelatedTopicImpl instantiate() {
        return new RelatedTopicImpl(this, al);
    }
}
