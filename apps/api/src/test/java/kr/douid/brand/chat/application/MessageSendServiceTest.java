package kr.douid.brand.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.douid.brand.chat.application.query.MessageView;
import kr.douid.brand.chat.domain.Conversation;
import kr.douid.brand.chat.domain.ConversationAccessDeniedException;
import kr.douid.brand.chat.domain.ConversationClosedException;
import kr.douid.brand.chat.domain.MessageRepository;

@ExtendWith(MockitoExtension.class)
class MessageSendServiceTest {

    @Mock
    private ConversationLookupService conversationLookupService;

    @Mock
    private MessageRepository messageRepository;

    private MessageSendService messageSendService;

    @BeforeEach
    void setUp() {
        messageSendService = new MessageSendService(conversationLookupService, messageRepository);
    }

    @Test
    void send_활성상담_유효ownership_저장성공() {
        Conversation conversation = Conversation.open(1L);
        given(conversationLookupService.getOwned(conversation.getPublicId(), Optional.of(1L)))
                .willReturn(conversation);
        given(messageRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        MessageView result = messageSendService.send(conversation.getPublicId(), Optional.of(1L), "안녕하세요");

        assertThat(result.content()).isEqualTo("안녕하세요");
    }

    @Test
    void send_종료된상담_예외() {
        Conversation conversation = Conversation.open(1L);
        conversation.close();
        given(conversationLookupService.getOwned(conversation.getPublicId(), Optional.of(1L)))
                .willReturn(conversation);

        assertThatThrownBy(() ->
                messageSendService.send(conversation.getPublicId(), Optional.of(1L), "안녕하세요"))
                .isInstanceOf(ConversationClosedException.class);
    }

    @Test
    void send_ownership불일치_예외() {
        Conversation conversation = Conversation.open(1L);
        given(conversationLookupService.getOwned(conversation.getPublicId(), Optional.of(2L)))
                .willThrow(new ConversationAccessDeniedException());

        assertThatThrownBy(() ->
                messageSendService.send(conversation.getPublicId(), Optional.of(2L), "안녕하세요"))
                .isInstanceOf(ConversationAccessDeniedException.class);
    }
}
