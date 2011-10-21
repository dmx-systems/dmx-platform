package de.deepamehta.plugins.geomaps;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.ClientContext;
import de.deepamehta.core.service.Directive;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.Plugin;
import de.deepamehta.core.util.JavaUtils;

import org.codehaus.jettison.json.JSONObject;

import java.net.URL;
import java.util.logging.Logger;



public class GeomapsPlugin extends Plugin {

    private static final String GEOCODER_URL = "http://maps.googleapis.com/maps/api/geocode/json?" +
        "address=%s&sensor=false";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // **************************************************
    // *** Core Hooks (called from DeepaMehta 4 Core) ***
    // **************************************************



    @Override
    public void postCreateHook(Topic topic, ClientContext clientContext) {
        if (topic.getTypeUri().equals("dm4.contacts.address")) {
            Address address = new Address(topic.getCompositeValue());
            if (!address.isEmpty()) {
                logger.info("### New " + address);
                address.geocode();
            } else {
                logger.info("### New empty address");
            }
        }
    }

    @Override
    public void postUpdateHook(Topic topic, TopicModel oldTopic, Directives directives) {
        if (topic.getTypeUri().equals("dm4.contacts.address")) {
            Address address    = new Address(topic.getCompositeValue());
            Address oldAddress = new Address(oldTopic.getCompositeValue());
            if (!address.equals(oldAddress)) {
                logger.info("### Address changed:" + address.changeReport(oldAddress));
                address.geocode();
            } else {
                logger.info("### Address not changed");
            }
        }
    }



    // ------------------------------------------------------------------------------------------------- Private Classes

    private class Address {

        String street, postalCode, city, country;

        // ---

        Address(CompositeValue address) {
            street     = address.getString("dm4.contacts.street");
            postalCode = address.getString("dm4.contacts.postal_code");
            city       = address.getString("dm4.contacts.city");
            country    = address.getString("dm4.contacts.country");
        }

        // ---

        void geocode() {
            URL url = null;
            try {
                String address = street + ", " + postalCode + " " + city + ", " + country;
                url = new URL(String.format(GEOCODER_URL, JavaUtils.encodeURIComponent(address)));
                JSONObject response = new JSONObject(JavaUtils.readTextURL(url));
                // check response status
                String status = response.getString("status");
                if (!status.equals("OK")) {
                    throw new RuntimeException(status);
                }
                // parse response
                JSONObject location = response.getJSONArray("results").getJSONObject(0).getJSONObject("geometry")
                    .getJSONObject("location");
                double lat = location.getDouble("lat");
                double lng = location.getDouble("lng");
                //
                logger.info("Geocoding \"" + address + "\"\n    " + url + "\n    => lat=" + lat + ", lng=" + lng);
            } catch (Exception e) {
                throw new RuntimeException("Geocoding failed (url=\"" + url + "\")", e);
            }
        }

        boolean isEmpty() {
            return street.equals("") && postalCode.equals("") && city.equals("") && country.equals("");
        }

        String changeReport(Address oldAddr) {
            StringBuilder report = new StringBuilder();
            if (!street.equals(oldAddr.street)) {
                report.append("\n    Street: \"" + oldAddr.street + "\" -> \"" + street + "\"");
            }
            if (!postalCode.equals(oldAddr.postalCode)) {
                report.append("\n    Postal Code: \"" + oldAddr.postalCode + "\" -> \"" + postalCode + "\"");
            }
            if (!city.equals(oldAddr.city)) {
                report.append("\n    City: \"" + oldAddr.city + "\" -> \"" + city + "\"");
            }
            if (!country.equals(oldAddr.country)) {
                report.append("\n    Country: \"" + oldAddr.country + "\" -> \"" + country + "\"");
            }
            return report.toString();
        }

        // === Java API ===

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Address)) {
                return false;
            }
            Address addr = (Address) o;
            return street.equals(addr.street) && postalCode.equals(addr.postalCode) &&
                city.equals(addr.city) && country.equals(addr.country);
        }

        @Override
        public int hashCode() {
            return (street + postalCode + city + country).hashCode();
        }

        @Override
        public String toString() {
            return "address (street=\"" + street + "\", postalCode=\"" + postalCode +
                "\", city=\"" + city + "\", country=\"" + country + "\")";
        }
    }
}
