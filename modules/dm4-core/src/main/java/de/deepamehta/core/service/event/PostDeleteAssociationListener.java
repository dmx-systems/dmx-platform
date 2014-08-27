package de.deepamehta.core.service.event;

import de.deepamehta.core.Association;
import de.deepamehta.core.service.EventListener;



public interface PostDeleteAssociationListener extends EventListener {

    void postDeleteAssociation(Association assoc);
}
