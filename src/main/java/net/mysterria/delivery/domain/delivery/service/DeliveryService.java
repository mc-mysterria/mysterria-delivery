package net.mysterria.delivery.domain.delivery.service;

import lombok.AllArgsConstructor;
import lombok.Setter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.data.TemporaryNodeMergeStrategy;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;
import net.mysterria.delivery.MysterriaDelivery;
import net.mysterria.delivery.config.DeliveryConfig;
import net.mysterria.delivery.domain.announcement.service.AnnouncementService;
import net.mysterria.delivery.domain.delivery.model.DeliveryResult;
import net.mysterria.delivery.domain.delivery.model.PurchaseRequest;
import net.mysterria.delivery.domain.delivery.model.QueuedDelivery;
import net.mysterria.delivery.domain.delivery.model.VoteReward;
import net.mysterria.delivery.domain.delivery.model.source.DeliveryStatus;
import net.mysterria.delivery.domain.delivery.model.source.ServiceCategory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

@AllArgsConstructor
public class DeliveryService {

    private static final Duration DEFAULT_DURATION = Duration.ofDays(30);

    private final MysterriaDelivery plugin;
    private final LuckPerms luckPerms;
    private final DeliveryLedger ledger;
    private final AnnouncementService announcements;
    @Setter
    private DeliveryConfig config;

    public DeliveryResult deliverItem(PurchaseRequest request) {
        requirePurchaseId(request.getPurchaseId());
        UUID playerUuid = parseUuid(request.getMinecraftUuid());
        List<String> commands = request.metadataList("commands");
        if (commands.isEmpty()) {
            throw new IllegalArgumentException("No delivery commands in metadata");
        }
        requireAllowed(commands);

        DeliveryResult existing = existingResult(request.getPurchaseId());
        if (existing != null) {
            return existing;
        }
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) {
            ledger.queue(queued(request.getPurchaseId(), playerUuid, request.getNickname(), request, null));
            return DeliveryResult.of(request.getPurchaseId(), DeliveryStatus.QUEUED);
        }
        return runItem(player, request);
    }

    public DeliveryResult deliverVote(VoteReward reward) {
        requirePurchaseId(reward.getPurchaseId());
        UUID playerUuid = parseUuid(reward.getMinecraftUUID());
        if (reward.getCommand() == null || reward.getCommand().isBlank()) {
            throw new IllegalArgumentException("Vote reward has no command");
        }
        requireAllowed(List.of(reward.getCommand()));

        DeliveryResult existing = existingResult(reward.getPurchaseId());
        if (existing != null) {
            return existing;
        }
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) {
            ledger.queue(queued(reward.getPurchaseId(), playerUuid, reward.getPlayer(), null, reward));
            return DeliveryResult.of(reward.getPurchaseId(), DeliveryStatus.QUEUED);
        }
        return runVote(player, reward);
    }

    public CompletableFuture<DeliveryResult> deliverSubscription(PurchaseRequest request) {
        String group = request.metadataValue("group");
        if (group == null || group.isBlank()) {
            throw new IllegalArgumentException("No group in metadata");
        }
        return grant(request, duration -> List.of(InheritanceNode.builder(group).expiry(duration).build()));
    }

    public CompletableFuture<DeliveryResult> deliverPermissions(PurchaseRequest request) {
        List<String> permissions = request.metadataList("permissions");
        if (permissions.isEmpty()) {
            throw new IllegalArgumentException("No permissions in metadata");
        }
        return grant(request, duration -> permissions.stream()
                .map(permission -> (Node) PermissionNode.builder(permission).value(true).expiry(duration).build())
                .toList());
    }

    public void deliverQueued(Player player) {
        for (QueuedDelivery delivery : ledger.takeQueued(player.getUniqueId())) {
            try {
                if (delivery.getPurchaseRequest() != null) {
                    runItem(player, delivery.getPurchaseRequest());
                } else {
                    runVote(player, delivery.getVoteReward());
                }
            } catch (IllegalStateException e) {
                retryLater(delivery, e.getMessage());
            }
        }
    }

    public int queuedCount() {
        return ledger.queuedCount();
    }

    public List<QueuedDelivery> failedDeliveries() {
        return ledger.failed();
    }

    private CompletableFuture<DeliveryResult> grant(PurchaseRequest request, Function<Duration, List<Node>> nodes) {
        requirePurchaseId(request.getPurchaseId());
        UUID playerUuid = parseUuid(request.getMinecraftUuid());
        List<Node> granted = nodes.apply(duration(request));

        DeliveryResult existing = existingResult(request.getPurchaseId());
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }
        ledger.markDelivered(request.getPurchaseId());

        // Renewals must extend the running node; plain add() refuses a second temporary node for the same group
        return luckPerms.getUserManager()
                .modifyUser(playerUuid, user -> granted.forEach(node -> user.data().add(node, TemporaryNodeMergeStrategy.ADD_NEW_DURATION_TO_EXISTING)))
                .thenApply(ignored -> {
                    plugin.getLogger().info("Granted " + request.getServiceName() + " (" + request.getPurchaseId() + ") to " + playerUuid);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Player player = Bukkit.getPlayer(playerUuid);
                        if (player != null) {
                            announcements.announcePurchase(player, ServiceCategory.fromBackendName(request.getServiceCategory()), request.getServiceName());
                        }
                    });
                    return DeliveryResult.delivered(request.getPurchaseId(), List.of());
                })
                .exceptionally(error -> {
                    ledger.unmarkDelivered(request.getPurchaseId());
                    throw new CompletionException(new IllegalStateException("LuckPerms failed to save " + request.getPurchaseId(), error));
                });
    }

    private DeliveryResult runItem(Player player, PurchaseRequest request) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("uuid", player.getUniqueId().toString());
        placeholders.put("quantity", String.valueOf(request.getQuantity() != null ? request.getQuantity() : 1));
        placeholders.put("service_name", String.valueOf(request.getServiceName()));
        if (request.getMetadata() != null) {
            request.getMetadata().forEach((key, value) -> placeholders.putIfAbsent(key, String.valueOf(value)));
        }

        List<String> commands = request.metadataList("commands").stream().map(template -> fill(template, placeholders)).toList();
        DeliveryResult result = runCommands(request.getPurchaseId(), commands);
        announcements.announcePurchase(player, ServiceCategory.fromBackendName(request.getServiceCategory()), request.getServiceName());
        return result;
    }

    private DeliveryResult runVote(Player player, VoteReward reward) {
        DeliveryResult result = runCommands(reward.getPurchaseId(), List.of(reward.getCommand()));
        announcements.announceVote(player, reward.getAmount());
        return result;
    }

    private DeliveryResult runCommands(String purchaseId, List<String> commands) {
        List<String> failed = new ArrayList<>();
        for (String command : commands) {
            plugin.getLogger().info("Delivering " + purchaseId + ": " + command);
            if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
                failed.add(command);
            }
        }
        if (failed.size() == commands.size()) {
            throw new IllegalStateException("No delivery command succeeded for " + purchaseId);
        }
        if (!failed.isEmpty()) {
            plugin.getLogger().severe("Purchase " + purchaseId + " was delivered partially, these commands failed: " + failed);
        }
        ledger.markDelivered(purchaseId);
        return DeliveryResult.delivered(purchaseId, failed);
    }

    private void retryLater(QueuedDelivery delivery, String reason) {
        delivery.setRetryCount(delivery.getRetryCount() + 1);
        if (delivery.getRetryCount() < config.getMaxRetries()) {
            ledger.queue(delivery);
            return;
        }
        plugin.getLogger().severe("Giving up on " + delivery.getPurchaseId() + " for " + delivery.getPlayerName()
                + " after " + delivery.getRetryCount() + " attempts, it is kept under 'failed' in ledger.json: " + reason);
        ledger.markFailed(delivery, reason);
    }

    private DeliveryResult existingResult(String purchaseId) {
        if (ledger.isDelivered(purchaseId)) {
            return DeliveryResult.of(purchaseId, DeliveryStatus.ALREADY_DELIVERED);
        }
        return ledger.isQueued(purchaseId) ? DeliveryResult.of(purchaseId, DeliveryStatus.QUEUED) : null;
    }

    private void requireAllowed(List<String> commands) {
        commands.stream().filter(command -> !config.isCommandAllowed(command)).findFirst().ifPresent(command -> {
            plugin.getLogger().severe("Rejected delivery command outside delivery.allowed-commands: " + command);
            throw new IllegalArgumentException("Command is not allowed: " + command.split("\\s+", 2)[0]);
        });
    }

    private Duration duration(PurchaseRequest request) {
        if (request.getExpiresAt() != null) {
            try {
                Duration duration = Duration.between(LocalDateTime.now(), LocalDateTime.parse(request.getExpiresAt()));
                if (duration.isNegative() || duration.isZero()) {
                    throw new IllegalArgumentException("expiresAt is in the past");
                }
                return duration;
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("expiresAt is not an ISO date-time");
            }
        }
        String days = request.metadataValue("duration");
        if (days == null) {
            return DEFAULT_DURATION;
        }
        try {
            long parsed = (long) Double.parseDouble(days);
            if (parsed <= 0) {
                throw new IllegalArgumentException("duration must be a positive number of days");
            }
            return Duration.ofDays(parsed);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("duration must be a number of days");
        }
    }

    private static QueuedDelivery queued(String purchaseId, UUID playerUuid, String playerName, PurchaseRequest request, VoteReward vote) {
        return QueuedDelivery.builder()
                .purchaseId(purchaseId)
                .playerUuid(playerUuid)
                .playerName(playerName)
                .purchaseRequest(request)
                .voteReward(vote)
                .queuedAt(Instant.now().toString())
                .build();
    }

    private static String fill(String template, Map<String, String> placeholders) {
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private static void requirePurchaseId(String purchaseId) {
        if (purchaseId == null || purchaseId.isBlank()) {
            throw new IllegalArgumentException("purchaseId is required");
        }
    }

    private static UUID parseUuid(String uuid) {
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("A valid Minecraft UUID is required");
        }
    }
}
