package systems.dmx.accesscontrol.identityproviderredirect;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import systems.dmx.core.JSONEnabled;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

public class IdentityProviderRedirectConfiguration implements JSONEnabled {

    private final String name;

    private final String label;

    private final URI imageUri;

    private final URI uri;

    public IdentityProviderRedirectConfiguration(IdentityProviderRedirectAdapter adapter) {
        this.name = adapter.getName();
        this.label = adapter.getLabel();
        this.imageUri = adapter.getImageUri();
        try {
            this.uri = new URI(
                    String.format("%s/access-control/identity-provider-redirect/uri?name=%s",
                            System.getProperty("dmx.host.url"),
                            URLEncoder.encode(name, "UTF-8")));
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JSONObject toJSON() {
        try {
            JSONObject json = new JSONObject();
            json.put("name", name);
            json.put("label", label);
            json.put("imageUri", imageUri);
            json.put("uri", uri);
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
