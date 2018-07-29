package systems.dmx.accesscontrol;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;



class AnonymousAccessFilter {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private List<String> prefixesRead;
    private List<String> prefixesWrite;

    // ---------------------------------------------------------------------------------------------------- Constructors

    AnonymousAccessFilter(String settingRead, String settingWrite) {
        this.prefixesRead  = initPrefixes(settingRead);
        this.prefixesWrite = initPrefixes(settingWrite);
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    boolean isAnonymousAccessAllowed(HttpServletRequest request) {
        return request.getMethod().equals("GET") ?
            prefixMatch(request, prefixesRead) :
            prefixMatch(request, prefixesWrite);
    }

    // ---

    String dumpReadSetting() {
        return dumpSetting(prefixesRead);
    }

    String dumpWriteSetting() {
        return dumpSetting(prefixesWrite);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private List<String> initPrefixes(String setting) {
        if (setting.equals("ALL")) {
            return null;
        } else if (setting.equals("NONE")) {
            return new ArrayList();
        } else {
            String[] p = setting.split(",\\s*");    // ignore whitespace after comma
            List<String> prefixes = new ArrayList();
            for (int i = 0; i < p.length; i++) {
                prefixes.add(p[i]);
            }
            return prefixes;
        }
    }

    private boolean prefixMatch(HttpServletRequest request, List<String> prefixes) {
        if (prefixes == null) {
            return true;
        }
        for (String prefix : prefixes) {
            if (request.getRequestURI().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String dumpSetting(List<String> prefixes) {
        if (prefixes == null) {
            return "ALL";
        } else if (prefixes.isEmpty()) {
            return "NONE";
        } else {
            return prefixes.toString();
        }
    }
}
