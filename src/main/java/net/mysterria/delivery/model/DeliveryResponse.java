package net.mysterria.delivery.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryResponse {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("message")
    private String message;

    @JsonProperty("purchaseId")
    private String purchaseId;

    @JsonProperty("queued")
    private Boolean queued;

    @JsonProperty("deliveredAt")
    private String deliveredAt;

    public static DeliveryResponse success(String purchaseId, String message) {
        return DeliveryResponse.builder()
                .success(true)
                .purchaseId(purchaseId)
                .message(message)
                .deliveredAt(java.time.LocalDateTime.now().toString())
                .build();
    }

    public static DeliveryResponse queued(String purchaseId, String message) {
        return DeliveryResponse.builder()
                .success(true)
                .purchaseId(purchaseId)
                .message(message)
                .queued(true)
                .build();
    }

    public static DeliveryResponse error(String purchaseId, String message) {
        return DeliveryResponse.builder()
                .success(false)
                .purchaseId(purchaseId)
                .message(message)
                .build();
    }
}