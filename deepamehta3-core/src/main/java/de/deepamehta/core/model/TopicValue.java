package de.deepamehta.core.model;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;



/**
 * A wrapper for a property value. Supported property types are String, int, long, boolean.
 * <p>
 * A TopicValue object may also represent "no-value".
 */
public class TopicValue {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Object value;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * Constructs a "no-value" respresenting value.
     */
    public TopicValue() {
    }

    /**
     * Called by JAX-RS container to create a TopicValue from a @PathParam or @QueryParam
     */
    public TopicValue(String value) {
        this.value = value;
    }

    public TopicValue(int value) {
        this.value = value;
    }

    public TopicValue(long value) {
        this.value = value;
    }

    public TopicValue(boolean value) {
        this.value = value;
    }

    public TopicValue(Object value) {
        this.value = value;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String toString() {
        return value != null ? value.toString() : null;
    }

    public int intValue() {
        return (Integer) value;
    }

    public long longValue() {
        return (Long) value;
    }

    public boolean booleanValue() {
        return (Boolean) value;
    }

    public Object value() {
        return value;
    }

    // ---

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TopicValue)) {
            return false;
        }
        TopicValue v = (TopicValue) o;
        return v.value == null ? value == null : value == null ? false : v.value.equals(value);
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
}
