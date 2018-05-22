package de.deepamehta.core.util;

import java.util.ArrayList;



public class IdList extends ArrayList<Long> {

    public IdList(String ids) {
        for (String id : ids.split(",")) {
            add(Long.parseLong(id));
        }
    }
}
