package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.topicmaps.AssociationViewModel;
import de.deepamehta.core.model.topicmaps.ViewProperties;

import org.codehaus.jettison.json.JSONObject;



// TODO: rethink inheritance. Can we have a common "ObjectViewModel" for both, topics and assocs?
// Is this a case for Java 8 interfaces, which can have a default implementation?
class AssociationViewModelImpl extends AssociationModelImpl implements AssociationViewModel {

    // --- Instance Variables ---

    private ViewProperties viewProps;

    // --- Constructors ---

    AssociationViewModelImpl(AssociationModelImpl assoc, ViewProperties viewProps) {
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
