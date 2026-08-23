package kr.douid.brand.chat.presentation;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kr.douid.brand.chat.application.ConversationLookupService;
import kr.douid.brand.chat.application.ConversationStartService;
import kr.douid.brand.chat.application.MessageSendService;
import kr.douid.brand.chat.application.command.StartConversationResult;
import kr.douid.brand.chat.application.query.ConversationMessageQueryService;
import kr.douid.brand.chat.application.query.MessageView;
import kr.douid.brand.chat.domain.Conversation;
import kr.douid.brand.chat.presentation.request.SendMessageRequest;
import kr.douid.brand.chat.presentation.response.ConversationResponse;
import kr.douid.brand.chat.presentation.response.MessageResponse;
import kr.douid.brand.shared.pagination.CursorPageResponse;
import kr.douid.brand.shared.response.ApiResponse;
import kr.douid.brand.shared.security.ClientIdentityContext;
import kr.douid.brand.shared.security.ClientTokenCookieProvider;
import lombok.RequiredArgsConstructor;

/**
 * 비회원 상담 REST API 컨트롤러
 *
 * client_token 쿠키 기반으로 상담 시작/복원, 조회, 메시지 송수신을 처리한다.
 * 인증 검증은 {@link kr.douid.brand.shared.security.ClientCredentialFilter}에서 중앙화되어 있으므로,
 * 이 컨트롤러는 {@link ClientIdentityContext}를 통해서만 현재 상담 주체를 참조한다(FR-025).
 */
@RestController
@RequiredArgsConstructor
public class ClientConversationController {

    private static final int DEFAULT_MESSAGE_PAGE_SIZE = 50;

    private final ConversationStartService conversationStartService;
    private final ConversationLookupService conversationLookupService;
    private final ConversationMessageQueryService conversationMessageQueryService;
    private final MessageSendService messageSendService;
    private final ClientTokenCookieProvider clientTokenCookieProvider;

    /**
     * 상담을 시작하거나 기존 활성 상담을 복원
     *
     * @return 상담 시작/복원 결과 응답. 새 credential이 발급된 경우 {@code Set-Cookie} 헤더 포함
     */
    @PostMapping("/api/client/conversations")
    public ResponseEntity<ApiResponse<ConversationResponse>> start() {
        StartConversationResult result = conversationStartService.start(ClientIdentityContext.get());

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
        if (result.rawClientToken() != null) {
            responseBuilder.header(
                    HttpHeaders.SET_COOKIE,
                    clientTokenCookieProvider.issueCookie(result.rawClientToken()).toString());
        }

        return responseBuilder.body(ApiResponse.success(ConversationResponse.from(result)));
    }

    /**
     * 상담 단건을 조회
     *
     * ownership 검증을 통과한 요청만 접근할 수 있다(FR-019, FR-020).
     *
     * @param conversationId 조회할 상담의 공개 식별자
     * @return 상담 조회 응답
     */
    @GetMapping("/api/client/conversations/{conversationId}")
    public ResponseEntity<ApiResponse<ConversationResponse>> find(@PathVariable UUID conversationId) {
        Conversation conversation =
                conversationLookupService.getOwned(conversationId, ClientIdentityContext.get());

        return ResponseEntity.ok(ApiResponse.success(
                new ConversationResponse(conversation.getPublicId(), conversation.getStatus().name(), false)));
    }

    /**
     * 상담에 속한 메시지 목록을 조회
     *
     * ownership 검증을 통과한 요청만 접근할 수 있다(FR-019, FR-020).
     *
     * @param conversationId 조회할 상담의 공개 식별자
     * @param cursor          이전 페이지 마지막 메시지 ID (없으면 처음부터)
     * @param size            페이지 크기 (기본 50)
     * @return 메시지 목록 응답
     */
    @GetMapping("/api/client/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<CursorPageResponse<MessageResponse>>> findMessages(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false, defaultValue = "" + DEFAULT_MESSAGE_PAGE_SIZE) int size) {
        Conversation conversation =
                conversationLookupService.getOwned(conversationId, ClientIdentityContext.get());

        CursorPageResponse<MessageView> page =
                conversationMessageQueryService.findMessages(conversation.getId(), cursor, size);
        CursorPageResponse<MessageResponse> response = new CursorPageResponse<>(
                page.getItems().stream()
                        .map(item -> new MessageResponse(item.id(), item.content(), item.createdAt()))
                        .toList(),
                page.getNextCursor(),
                page.isHasNext());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 상담에 새 메시지를 전송
     *
     * ownership이 확인된 활성(OPEN) 상담에만 전송할 수 있다(FR-027, FR-028).
     *
     * @param conversationId 메시지를 보낼 상담의 공개 식별자
     * @param request         메시지 전송 요청
     * @return 저장된 메시지 응답
     */
    @PostMapping("/api/client/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable UUID conversationId, @Valid @RequestBody SendMessageRequest request) {
        MessageView saved = messageSendService.send(conversationId, ClientIdentityContext.get(), request.content());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                new MessageResponse(saved.id(), saved.content(), saved.createdAt())));
    }
}
