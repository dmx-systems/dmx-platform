package systems.dmx.accesscontrol;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import systems.dmx.core.JSONEnabled;

import java.net.URI;

public class LogoutResponse implements JSONEnabled {

    public URI logoutRedirectUri;

    LogoutResponse(URI logoutRedirectUri) {
        this.logoutRedirectUri = logoutRedirectUri;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("logoutRedirectUri", logoutRedirectUri);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return json;
    }
}
