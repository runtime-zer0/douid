package kr.douid.brand.ai.presentation;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import kr.douid.brand.ai.application.*;
import kr.douid.brand.ai.presentation.stream.*;

/** 익명 JSON POST와 SSE 응답을 연결하는 공개 transport */
@RestController
public class AiChatController {
    private final AiChatUseCase useCase;
    private final AiChatPolicy policy;
    private final AiChatRequestReader reader;

    /** 공개 transport 구성
     * @param useCase 검증·수락·실행 application 경계
     * @param policy 요청 및 전송 제한
     */
    public AiChatController(AiChatUseCase useCase, AiChatPolicy policy) {
        this.useCase = useCase; this.policy = policy;
        this.reader = new AiChatRequestReader(policy.maxRequestBytes());
    }

    /** 비회원 질문 검증 및 스트리밍 응답 수락
     * @param request JSON 입력
     * @return 초기화 뒤 실행될 SSE 응답
     * @throws IOException 요청 읽기 실패
     */
    @PostMapping(value = "/api/public/ai-chat/stream", consumes = "application/json", produces = "text/event-stream")
    public ResponseEntity<SseEmitter> stream(HttpServletRequest request) throws IOException {
        useCase.attempt();
        var command = reader.read(request).toCommand();
        var prepared = useCase.prepare(command);
        var presenter = new AiChatSsePresenter(policy);
        var execution = useCase.start(prepared, presenter, presenter::attach);
        request.setAttribute(AiChatAsyncInterceptor.EXECUTION, execution);
        return ResponseEntity.ok().header("Cache-Control", "no-store, no-transform")
                .header("X-Accel-Buffering", "no").body(presenter.emitter());
    }
}
