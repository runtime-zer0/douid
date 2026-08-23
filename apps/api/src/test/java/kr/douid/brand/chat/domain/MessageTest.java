package kr.douid.brand.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MessageTest {

    @Test
    void write_메시지_conversationId와_content로_생성() {
        Message message = Message.write(1L, "안녕하세요");

        assertThat(message.getConversationId()).isEqualTo(1L);
        assertThat(message.getContent()).isEqualTo("안녕하세요");
    }
}
