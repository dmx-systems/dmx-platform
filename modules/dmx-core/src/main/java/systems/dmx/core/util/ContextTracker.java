package systems.dmx.core.util;

import java.util.concurrent.Callable;



public class ContextTracker {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private ThreadLocal<Long> tl = new ThreadLocal();

    // -------------------------------------------------------------------------------------------------- Public Methods

    public <V> V run(long value, Callable<V> callable) throws Exception {
        Long _value = null;
        try {
            _value = tl.get();
            tl.set(value);
            return callable.call();     // throws exception
        } finally {
            tl.set(_value);
        }
    }

    public Long getValue() {
        return tl.get();
    }
}
