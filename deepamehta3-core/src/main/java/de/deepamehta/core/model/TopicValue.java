package de.deepamehta.core.model;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;



/**
 * A wrapper for the topic value (atomic, non-null). Supported value types are string, int, long, boolean.
 */
public class TopicValue {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Object value;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * Called by JAX-RS container to create a TopicValue from a @PathParam or @QueryParam
     */
    public TopicValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Tried to build a TopicValue from a null String");
        }
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
        if (value == null) {
            throw new IllegalArgumentException("Tried to build a TopicValue from a null Object");
        }
        this.value = value;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String toString() {
        return value.toString();
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
        return ((TopicValue) o).value.equals(value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
