package de.deepamehta.core.service.event;

import de.deepamehta.core.model.AssociationTypeModel;
import de.deepamehta.core.service.EventListener;



public interface PreCreateAssociationTypeListener extends EventListener {

    void preCreateAssociationType(AssociationTypeModel model);
}
