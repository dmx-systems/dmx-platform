package de.deepamehta.core.util;

import java.util.concurrent.Callable;



public class ContextTracker {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private ThreadLocal<Integer> trackingLevel = new ThreadLocal() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };

    // -------------------------------------------------------------------------------------------------- Public Methods

    public <V> V run(Callable<V> callable) throws Exception {
        int level = trackingLevel.get();
        try {
            trackingLevel.set(level + 1);
            return callable.call();     // throws exception
        } finally {
            trackingLevel.set(level);
        }
    }

    public boolean runsInTrackedContext() {
        return trackingLevel.get() > 0;
    }
}
