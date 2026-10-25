package net.mysterria.delivery.domain.delivery.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PurchaseRequest {

    private String purchaseId;
    private String userId;
    private String minecraftUuid;
    private String nickname;
    private Integer serviceId;
    private String serviceName;
    private String serviceCategory;
    private Map<String, Object> metadata;
    @JsonDeserialize(using = LocalDateTimeArrayDeserializer.class)
    private String expiresAt;
    @JsonAlias("amount")
    private Integer quantity;

    public List<String> metadataList(String key) {
        Object value = metadata != null ? metadata.get(key) : null;
        return switch (value) {
            case List<?> list -> list.stream().map(String::valueOf).toList();
            case String text when !text.isBlank() -> List.of(text);
            case null, default -> List.of();
        };
    }

    public String metadataValue(String key) {
        Object value = metadata != null ? metadata.get(key) : null;
        return value != null ? String.valueOf(value) : null;
    }

    public static class LocalDateTimeArrayDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode node = parser.getCodec().readTree(parser);
            if (node.isTextual()) {
                return node.asText();
            }
            if (node.isArray() && node.size() >= 6) {
                return LocalDateTime.of(node.get(0).asInt(), node.get(1).asInt(), node.get(2).asInt(),
                        node.get(3).asInt(), node.get(4).asInt(), node.get(5).asInt(),
                        node.size() > 6 ? node.get(6).asInt() : 0).toString();
            }
            return null;
        }
    }
}
