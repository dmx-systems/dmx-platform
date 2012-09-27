package de.deepamehta.core.service.event;

import de.deepamehta.core.Association;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Listener;



public interface PreDeleteAssociationListener extends Listener {

    void preDeleteAssociation(Association assoc, Directives directives);
}
