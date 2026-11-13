package kr.douid.brand.ai.presentation.stream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import kr.douid.brand.ai.application.AiChatExecution;

/** MVC emitter 초기화 이후 실행 활성화, 초기화 실패 시 폐기 */
public final class AiChatAsyncInterceptor implements AsyncHandlerInterceptor {
    public static final String EXECUTION = AiChatAsyncInterceptor.class.getName() + ".execution";
    /** emitter 초기화가 끝난 요청의 실행 활성화
     * @param request 초기화된 요청
     * @param response HTTP 응답
     * @param handler 선택된 MVC handler
     */
    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getAttribute(EXECUTION) instanceof AiChatExecution execution) execution.activate();
    }
    /** 비동기 초기화 실패의 미실행 자원 폐기
     * @param request 처리된 요청
     * @param response HTTP 응답
     * @param handler 선택된 MVC handler
     * @param error 처리 오류 또는 null
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception error) {
        if (request.getDispatcherType() == jakarta.servlet.DispatcherType.REQUEST
                && !request.isAsyncStarted() && request.getAttribute(EXECUTION) instanceof AiChatExecution execution) {
            execution.abortBeforeStart();
        }
    }
}
