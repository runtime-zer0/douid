package kr.douid.brand.ai.testsupport;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
/** 자동 운영 선택이 없는 테스트 전용 공급자 연결 */
@TestConfiguration(proxyBeanMethods=false)
public class AiChatTestConfiguration {
    @Bean @Primary ControlledLlmGateway controlledLlmGateway() {return new ControlledLlmGateway();}
}
