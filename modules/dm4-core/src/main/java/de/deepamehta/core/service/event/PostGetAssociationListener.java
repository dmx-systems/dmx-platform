package de.deepamehta.core.service.event;

import de.deepamehta.core.Association;
import de.deepamehta.core.service.EventListener;



public interface PostGetAssociationListener extends EventListener {

    void postGetAssociation(Association assoc);
}
