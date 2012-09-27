package de.deepamehta.core.model;



/**
 * A wrapper for the topic value (atomic, non-null). Supported value types are string, int, long, double, boolean.
 */
public class SimpleValue {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Object value;

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * Called by JAX-RS container to create a SimpleValue from a @PathParam or @QueryParam
     */
    public SimpleValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Tried to build a SimpleValue from a null String");
        }
        this.value = value;
    }

    public SimpleValue(int value) {
        this.value = value;
    }

    public SimpleValue(long value) {
        this.value = value;
    }

    public SimpleValue(double value) {
        this.value = value;
    }

    public SimpleValue(boolean value) {
        this.value = value;
    }

    public SimpleValue(Object value) {
        // check argument
        if (value == null) {
            throw new IllegalArgumentException("Tried to build a SimpleValue from a null Object");
        }
        if (!(value instanceof String || value instanceof Integer || value instanceof Long ||
              value instanceof Double || value instanceof Boolean)) {
            throw new IllegalArgumentException("Tried to build a SimpleValue from a " + value.getClass().getName() +
                " (expected are String, Integer, Long, Double, or Boolean)");
        }
        //
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

    public double doubleValue() {
        return (Double) value;
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
        if (!(o instanceof SimpleValue)) {
            return false;
        }
        return ((SimpleValue) o).value.equals(value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
