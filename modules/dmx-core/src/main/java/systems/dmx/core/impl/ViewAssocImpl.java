package systems.dmx.core.impl;

import systems.dmx.core.model.AssociationModel;
import systems.dmx.core.model.topicmaps.ViewAssoc;
import systems.dmx.core.model.topicmaps.ViewProps;

import org.codehaus.jettison.json.JSONObject;



// TODO: rethink inheritance. Can we have a common "ObjectViewModel" for both, topics and assocs?
// Is this a case for Java 8 interfaces, which can have a default implementation?
class ViewAssocImpl extends AssociationModelImpl implements ViewAssoc {

    // --- Instance Variables ---

    private ViewProps viewProps;

    // --- Constructors ---

    ViewAssocImpl(AssociationModelImpl assoc, ViewProps viewProps) {
        super(assoc);
        this.viewProps = viewProps;
    }

    // --- Public Methods ---

    @Override
    public JSONObject toJSON() {
        try {
            return super.toJSON().put("viewProps", viewProps.toJSON());
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }
}
