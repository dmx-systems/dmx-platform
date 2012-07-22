package de.deepamehta.core.service.listener;

import de.deepamehta.core.Association;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Listener;



public interface PostRetypeAssociationListener extends Listener {

    void postRetypeAssociation(Association assoc, String oldTypeUri, Directives directives);
}
