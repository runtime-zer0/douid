package kr.douid.brand.ai.presentation.stream;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 공개 채팅 경로에 한정한 MVC 초기화 gate 연결 */
@Configuration(proxyBeanMethods = false)
public class AiChatWebConfig implements WebMvcConfigurer {
    /** 공개 채팅 경로에 한정한 초기화 callback 등록
     * @param registry MVC interceptor 등록 경계
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AiChatAsyncInterceptor()).addPathPatterns("/api/public/ai-chat/stream");
    }
}
