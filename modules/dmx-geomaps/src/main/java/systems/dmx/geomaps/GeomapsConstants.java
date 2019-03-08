package systems.dmx.geomaps;



public interface GeomapsConstants {

    // topic types
    static final String GEO_COORDINATE       = "dmx.geomaps.geo_coordinate";
    static final String GEO_COORDINATE_FACET = "dmx.geomaps.geo_coordinate_facet";
    static final String LONGITUDE            = "dmx.geomaps.longitude";
    static final String LATITUDE             = "dmx.geomaps.latitude";

    // content assoc
    static final String GEOMAP_CONTEXT       = "dmx.geomaps.geomap_context";
    static final String ROLE_TYPE_GEOMAP     = "dmx.core.default";
    static final String ROLE_TYPE_CONTENT    = "dmx.core.default";

    // geomap props
    static final String PROP_LONGITUDE       = "dmx.geomaps.longitude";
    static final String PROP_LATITUDE        = "dmx.geomaps.latitude";
    static final String PROP_ZOOM            = "dmx.geomaps.zoom";
}
