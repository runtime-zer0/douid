package kr.douid.brand.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.douid.brand.chat.application.command.StartConversationResult;
import kr.douid.brand.chat.domain.Conversation;
import kr.douid.brand.chat.domain.ConversationRepository;
import kr.douid.brand.chat.domain.ConversationStatus;
import kr.douid.brand.client.application.ClientIdentityProvisioningService;
import kr.douid.brand.client.application.ClientIdentityProvisioningService.ProvisionedClient;
import kr.douid.brand.client.domain.ClientIdentityRepository;

@ExtendWith(MockitoExtension.class)
class ConversationStartServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ClientIdentityRepository clientIdentityRepository;

    @Mock
    private ClientIdentityProvisioningService clientIdentityProvisioningService;

    private ConversationStartService conversationStartService;

    @BeforeEach
    void setUp() {
        conversationStartService = new ConversationStartService(
                conversationRepository, clientIdentityRepository, clientIdentityProvisioningService);
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
        verify(conversationRepository, never()).save(any());
        verify(clientIdentityProvisioningService, never()).provision();
    }

    @Test
    void start_미인증_신규상담주체발급후_새상담생성() {
        given(clientIdentityProvisioningService.provision())
                .willReturn(new ProvisionedClient(1L, "raw-token"));
        given(conversationRepository.findOpenByClientIdentityId(1L)).willReturn(Optional.empty());
        given(conversationRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        StartConversationResult result = conversationStartService.start(Optional.empty());

        assertThat(result.resumed()).isFalse();
        assertThat(result.rawClientToken()).isEqualTo("raw-token");
        assertThat(result.status()).isEqualTo(ConversationStatus.OPEN);
        verify(conversationRepository).save(any());
    }

    @Test
    void start_인증됨_활성상담없음_새상담만생성하고_신규발급없음() {
        given(conversationRepository.findOpenByClientIdentityId(1L)).willReturn(Optional.empty());
        given(conversationRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        StartConversationResult result = conversationStartService.start(Optional.of(1L));

        assertThat(result.resumed()).isFalse();
        assertThat(result.rawClientToken()).isNull();
        verify(clientIdentityProvisioningService, never()).provision();
        verify(conversationRepository).save(any());
    }
}
