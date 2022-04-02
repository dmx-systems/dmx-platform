package systems.dmx.core.util;

import systems.dmx.core.Identifiable;
import java.util.ArrayList;



public class IdList extends ArrayList<Long> {

    public IdList() {
    }

    public IdList(String ids) {
        for (String id : ids.split(",")) {
            add(Long.parseLong(id));
        }
    }

    public IdList(Iterable<? extends Identifiable> items) {
        items.forEach(item -> add(item.getId()));
    }
}
