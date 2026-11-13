package kr.douid.brand.ai.presentation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import kr.douid.brand.ai.application.AiChatCommand.HistoryMessage;

/** 최대 byte 읽기와 endpoint 전용 strict JSON 변환 */
public final class AiChatRequestReader {
    private final int maxBytes;
    private final JsonMapper mapper = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

    /** bounded reader 생성
     * @param maxBytes 최대 수신 byte
     */
    public AiChatRequestReader(int maxBytes) { this.maxBytes = maxBytes; }

    /** JSON 구조 검증 및 요청 값 변환
     * @param request 수신 HTTP 요청
     * @return 신뢰되지 않은 요청 값
     * @throws IOException 수신 연결 읽기 실패
     */
    public AiChatRequest read(HttpServletRequest request) throws IOException {
        byte[] bytes = request.getInputStream().readNBytes(maxBytes + 1);
        if (bytes.length > maxBytes) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        try {
            JsonNode root = mapper.readTree(bytes);
            if (root == null || !root.isObject() || !string(root.get("message"))
                    || root.size() != (root.has("history") ? 2 : 1)) throw invalid();
            List<HistoryMessage> history = new ArrayList<>();
            if (root.has("history")) {
                var items = root.get("history");
                if (!items.isArray()) throw invalid();
                for (var item : items) {
                    if (!item.isObject() || item.size() != 2 || !string(item.get("role"))
                            || !string(item.get("content"))) throw invalid();
                    history.add(new HistoryMessage(item.get("role").stringValue(), item.get("content").stringValue()));
                }
            }
            return new AiChatRequest(root.get("message").stringValue(), List.copyOf(history));
        } catch (JacksonException e) {
            throw invalid();
        }
    }

    private boolean string(JsonNode node) { return node != null && node.isString(); }
    private ResponseStatusException invalid() { return new ResponseStatusException(HttpStatus.BAD_REQUEST); }
}
