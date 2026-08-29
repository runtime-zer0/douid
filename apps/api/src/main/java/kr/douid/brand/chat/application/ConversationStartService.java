package kr.douid.brand.chat.application;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import kr.douid.brand.chat.application.command.StartConversationResult;
import kr.douid.brand.chat.domain.Conversation;
import kr.douid.brand.chat.domain.ConversationRepository;
import kr.douid.brand.client.application.ClientIdentityProvisioningService;
import kr.douid.brand.client.application.ClientIdentityProvisioningService.ProvisionedClient;
import lombok.RequiredArgsConstructor;

/**
 * 상담 시작/복원 유스케이스
 *
 * 유효한 상담 주체가 없으면 새로 발급하고, 있으면 활성 상담을 재사용하거나 없으면 새로 생성한다.
 * 동일 상담 주체에서 거의 동시에 여러 상담 시작 요청이 발생해도 활성 Conversation이 최대 1개만
 * 존재하는 것은 {@code conversation.client_identity_id}의 조건부(partial) unique index가
 * 보장한다(FR-026) — 정상 경로는 락 대기 없이 처리되고, 동시 생성 시도로 index 위반이 발생하는
 * 드문 경우에만 재조회해서 먼저 생성된 상담을 반환한다.
 *
 * PostgreSQL은 제약 위반이 발생한 트랜잭션을 즉시 abort 상태로 만들어 같은 트랜잭션에서 재조회를
 * 이어갈 수 없다. 그래서 생성 시도는 {@link ConversationCreationService}의 독립 트랜잭션에
 * 맡기고, 이 서비스는 트랜잭션 경계를 갖지 않은 채 조회·생성·재조회를 순서대로 조율한다.
 */
@Service
@RequiredArgsConstructor
public class ConversationStartService {

    private final ConversationRepository conversationRepository;
    private final ConversationCreationService conversationCreationService;
    private final ClientIdentityProvisioningService clientIdentityProvisioningService;

    /**
     * 상담을 시작하거나 기존 활성 상담을 복원
     *
     * @param resolvedClientIdentityId 인증된 상담 주체 ID (미인증이면 empty)
     * @return 상담 시작/복원 결과
     */
    public StartConversationResult start(Optional<Long> resolvedClientIdentityId) {
        String rawClientToken = null;
        Long clientIdentityId;

        if (resolvedClientIdentityId.isPresent()) {
            clientIdentityId = resolvedClientIdentityId.get();
        } else {
            ProvisionedClient provisioned = clientIdentityProvisioningService.provision();
            clientIdentityId = provisioned.clientIdentityId();
            rawClientToken = provisioned.rawToken();
        }

        Optional<Conversation> existingOpen = conversationRepository.findOpenByClientIdentityId(clientIdentityId);
        if (existingOpen.isPresent()) {
            Conversation conversation = existingOpen.get();
            return new StartConversationResult(
                    conversation.getPublicId(), conversation.getStatus(), true, rawClientToken);
        }

        try {
            Conversation conversation = conversationCreationService.create(clientIdentityId);
            return new StartConversationResult(
                    conversation.getPublicId(), conversation.getStatus(), false, rawClientToken);
        } catch (DataIntegrityViolationException e) {
            Conversation conversation = conversationRepository.findOpenByClientIdentityId(clientIdentityId)
                    .orElseThrow(() -> e);
            return new StartConversationResult(
                    conversation.getPublicId(), conversation.getStatus(), true, rawClientToken);
        }
    }
}
