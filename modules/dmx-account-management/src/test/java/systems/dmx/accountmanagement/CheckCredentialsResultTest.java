package systems.dmx.accountmanagement;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import systems.dmx.core.Topic;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class CheckCredentialsResultTest {

    @Test
    @DisplayName("success() should create instance with topic set and success set")
    void success_should_create_instance_with_topic_and_success_set() {
        // given:
        Topic topic = mock();

        // when:
        CheckCredentialsResult result = CheckCredentialsResult.success(topic);

        // then:
        assertThat(result.usernameTopic).isEqualTo(topic);
        assertThat(result.success).isTrue();
    }

    @Test
    @DisplayName("success() should throw IllegalArgumentException when no topic provided")
    void success_should_throw() {
        // when:
        ThrowableAssert.ThrowingCallable closure = () -> CheckCredentialsResult.success(null);

        // then:
        assertThatThrownBy(closure).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("lookupOrCreationRequired() should create instance with no topic set but success set")
    void lookupOrCreationRequired_should_create_instance_with_no_topic_set_and_success_set() {
        // when:
        CheckCredentialsResult result = CheckCredentialsResult.lookupOrCreationRequired();

        // then:
        assertThat(result.usernameTopic).isNull();
        assertThat(result.success).isTrue();
    }

    @Test
    @DisplayName("failed() should create instance with no topic set and success cleared")
    void failed_should_create_instance_with_no_topic_set_and_success_cleared() {
        // when:
        CheckCredentialsResult result = CheckCredentialsResult.failed();

        // then:
        assertThat(result.usernameTopic).isNull();
        assertThat(result.success).isFalse();
    }
}
