package kr.douid.brand.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import kr.douid.brand.chat.application.ConversationStartService;
import kr.douid.brand.chat.application.command.StartConversationResult;
import kr.douid.brand.chat.domain.Conversation;
import kr.douid.brand.chat.domain.ConversationRepository;
import kr.douid.brand.client.application.ClientAuthenticationService;
import kr.douid.brand.client.application.ClientCredentialIssuer;
import kr.douid.brand.client.application.ClientIdentityProvisioningService;
import kr.douid.brand.client.application.ClientIdentityProvisioningService.ProvisionedClient;
import kr.douid.brand.client.domain.ClientCredential;
import kr.douid.brand.client.domain.ClientCredentialRepository;

/**
 * Recovery로 발급된 새 client_token이 Phase 07의 {@link ConversationStartService}를 그대로 타면서
 * 활성 Conversation 복원/미복원이 올바르게 동작하는지 검증(FR-025~027)
 *
 * 신규 구현 없음 — chat feature는 이번 Phase에서 어떤 코드도 추가·수정하지 않았다(research.md #9, #11).
 * 이 테스트는 client feature의 신규 코드(credential 재발급)와 기존 chat 로직의 조합이 요구사항을
 * 만족하는지 확인하는 통합 검증이다.
 */
@SpringBootTest
@Testcontainers
class RecoveryConversationContinuityTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("spring.sql.init.mode", () -> "never");
    }

    @Autowired
    private ClientIdentityProvisioningService clientIdentityProvisioningService;

    @Autowired
    private ClientCredentialRepository clientCredentialRepository;

    @Autowired
    private ClientCredentialIssuer clientCredentialIssuer;

    @Autowired
    private ClientAuthenticationService clientAuthenticationService;

    @Autowired
    private ConversationStartService conversationStartService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Test
    void 활성Conversation있는상담주체_복원후_기존Conversation이반환된다() {
        ProvisionedClient provisioned = clientIdentityProvisioningService.provision();
        Long clientIdentityId = provisioned.clientIdentityId();
        StartConversationResult firstStart = conversationStartService.start(Optional.of(clientIdentityId));

        String recoveredRawToken = issueNewCredential(clientIdentityId);
        Optional<Long> resolved = clientAuthenticationService.resolve(recoveredRawToken);
        StartConversationResult resumed = conversationStartService.start(resolved);

        assertThat(resumed.resumed()).isTrue();
        assertThat(resumed.conversationPublicId()).isEqualTo(firstStart.conversationPublicId());
    }

    @Test
    void 활성Conversation없는상담주체_복원해도_자동재오픈되지않고_새요청시_새Conversation이생성된다() {
        ProvisionedClient provisioned = clientIdentityProvisioningService.provision();
        Long clientIdentityId = provisioned.clientIdentityId();
        StartConversationResult firstStart = conversationStartService.start(Optional.of(clientIdentityId));
        closeConversation(firstStart.conversationPublicId());

        String recoveredRawToken = issueNewCredential(clientIdentityId);
        Optional<Long> resolved = clientAuthenticationService.resolve(recoveredRawToken);
        StartConversationResult afterRecovery = conversationStartService.start(resolved);

        assertThat(afterRecovery.resumed()).isFalse();
        assertThat(afterRecovery.conversationPublicId()).isNotEqualTo(firstStart.conversationPublicId());
    }

    private String issueNewCredential(Long clientIdentityId) {
        String rawToken = clientCredentialIssuer.issueRawToken();
        String tokenHash = clientCredentialIssuer.hash(rawToken);
        clientCredentialRepository.save(
                ClientCredential.issue(clientIdentityId, tokenHash, LocalDateTime.now().plusDays(180)));
        return rawToken;
    }

    private void closeConversation(UUID publicId) {
        Conversation conversation = conversationRepository.findByPublicId(publicId).orElseThrow();
        conversation.close();
        conversationRepository.save(conversation);
    }
}
