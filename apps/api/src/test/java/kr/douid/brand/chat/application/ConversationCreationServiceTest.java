package kr.douid.brand.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.douid.brand.chat.domain.Conversation;
import kr.douid.brand.chat.domain.ConversationRepository;
import kr.douid.brand.chat.domain.ConversationStatus;

@ExtendWith(MockitoExtension.class)
class ConversationCreationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    private ConversationCreationService conversationCreationService;

    @BeforeEach
    void setUp() {
        conversationCreationService = new ConversationCreationService(conversationRepository);
    }

    @Test
    void create_저장된_OPEN상태_상담을반환() {
        Long clientIdentityId = 1L;
        given(conversationRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        Conversation conversation = conversationCreationService.create(clientIdentityId);

        assertThat(conversation.getClientIdentityId()).isEqualTo(clientIdentityId);
        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.OPEN);
        verify(conversationRepository).save(any());
    }
}
