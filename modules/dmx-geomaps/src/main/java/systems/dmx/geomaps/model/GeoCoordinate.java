package systems.dmx.geomaps.model;



public class GeoCoordinate {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    public double lon, lat;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public GeoCoordinate(double lon, double lat) {
        this.lon = lon;
        this.lat = lat;
    }

    /**
     * Called by JAX-RS container to create a GeoCoordinate from a @QueryParam
     */
    public GeoCoordinate(String lonLat) {
        String[] str = lonLat.split(",");
        this.lon = Double.valueOf(str[0]);
        this.lat = Double.valueOf(str[1]);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String toString() {
        return "long=" + lon + ", lat=" + lat;
    }
}
