package systems.dmx.core.impl;

import systems.dmx.core.RelatedTopic;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.RelatedTopicModel;
import systems.dmx.core.model.TopicModel;

import org.codehaus.jettison.json.JSONObject;



public class RelatedTopicModelImpl extends TopicModelImpl implements RelatedTopicModel {

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
            return super.toJSON().put("assoc", relatingAssoc.toJSON());
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }



    // === Java API ===

    @Override
    public RelatedTopicModel clone() {
        try {
            return (RelatedTopicModel) super.clone();       // FIXME: clone rel-assoc explicitly
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
