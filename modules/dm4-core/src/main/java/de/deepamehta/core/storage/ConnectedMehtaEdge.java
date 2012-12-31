package de.deepamehta.core.storage;

import de.deepamehta.core.storage.spi.MehtaEdge;



public class ConnectedMehtaEdge {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private MehtaEdge mehtaEdge;
    private MehtaEdge connectingMehtaEdge;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public ConnectedMehtaEdge(MehtaEdge mehtaEdge, MehtaEdge connectingMehtaEdge) {
        this.mehtaEdge = mehtaEdge;
        this.connectingMehtaEdge = connectingMehtaEdge;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public MehtaEdge getMehtaEdge() {
        return mehtaEdge;
    }

    public MehtaEdge getConnectingMehtaEdge() {
        return connectingMehtaEdge;
    }
}
