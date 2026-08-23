package kr.douid.brand.chat.application;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.douid.brand.chat.application.command.StartConversationResult;
import kr.douid.brand.chat.domain.Conversation;
import kr.douid.brand.chat.domain.ConversationRepository;
import kr.douid.brand.client.application.ClientIdentityProvisioningService;
import kr.douid.brand.client.application.ClientIdentityProvisioningService.ProvisionedClient;
import kr.douid.brand.client.domain.ClientIdentityRepository;
import lombok.RequiredArgsConstructor;

/**
 * 상담 시작/복원 유스케이스
 *
 * 유효한 상담 주체가 없으면 새로 발급하고, 있으면 활성 상담을 재사용하거나 없으면 새로 생성한다.
 * 활성 상담 조회 전에 상담 주체({@link kr.douid.brand.client.domain.ClientIdentity}) 행을
 * 비관적 락으로 먼저 조회해 직렬화한다. 아직 존재하지 않는 Conversation 행에는 락을 걸 수
 * 없으므로, 이미 존재하는 상담 주체 행에 락을 걸어야 동시 요청에도 활성 상담이 최대 1개만
 * 존재하도록 보장할 수 있다(FR-026).
 */
@Service
@RequiredArgsConstructor
public class ConversationStartService {

    private final ConversationRepository conversationRepository;
    private final ClientIdentityRepository clientIdentityRepository;
    private final ClientIdentityProvisioningService clientIdentityProvisioningService;

    /**
     * 상담을 시작하거나 기존 활성 상담을 복원
     *
     * @param resolvedClientIdentityId 인증된 상담 주체 ID (미인증이면 empty)
     * @return 상담 시작/복원 결과
     */
    @Transactional
    public StartConversationResult start(Optional<Long> resolvedClientIdentityId) {
        String rawClientToken = null;
        Long clientIdentityId;

        if (resolvedClientIdentityId.isPresent()) {
            clientIdentityId = resolvedClientIdentityId.get();
            // 상담 주체 행 자체의 필드는 필요 없다. 이 조회의 유일한 목적은 findOpenByClientIdentityId()
            // 이전에 상담 주체 행을 잠가 "활성 Conversation 조회 후 없으면 생성"을 직렬화하는 것이다.
            clientIdentityRepository.findByIdForUpdate(clientIdentityId);
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

        Conversation conversation = conversationRepository.save(Conversation.open(clientIdentityId));
        return new StartConversationResult(
                conversation.getPublicId(), conversation.getStatus(), false, rawClientToken);
    }
}
