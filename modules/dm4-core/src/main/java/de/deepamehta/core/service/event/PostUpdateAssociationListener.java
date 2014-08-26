package de.deepamehta.core.service.event;

import de.deepamehta.core.Association;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.EventListener;



public interface PostUpdateAssociationListener extends EventListener {

    void postUpdateAssociation(Association assoc, AssociationModel oldModel, Directives directives);
}
