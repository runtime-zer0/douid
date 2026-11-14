package kr.douid.brand.ai.infrastructure.openai;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.security.*;
import java.util.*;
import java.util.function.Consumer;
import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import kr.douid.brand.ai.application.LlmIntegrationException;
import static kr.douid.brand.ai.application.LlmIntegrationException.Kind.*;

/** 유한 SSE 프레임과 명시적 Provider 성공 검증 */
final class OpenAiEventReader {
    static final JsonMapper JSON = JsonMapper.builder(JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(32).build()).build())
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    private final int maxEventBytes;
    private final Map<Integer, Item> items = new HashMap<>();
    private String responseId;
    private long sequence = -1;
    private boolean nonBlank;
    private JsonNode usage;
    private int frameBytes;

    OpenAiEventReader(int maxEventBytes) { this.maxEventBytes = maxEventBytes; }
    JsonNode usage() { return usage; }

    void read(InputStream body, Consumer<String> delta) throws IOException {
        var input = new PushbackInputStream(new BufferedInputStream(body), 1);
        var data = new StringBuilder();
        String event = "";
        for (;;) {
            String line = line(input);
            if (line == null) throw new LlmIntegrationException(PROTOCOL);
            if (line.isEmpty()) {
                frameBytes = 0;
                if (!data.isEmpty()) {
                    if (accept(event, data.toString(), delta)) return;
                    data.setLength(0);
                }
                event = "";
            } else if (!line.startsWith(":")) {
                int colon = line.indexOf(':');
                String field = colon < 0 ? line : line.substring(0, colon);
                String value = colon < 0 ? "" : line.substring(colon + 1);
                if (value.startsWith(" ")) value = value.substring(1);
                if (field.equals("data")) { if (!data.isEmpty()) data.append('\n'); data.append(value); }
                else if (field.equals("event")) event = value;
            }
        }
    }

    private String line(PushbackInputStream input) throws IOException {
        var bytes = new ByteArrayOutputStream();
        int value;
        while ((value = input.read()) != -1) {
            if (++frameBytes > maxEventBytes) throw new LlmIntegrationException(PROVIDER_RESOURCE_LIMIT);
            if (value == '\n') break;
            if (value == '\r') {
                int next = input.read();
                if (next == '\n' && ++frameBytes > maxEventBytes) throw new LlmIntegrationException(PROVIDER_RESOURCE_LIMIT);
                if (next != '\n' && next != -1) input.unread(next);
                break;
            }
            bytes.write(value);
        }
        if (value == -1) return null;
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
        } catch (CharacterCodingException e) { throw new LlmIntegrationException(PROTOCOL); }
    }

    private boolean accept(String event, String data, Consumer<String> delta) {
        JsonNode node;
        try { node = JSON.readTree(data); }
        catch (JacksonException e) { throw new LlmIntegrationException(PROTOCOL); }
        String type = text(node, "type");
        if (!event.isEmpty() && !event.equals(type)) fail();
        long next = integer(node, "sequence_number");
        if (next <= sequence) fail();
        sequence = next;
        switch (type) {
            case "response.created", "response.in_progress", "response.queued" -> {
                var response = response(node);
                if (!Set.of("queued", "in_progress").contains(text(response, "status"))) fail();
            }
            case "response.output_item.added" -> {
                if (responseId == null) fail();
                int index = index(node, "output_index");
                var item = node.path("item");
                supportedItem(item);
                if (item.has("status") && !"in_progress".equals(text(item, "status"))) fail();
                if (items.size() >= 64 || items.putIfAbsent(index, new Item(text(item, "id"))) != null) fail();
            }
            case "response.content_part.added" -> {
                var item = item(node);
                if (item.done) fail();
                var part = node.path("part");
                String kind = text(part, "type");
                supportedPart(kind);
                if (item.parts.size() >= 64 || item.parts.putIfAbsent(index(node, "content_index"), new Part(kind)) != null) fail();
            }
            case "response.output_text.delta", "response.refusal.delta" -> {
                var part = part(node);
                if (part.done || !type.equals("response." + part.kind + ".delta")) fail();
                String value = text(node, "delta");
                part.hash.update(value.getBytes(StandardCharsets.UTF_8));
                nonBlank |= !value.isBlank();
                if (!value.isEmpty()) delta.accept(value);
            }
            case "response.output_text.done", "response.refusal.done" -> {
                var part = part(node);
                if (part.done || !type.equals("response." + part.kind + ".done")) fail();
                checkText(part, text(node, part.kind.equals("refusal") ? "refusal" : "text"));
                part.done = true;
            }
            case "response.content_part.done" -> {
                var part = part(node);
                var content = node.path("part");
                if (!part.kind.equals(text(content, "type"))) fail();
                checkText(part, text(content, part.kind.equals("refusal") ? "refusal" : "text"));
                part.done = true;
            }
            case "response.output_item.done" -> {
                int index = index(node, "output_index");
                checkItem(items.get(index), node.path("item"));
                items.get(index).done = true;
            }
            case "response.completed" -> {
                var response = response(node);
                usage = response.get("usage");
                if (!"completed".equals(text(response, "status"))) fail();
                var output = response.path("output");
                if (!output.isArray() || output.size() != items.size()) fail();
                for (int i = 0; i < output.size(); i++) checkItem(items.get(i), output.get(i));
                if (!nonBlank) throw new LlmIntegrationException(EMPTY_OUTPUT);
                return true;
            }
            case "response.failed", "response.incomplete" -> {
                var response = response(node);
                usage = response.get("usage");
                String expected = type.substring("response.".length());
                if (!expected.equals(text(response, "status"))) fail();
                if (type.equals("response.failed")) throw new LlmIntegrationException(PROVIDER_FAILED);
                throw new LlmIntegrationException("max_output_tokens".equals(response.path("incomplete_details").path("reason").asString())
                        ? INCOMPLETE_OUTPUT_LIMIT : INCOMPLETE_OTHER);
            }
            case "error" -> throw new LlmIntegrationException(PROVIDER_FAILED);
            default -> {
                if (type.contains("delta") || type.contains("audio") || type.contains("tool")
                        || type.contains("function") || type.startsWith("response.output_")
                        || type.startsWith("response.content_part")) throw new LlmIntegrationException(UNSUPPORTED_OUTPUT);
                // 부가 이벤트는 완료 조건을 대체하지 않음
            }
        }
        return false;
    }

    private JsonNode response(JsonNode node) {
        var response = node.path("response");
        String id = text(response, "id");
        if (id.isBlank() || id.length() > 256 || responseId != null && !responseId.equals(id)) fail();
        responseId = id;
        return response;
    }
    private Item item(JsonNode node) {
        var item = items.get(index(node, "output_index"));
        if (item == null || !item.id.equals(text(node, "item_id"))) fail();
        return item;
    }
    private Part part(JsonNode node) {
        var item = item(node);
        if (item.done) fail();
        var part = item.parts.get(index(node, "content_index"));
        if (part == null) fail();
        return part;
    }
    private void checkItem(Item expected, JsonNode node) {
        supportedItem(node);
        if (node.has("status") && !"completed".equals(text(node, "status"))) fail();
        if (expected == null || !expected.id.equals(text(node, "id"))) fail();
        var content = node.path("content");
        if (!content.isArray() || content.size() != expected.parts.size()) fail();
        for (int i = 0; i < content.size(); i++) {
            var part = expected.parts.get(i);
            if (part == null || !part.kind.equals(text(content.get(i), "type"))) fail();
            checkText(part, text(content.get(i), part.kind.equals("refusal") ? "refusal" : "text"));
        }
    }
    private void supportedItem(JsonNode item) {
        if (!"message".equals(text(item, "type")) || !"assistant".equals(text(item, "role")))
            throw new LlmIntegrationException(UNSUPPORTED_OUTPUT);
        if (text(item, "id").isBlank() || text(item, "id").length() > 256) fail();
    }
    private void supportedPart(String kind) {
        if (!Set.of("output_text", "refusal").contains(kind)) throw new LlmIntegrationException(UNSUPPORTED_OUTPUT);
    }
    private void checkText(Part part, String text) {
        try {
            var copy = (MessageDigest) part.hash.clone();
            if (!MessageDigest.isEqual(copy.digest(), digest().digest(text.getBytes(StandardCharsets.UTF_8)))) fail();
        } catch (CloneNotSupportedException e) { throw new LlmIntegrationException(PROTOCOL); }
    }
    static String text(JsonNode node, String field) {
        if (node == null || !node.isObject()) throw new LlmIntegrationException(PROTOCOL);
        var value = node.get(field);
        if (value == null || !value.isString()) throw new LlmIntegrationException(PROTOCOL);
        return value.stringValue();
    }
    static long integer(JsonNode node, String field) {
        if (node == null || !node.isObject()) throw new LlmIntegrationException(PROTOCOL);
        var value = node.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0)
            throw new LlmIntegrationException(PROTOCOL);
        return value.longValue();
    }
    private int index(JsonNode node, String field) {
        long value = integer(node, field);
        if (value >= 64) fail();
        return (int) value;
    }
    private static void fail() { throw new LlmIntegrationException(PROTOCOL); }
    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("Missing SHA-256"); }
    }
    private static final class Item {
        final String id;
        final Map<Integer, Part> parts = new HashMap<>();
        boolean done;
        Item(String id) { this.id = id; }
    }
    private static final class Part {
        final String kind;
        final MessageDigest hash = digest();
        boolean done;
        Part(String kind) { this.kind = kind; }
    }
}
