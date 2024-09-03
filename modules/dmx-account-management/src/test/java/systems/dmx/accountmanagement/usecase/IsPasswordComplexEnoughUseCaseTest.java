package systems.dmx.accountmanagement.usecase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import systems.dmx.accountmanagement.configuration.ExpectedPasswordComplexity;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class IsPasswordComplexEnoughUseCaseTest {

    private static Stream<Arguments> weakPasswordParams() {
        return Stream.of(
                Arguments.of("xX1¤"), // too short
                Arguments.of("AAAAAAa6"),    // no special char
                Arguments.of("AAAAAAa%"),    // no digit
                Arguments.of("AAAAAA6%"),    // no lower-case letter
                Arguments.of("aaaaaa6%")    // no upper-case letter
        );
    }

    private static Stream<Arguments> malformedPasswordParams() {
        return Stream.of(
                Arguments.of("ABCDEFa6%"),    // simple char sequence
                Arguments.of("Qwertza6%"),    // simple char sequence
                Arguments.of("12345Aa6%"),    // simple digit sequence
                Arguments.of("FBD JDFa6%")    // whitespace
        );
    }

    private static Stream<Arguments> simplePasswordParams() {
        return Stream.of(
                Arguments.of("ABCDEFa6%"),    // simple char sequence
                Arguments.of("Qwertza6%"),    // simple char sequence
                Arguments.of("12345Aa6%"),    // simple digit sequence
                Arguments.of("AAAAAAa6"),    // no special char
                Arguments.of("AAAAAAa%"),    // no digit
                Arguments.of("AAAAAA6%"),    // no lower-case letter
                Arguments.of("aaaaaa6%")    // no upper-case letter
        );
    }

    private static Stream<Arguments> strongPasswordParams() {
        return Stream.of(
                Arguments.of("AFJEKFa6%"),
                Arguments.of("Qgervza6%"),
                Arguments.of("9a¤6f45Aa6"),
                Arguments.of("F1BD4JDFa%"),
                Arguments.of("4pKw$Rs5x")
        );
    }

    private static Stream<Arguments> tooLongPasswordParams() {
        return Stream.of(
                Arguments.of("öFlGKFa6%9a¤4f45-") // 17 characters
        );
    }

    private final int minPasswordLength = 8;

    private final int maxPasswordLength = 16;

    private final IsPasswordComplexEnoughUseCase subject = new IsPasswordComplexEnoughUseCase(minPasswordLength, maxPasswordLength);

    @ParameterizedTest(name = "{displayName} with weak password {0}")
    @MethodSource("weakPasswordParams")
    @DisplayName("invoke() should return true when expected password complexity is NONE")
    void invoke_should_return_true_when_complexity_none_for_weak_passwords(String givenPassword) {
        // when:
        boolean result = subject.invoke(ExpectedPasswordComplexity.NONE, givenPassword);

        // then:
        assertThat(result).isTrue();
    }

    @ParameterizedTest(name = "{displayName} with malformed password {0}")
    @MethodSource("malformedPasswordParams")
    @DisplayName("invoke() should return true when expected password complexity is NONE")
    void invoke_should_return_true_when_complexity_none_for_malformed_passwords(String givenPassword) {
        // when:
        boolean result = subject.invoke(ExpectedPasswordComplexity.NONE, givenPassword);

        // then:
        assertThat(result).isTrue();
    }

    @ParameterizedTest(name = "{displayName} with strong password {0}")
    @MethodSource("strongPasswordParams")
    @DisplayName("invoke() should return true when expected password complexity is NONE")
    void invoke_should_return_true_when_complexity_none_for_strong_passwords(String givenPassword) {
        // when:
        boolean result = subject.invoke(ExpectedPasswordComplexity.NONE, givenPassword);

        // then:
        assertThat(result).isTrue();
    }

    @ParameterizedTest(name = "{displayName} with too long password {0}")
    @MethodSource("tooLongPasswordParams")
    @DisplayName("invoke() should return true when expected password complexity is NONE")
    void invoke_should_return_true_when_complexity_none_for_too_long_passwords(String givenPassword) {
        // when:
        boolean result = subject.invoke(ExpectedPasswordComplexity.NONE, givenPassword);

        // then:
        assertThat(result).isTrue();
    }

    @ParameterizedTest(name = "{displayName} with weak password {0}")
    @MethodSource("weakPasswordParams")
    @DisplayName("invoke() should return false when expected password complexity is COMPLEX")
    void invoke_should_return_false_when_complexity_complex_for_weak_passwords(String givenPassword) {
        // when:
        boolean result = subject.invoke(ExpectedPasswordComplexity.COMPLEX, givenPassword);

        // then:
        assertThat(result).isFalse();
    }

    @ParameterizedTest(name = "{displayName} with malformed password {0}")
    @MethodSource("malformedPasswordParams")
    @DisplayName("invoke() should return false when expected password complexity is COMPLEX")
    void invoke_should_return_false_when_complexity_complex_for_malformed_passwords(String givenPassword) {
        // when:
        boolean result = subject.invoke(ExpectedPasswordComplexity.COMPLEX, givenPassword);

        // then:
        assertThat(result).isFalse();
    }

    @ParameterizedTest(name = "{displayName} with strong password {0}")
    @MethodSource("strongPasswordParams")
    @DisplayName("invoke() should return true when expected password complexity is SIMPLE")
    void invoke_should_return_true_when_complexity_simple_for_strong_passwords(String givenPassword) {
        // when:
        boolean result = subject.invoke(ExpectedPasswordComplexity.SIMPLE, givenPassword);

        // then:
        assertThat(result).isTrue();
    }

    @ParameterizedTest(name = "{displayName} with strong password {0}")
    @MethodSource("strongPasswordParams")
    @DisplayName("invoke() should return true when expected password complexity is COMPLEX")
    void invoke_should_return_true_when_complexity_complex_for_strong_passwords(String givenPassword) {
        // when:
        boolean result = subject.invoke(ExpectedPasswordComplexity.COMPLEX, givenPassword);

        // then:
        assertThat(result).isTrue();
    }

    @ParameterizedTest(name = "{displayName} with simple password {0}")
    @MethodSource("simplePasswordParams")
    @DisplayName("invoke() should return true when expected password complexity is SIMPLE")
    void invoke_should_return_true_when_complexity_simple_for_simple_passwords(String givenPassword) {
        // when:
        boolean result = subject.invoke(ExpectedPasswordComplexity.SIMPLE, givenPassword);

        // then:
        assertThat(result).isTrue();
    }

    @ParameterizedTest(name = "{displayName} with simple password {0}")
    @MethodSource("simplePasswordParams")
    @DisplayName("invoke() should return false when expected password complexity is COMPLEX")
    void invoke_should_return_false_when_complexity_complex_for_simple_passwords(String givenPassword) {
        // when:
        boolean result = subject.invoke(ExpectedPasswordComplexity.COMPLEX, givenPassword);

        // then:
        assertThat(result).isFalse();
    }

    @ParameterizedTest(name = "{displayName} with too long password {0}")
    @MethodSource("tooLongPasswordParams")
    @DisplayName("invoke() should return false when expected password complexity is SIMPE")
    void invoke_should_return_false_when_complexity_simple_for_too_long_passwords(String givenPassword) {
        // when:
        boolean result = subject.invoke(ExpectedPasswordComplexity.SIMPLE, givenPassword);

        // then:
        assertThat(result).isFalse();
    }

    @ParameterizedTest(name = "{displayName} with too long password {0}")
    @MethodSource("tooLongPasswordParams")
    @DisplayName("invoke() should return false when expected password complexity is COMPLEX")
    void invoke_should_return_false_when_complexity_complex_for_too_long_passwords(String givenPassword) {
        // when:
        boolean result = subject.invoke(ExpectedPasswordComplexity.COMPLEX, givenPassword);

        // then:
        assertThat(result).isFalse();
    }

}