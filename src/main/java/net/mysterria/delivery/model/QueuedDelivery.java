package net.mysterria.delivery.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueuedDelivery {
    private String purchaseId;
    private UUID playerUuid;
    private String playerName;
    private PurchaseRequest purchaseRequest;
    private LocalDateTime queuedAt;
    private int retryCount;
}