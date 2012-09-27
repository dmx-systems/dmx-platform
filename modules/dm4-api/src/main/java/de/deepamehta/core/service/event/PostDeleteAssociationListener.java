package de.deepamehta.core.service.event;

import de.deepamehta.core.Association;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Listener;



public interface PostDeleteAssociationListener extends Listener {

    void postDeleteAssociation(Association assoc, Directives directives);
}
