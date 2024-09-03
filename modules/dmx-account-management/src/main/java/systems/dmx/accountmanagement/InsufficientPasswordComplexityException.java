package systems.dmx.accountmanagement;

import systems.dmx.accountmanagement.configuration.ExpectedPasswordComplexity;

public class InsufficientPasswordComplexityException extends RuntimeException {
    public final int expectedMinPasswordLength;
    public final int expectedMaxPasswordLength;
    public final ExpectedPasswordComplexity expectedPasswordComplexity;
    public InsufficientPasswordComplexityException(
            int expectedMinPasswordLength,
            int expectedMaxPasswordLength,
            ExpectedPasswordComplexity expectedPasswordComplexity
    ) {
        this.expectedMinPasswordLength = expectedMinPasswordLength;
        this.expectedMaxPasswordLength = expectedMaxPasswordLength;
        this.expectedPasswordComplexity = expectedPasswordComplexity;
    }
}
