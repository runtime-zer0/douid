package kr.douid.brand.ai.infrastructure.openai;

import java.net.http.HttpClient;
import java.util.List;
import org.junit.jupiter.api.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import kr.douid.brand.ai.application.AiChatCommand;
import kr.douid.brand.ai.application.port.LlmGateway;
import kr.douid.brand.ai.infrastructure.config.OpenAiProperties;
import kr.douid.brand.ai.infrastructure.stream.BoundedChatStreamExecutor;
import kr.douid.brand.ai.testsupport.OpenAiStubServer;

abstract class OpenAiTestSupport {
    OpenAiStubServer stub;
    HttpClient client;
    BoundedChatStreamExecutor executor;
    OpenAiProperties properties;
    SimpleMeterRegistry metrics;

    @BeforeEach void setupProvider() throws Exception {
        stub = new OpenAiStubServer();
        client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        executor = new BoundedChatStreamExecutor(2);
        properties = new OpenAiProperties(); properties.setEnabled(true); properties.setApiKey("KEY_CANARY");
        metrics = new SimpleMeterRegistry();
    }
    LlmGateway gateway() {
        return OpenAiResponsesGateway.forLoopbackTest(properties, client, executor, metrics, stub.endpoint());
    }
    LlmGateway.LlmStream source() { return gateway().prepare(new AiChatCommand("QUESTION_CANARY", List.of())); }
    @AfterEach void closeProvider() {
        executor.close(); client.shutdownNow(); stub.close(); metrics.close();
    }
}
