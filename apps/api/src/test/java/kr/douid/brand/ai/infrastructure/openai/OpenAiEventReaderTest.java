package kr.douid.brand.ai.infrastructure.openai;

import static org.assertj.core.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import kr.douid.brand.ai.application.LlmIntegrationException;
import kr.douid.brand.ai.testsupport.OpenAiStubServer;

class OpenAiEventReaderTest {
    @Test void 이미_완료된_item에_새_part가_추가되면_거부() {
        var nodes=frames("hello");
        var done=OpenAiEventReader.JSON.createObjectNode().put("type","response.output_item.done")
                .put("sequence_number",2).put("output_index",0);
        done.set("item",nodes.get(1).get("item").deepCopy());
        nodes.add(2,done);
        for(int i=0;i<nodes.size();i++)nodes.get(i).put("sequence_number",i);
        assertThatThrownBy(()->read(join(nodes))).isInstanceOf(LlmIntegrationException.class);
    }
    @Test void 완료_응답안에_아직_생성중인_item은_성공아님() {
        var nodes=frames("hello");
        ((tools.jackson.databind.node.ObjectNode)nodes.get(5).path("response").path("output").get(0)).put("status","in_progress");
        assertThatThrownBy(()->read(join(nodes))).isInstanceOf(LlmIntegrationException.class);
    }
    @Test void 이벤트_byte_정확경계와_한byte초과() throws Exception {
        String sse=OpenAiStubServer.success("한글😀");
        int max=java.util.Arrays.stream(sse.split("(?<=\n\n)"))
                .mapToInt(frame->frame.getBytes(StandardCharsets.UTF_8).length).max().orElseThrow();
        new OpenAiEventReader(max).read(new ByteArrayInputStream(sse.getBytes(StandardCharsets.UTF_8)),s->{});
        assertThatThrownBy(()->new OpenAiEventReader(max-1).read(new ByteArrayInputStream(sse.getBytes(StandardCharsets.UTF_8)),s->{}))
                .isInstanceOfSatisfying(LlmIntegrationException.class,e->assertThat(e.kind())
                        .isEqualTo(LlmIntegrationException.Kind.PROVIDER_RESOURCE_LIMIT));
    }
    @Test void 부가이벤트와_최대32단계_JSON도_정상완료를_대체하지않음() throws Exception {
        var nodes=frames("hello");
        for(int i=0;i<nodes.size();i++)nodes.get(i).put("sequence_number",i+1);
        String metadata=OpenAiStubServer.event("{\"type\":\"response.metadata\",\"sequence_number\":0,\"extra\":"+
                "[".repeat(31)+"0"+"]".repeat(31)+"}");
        read(metadata+join(nodes));
        assertThatThrownBy(()->read(metadata)).isInstanceOf(LlmIntegrationException.class);
    }
    void read(String sse) throws Exception {
        new OpenAiEventReader(131072).read(new ByteArrayInputStream(sse.getBytes(StandardCharsets.UTF_8)), s -> {});
    }
    @ParameterizedTest @ValueSource(strings = {"failed", "incomplete"})
    void 정상종료_이외에는_부분텍스트가_있어도_실패(String status) {
        String sse = OpenAiStubServer.success("partial").replace("response.completed", "response."+status)
                .replace("\"status\":\"completed\"", "\"status\":\""+status+"\"");
        assertThatThrownBy(() -> read(sse)).isInstanceOf(LlmIntegrationException.class);
    }
    @Test void done만_있고_완료없는_EOF는_실패() {
        String sse = OpenAiStubServer.success("partial");
        assertThatThrownBy(() -> read(sse.substring(0, sse.indexOf("data: {\"type\":\"response.completed"))))
                .isInstanceOf(LlmIntegrationException.class);
    }
    @Test void 같은_문구의_실제반복을_중복으로_제거하지않음() throws Exception {
        var nodes = frames("반복");
        var second = nodes.get(3).deepCopy(); second.put("sequence_number", 4);
        nodes.add(4, second);
        nodes.get(5).put("sequence_number", 5).put("text", "반복반복");
        nodes.get(6).put("sequence_number", 6);
        ((tools.jackson.databind.node.ObjectNode)nodes.get(6).path("response").path("output").get(0).path("content").get(0)).put("text", "반복반복");
        var deltas = new ArrayList<String>();
        new OpenAiEventReader(131072).read(new ByteArrayInputStream(join(nodes).getBytes(StandardCharsets.UTF_8)), deltas::add);
        assertThat(deltas).containsExactly("반복", "반복");
    }
    @Test void 거절텍스트도_명시적성공_때만_완료() throws Exception {
        read(OpenAiStubServer.success("거절안내").replace("output_text", "refusal").replace("\"text\":", "\"refusal\":"));
    }
    @ParameterizedTest @ValueSource(strings = {"sequence", "item", "part", "type", "status", "text", "tool", "empty"})
    void 모순되거나_지원하지않는_출력을_성공으로_추정하지않음(String kind) {
        var nodes = frames(kind.equals("empty") ? " " : "hello");
        switch (kind) {
            case "sequence" -> nodes.get(3).put("sequence_number", 1);
            case "item" -> nodes.get(3).put("item_id", "other");
            case "part" -> nodes.get(3).put("content_index", 1);
            case "type" -> nodes.get(3).put("type", "response.audio.delta");
            case "status" -> ((tools.jackson.databind.node.ObjectNode)nodes.get(5).get("response")).put("status", "in_progress");
            case "text" -> nodes.get(4).put("text", "contradiction");
            case "tool" -> ((tools.jackson.databind.node.ObjectNode)nodes.get(1).get("item")).put("type", "function_call");
        }
        assertThatThrownBy(() -> read(join(nodes))).isInstanceOf(LlmIntegrationException.class);
    }
    @Test void CR_LF_CRLF와_여러줄_data_주석을_허용() throws Exception {
        String sse = OpenAiStubServer.success("😀한글").replace("data: {", ": comment\nevent: ignored\nevent:\ndata: {\ndata: ")
                .replace("\n", "\r\n");
        read(sse);
        read(OpenAiStubServer.success("한글").replace('\n', '\r'));
    }
    @Test void 손상_UTF8_JSON_중첩_프레임_상한은_안전한_실패() {
        assertThatThrownBy(() -> read("data: {broken}\n\n")).isInstanceOf(LlmIntegrationException.class);
        assertThatThrownBy(() -> read("data: "+"[".repeat(33)+"0"+"]".repeat(33)+"\n\n"))
                .isInstanceOf(LlmIntegrationException.class);
        assertThatThrownBy(() -> read("data: "+"x".repeat(131072)+"\n\n")).isInstanceOf(LlmIntegrationException.class);
        assertThatThrownBy(() -> new OpenAiEventReader(131072).read(new ByteArrayInputStream(new byte[]{(byte)0xff, '\n'}), s -> {}))
                .isInstanceOf(LlmIntegrationException.class);
    }
    static java.util.List<tools.jackson.databind.node.ObjectNode> frames(String text) {
        return java.util.Arrays.stream(OpenAiStubServer.success(text).strip().split("\n\n"))
                .map(s -> (tools.jackson.databind.node.ObjectNode)OpenAiEventReader.JSON.readTree(s.substring(6)))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
    static String join(java.util.List<tools.jackson.databind.node.ObjectNode> nodes) {
        return nodes.stream().map(n -> OpenAiStubServer.event(n.toString())).collect(java.util.stream.Collectors.joining());
    }
    @Test void Delta만_순차전달하고_done과_최종텍스트는_재출력하지않음() throws Exception {
        var deltas = new ArrayList<String>();
        var reader = new OpenAiEventReader(131072);
        reader.read(new ByteArrayInputStream(OpenAiStubServer.success("안내😀").getBytes(StandardCharsets.UTF_8)), deltas::add);
        assertThat(deltas).containsExactly("안내😀");
        assertThat(reader.usage().path("total_tokens").asInt()).isEqualTo(102);
    }
}
