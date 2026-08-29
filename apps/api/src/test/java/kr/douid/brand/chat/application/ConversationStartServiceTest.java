package kr.douid.brand.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import kr.douid.brand.chat.application.command.StartConversationResult;
import kr.douid.brand.chat.domain.Conversation;
import kr.douid.brand.chat.domain.ConversationRepository;
import kr.douid.brand.chat.domain.ConversationStatus;
import kr.douid.brand.client.application.ClientIdentityProvisioningService;
import kr.douid.brand.client.application.ClientIdentityProvisioningService.ProvisionedClient;

@ExtendWith(MockitoExtension.class)
class ConversationStartServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationCreationService conversationCreationService;

    @Mock
    private ClientIdentityProvisioningService clientIdentityProvisioningService;

    private ConversationStartService conversationStartService;

    @BeforeEach
    void setUp() {
        conversationStartService = new ConversationStartService(
                conversationRepository, conversationCreationService, clientIdentityProvisioningService);
    }

    @Test
    void start_인증됨_활성상담있음_기존상담반환하고_신규생성없음() {
        Long clientIdentityId = 1L;
        Conversation existing = Conversation.open(clientIdentityId);
        given(conversationRepository.findOpenByClientIdentityId(clientIdentityId))
                .willReturn(Optional.of(existing));

        StartConversationResult result = conversationStartService.start(Optional.of(clientIdentityId));

        assertThat(result.resumed()).isTrue();
        assertThat(result.rawClientToken()).isNull();
        assertThat(result.conversationPublicId()).isEqualTo(existing.getPublicId());
        verify(conversationCreationService, never()).create(any());
        verify(clientIdentityProvisioningService, never()).provision();
    }

    @Test
    void start_미인증_신규상담주체발급후_새상담생성() {
        given(clientIdentityProvisioningService.provision())
                .willReturn(new ProvisionedClient(1L, "raw-token"));
        given(conversationRepository.findOpenByClientIdentityId(1L)).willReturn(Optional.empty());
        given(conversationCreationService.create(1L)).willReturn(Conversation.open(1L));

        StartConversationResult result = conversationStartService.start(Optional.empty());

        assertThat(result.resumed()).isFalse();
        assertThat(result.rawClientToken()).isEqualTo("raw-token");
        assertThat(result.status()).isEqualTo(ConversationStatus.OPEN);
        verify(conversationCreationService).create(1L);
    }

    @Test
    void start_인증됨_활성상담없음_새상담만생성하고_신규발급없음() {
        given(conversationRepository.findOpenByClientIdentityId(1L)).willReturn(Optional.empty());
        given(conversationCreationService.create(1L)).willReturn(Conversation.open(1L));

        StartConversationResult result = conversationStartService.start(Optional.of(1L));

        assertThat(result.resumed()).isFalse();
        assertThat(result.rawClientToken()).isNull();
        verify(clientIdentityProvisioningService, never()).provision();
        verify(conversationCreationService).create(1L);
    }

    @Test
    void start_저장시점경쟁조건으로_unique제약위반_재조회한기존상담반환() {
        Long clientIdentityId = 1L;
        Conversation racedConversation = Conversation.open(clientIdentityId);
        given(conversationRepository.findOpenByClientIdentityId(clientIdentityId))
                .willReturn(Optional.empty(), Optional.of(racedConversation));
        willThrow(new DataIntegrityViolationException("unique constraint violation"))
                .given(conversationCreationService).create(clientIdentityId);

        StartConversationResult result = conversationStartService.start(Optional.of(clientIdentityId));

        assertThat(result.resumed()).isTrue();
        assertThat(result.conversationPublicId()).isEqualTo(racedConversation.getPublicId());
    }
}
