package kr.douid.brand.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConversationTest {

    @Test
    void open_새상담_OPEN상태로_생성() {
        Conversation conversation = Conversation.open(1L);

        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.OPEN);
        assertThat(conversation.getClientIdentityId()).isEqualTo(1L);
        assertThat(conversation.getPublicId()).isNotNull();
    }

    @Test
    void close_OPEN상태에서_CLOSED로_전이() {
        Conversation conversation = Conversation.open(1L);

        conversation.close();

        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.CLOSED);
    }

    @Test
    void close_이미CLOSED_상태유지() {
        Conversation conversation = Conversation.open(1L);
        conversation.close();

        conversation.close();

        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.CLOSED);
    }

    @Test
    void isOwnedBy_소유자일치_true() {
        Conversation conversation = Conversation.open(1L);

        assertThat(conversation.isOwnedBy(1L)).isTrue();
    }

    @Test
    void isOwnedBy_소유자불일치_false() {
        Conversation conversation = Conversation.open(1L);

        assertThat(conversation.isOwnedBy(2L)).isFalse();
    }
}
