package de.deepamehta.mehtagraph;

import de.deepamehta.mehtagraph.spi.MehtaEdge;



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
