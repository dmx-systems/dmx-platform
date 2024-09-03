package systems.dmx.accountmanagement.configuration;

public enum ExpectedPasswordComplexity {
    NONE, SIMPLE, COMPLEX;

    public static ExpectedPasswordComplexity fromStringOrComplex(String value) {
        try {
            return ExpectedPasswordComplexity.valueOf(value.trim().toUpperCase());
        } catch (NullPointerException | IllegalArgumentException e) {
            return COMPLEX;
        }
    }
}
