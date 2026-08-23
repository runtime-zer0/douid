package kr.douid.brand.chat.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import kr.douid.brand.chat.domain.Conversation;
import kr.douid.brand.chat.domain.ConversationAccessDeniedException;

class ConversationOwnershipValidatorTest {

    private final ConversationOwnershipValidator validator = new ConversationOwnershipValidator();

    @Test
    void validate_소유자일치_예외없음() {
        Conversation conversation = Conversation.open(1L);

        assertThatCode(() -> validator.validate(conversation, Optional.of(1L)))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_소유자불일치_접근거부예외() {
        Conversation conversation = Conversation.open(1L);

        assertThatThrownBy(() -> validator.validate(conversation, Optional.of(2L)))
                .isInstanceOf(ConversationAccessDeniedException.class);
    }

    @Test
    void validate_미인증_접근거부예외() {
        Conversation conversation = Conversation.open(1L);

        assertThatThrownBy(() -> validator.validate(conversation, Optional.empty()))
                .isInstanceOf(ConversationAccessDeniedException.class);
    }
}
