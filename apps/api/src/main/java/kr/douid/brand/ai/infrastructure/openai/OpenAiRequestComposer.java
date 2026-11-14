package kr.douid.brand.ai.infrastructure.openai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import kr.douid.brand.ai.application.AiChatCommand;
import kr.douid.brand.ai.application.LlmIntegrationException;
import kr.douid.brand.ai.infrastructure.config.OpenAiProperties;
import tools.jackson.databind.node.ObjectNode;
import static kr.douid.brand.ai.application.LlmIntegrationException.Kind.*;

/** 서버 지시문과 비신뢰 이력을 분리하는 불변 요청 구성 */
public final class OpenAiRequestComposer {
    private final OpenAiProperties properties;
    private final String instructions;

    /** 서버 리소스 로드
     * @param properties 서버 전용 설정
     */
    public OpenAiRequestComposer(OpenAiProperties properties) {
        properties.validate(java.time.Duration.ofSeconds(60));
        this.properties = properties;
        try (var resource = OpenAiRequestComposer.class.getResourceAsStream("/ai/douid-instructions.txt")) {
            if (resource == null) throw new IllegalStateException("Missing AI instructions");
            instructions = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) { throw new IllegalStateException("Cannot load AI instructions"); }
    }

    record Requests(byte[] count, byte[] generation) {}

    Requests compose(AiChatCommand command) {
        ObjectNode common = OpenAiEventReader.JSON.createObjectNode();
        common.put("model", properties.getModel());
        String url = properties.getKakaoChannelUrl();
        common.put("instructions", instructions + (url == null || url.isBlank() ? "" : "\n서버에서 확인한 카카오톡 채널 URL: " + url));
        var input = common.putArray("input");
        for (var item : command.history()) {
            if (!"user".equals(item.role()) && !"assistant".equals(item.role())) throw new LlmIntegrationException(PROTOCOL);
            input.addObject().put("role", item.role()).put("content", item.content());
        }
        input.addObject().put("role", "user").put("content", command.message());
        common.putObject("reasoning").put("effort", "none");
        common.putObject("text").putObject("format").put("type", "text");
        common.putArray("tools");
        common.put("tool_choice", "none");
        common.put("truncation", "disabled");
        byte[] count = OpenAiEventReader.JSON.writeValueAsBytes(common);
        common.put("stream", true).put("store", false).put("max_output_tokens", properties.getMaxOutputTokens());
        byte[] generation = OpenAiEventReader.JSON.writeValueAsBytes(common);
        if (count.length > properties.getMaxProviderRequestBytes() || generation.length > properties.getMaxProviderRequestBytes())
            throw new LlmIntegrationException(PROVIDER_RESOURCE_LIMIT);
        return new Requests(count, generation);
    }
}
