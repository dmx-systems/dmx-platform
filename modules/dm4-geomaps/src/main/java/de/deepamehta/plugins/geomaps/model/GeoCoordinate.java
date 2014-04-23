package de.deepamehta.plugins.geomaps.model;



public class GeoCoordinate {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    public double lon, lat;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public GeoCoordinate(double lon, double lat) {
        this.lon = lon;
        this.lat = lat;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String toString() {
        return "long=" + lon + ", lat=" + lat;
    }
}
