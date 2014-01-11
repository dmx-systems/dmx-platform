package de.deepamehta.core.service.event;

import de.deepamehta.core.Association;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.EventListener;



public interface PostCreateAssociationListener extends EventListener {

    void postCreateAssociation(Association assoc, ClientState clientState, Directives directives);
}
