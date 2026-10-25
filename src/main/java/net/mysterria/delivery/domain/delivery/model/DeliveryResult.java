package net.mysterria.delivery.domain.delivery.model;

import lombok.Data;
import net.mysterria.delivery.domain.delivery.model.source.DeliveryStatus;

import java.time.Instant;
import java.util.List;

@Data
public class DeliveryResult {

    private final String purchaseId;
    private final DeliveryStatus status;
    private final String deliveredAt;
    private final List<String> failedCommands;

    public static DeliveryResult delivered(String purchaseId, List<String> failedCommands) {
        return new DeliveryResult(purchaseId, DeliveryStatus.DELIVERED, Instant.now().toString(), failedCommands);
    }

    public static DeliveryResult of(String purchaseId, DeliveryStatus status) {
        return new DeliveryResult(purchaseId, status, null, List.of());
    }
}
