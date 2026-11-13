package kr.douid.brand.ai.presentation;
import static org.assertj.core.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class AiChatRequestReaderTest {
    private AiChatRequest read(String json,int limit) throws Exception {
        var request=new MockHttpServletRequest(); request.setContent(json.getBytes(StandardCharsets.UTF_8));
        return new AiChatRequestReader(limit).read(request);
    }
    @Test void 생략_이력과_빈_이력_수락() throws Exception {
        assertThat(read("{\"message\":\"안녕\"}",100).history()).isEmpty();
        assertThat(read("{\"message\":\"안녕\",\"history\":[]}",100).history()).isEmpty();
    }
    @ParameterizedTest
    @ValueSource(strings={"null","[]","{}","{invalid","{\"message\":1}","{\"message\":true}",
            "{\"message\":null}","{\"message\":\"hi\",\"history\":null}",
            "{\"message\":\"hi\",\"model\":\"admin\"}","{\"message\":\"a\",\"message\":\"b\"}",
            "{\"message\":\"hi\"} {}","{\"message\":\"hi\",\"history\":[null]}",
            "{\"message\":\"hi\",\"history\":[{\"role\":\"user\",\"content\":false}]}"})
    void 잘못된_구조는_원문없이_거부(String json) {
        assertThatThrownBy(() -> read(json,1024)).isInstanceOf(ResponseStatusException.class)
                .hasMessageNotContaining("admin");
    }
    @Test void 실제_바이트_한도_L전후_및_길이헤더없이_검증() throws Exception {
        String json="{\"message\":\"한글😀\"}";
        int size=json.getBytes(StandardCharsets.UTF_8).length;
        assertThat(read(json,size).message()).isEqualTo("한글😀");
        assertThat(read(json,size+1).message()).isEqualTo("한글😀");
        assertThatThrownBy(() -> read(json,size-1)).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(413));
    }
}
