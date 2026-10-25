package net.mysterria.delivery.domain.delivery.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private VoteReward voteReward;
    private String queuedAt;
    private int retryCount;
    private String failureReason;
}
