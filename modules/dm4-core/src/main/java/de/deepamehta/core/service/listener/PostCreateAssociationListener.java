package de.deepamehta.core.service.listener;

import de.deepamehta.core.Association;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Listener;



public interface PostCreateAssociationListener extends Listener {

    void postCreateAssociation(Association assoc, ClientState clientState, Directives directives);
}
