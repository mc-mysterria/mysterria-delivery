package net.mysterria.delivery.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;
import net.mysterria.delivery.MysterriaDelivery;
import net.mysterria.delivery.config.DeliveryConfig;
import net.mysterria.delivery.model.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DeliveryManager {
    
    private final MysterriaDelivery plugin;
    private DeliveryConfig config;
    private final QueueManager queueManager;
    private final Set<String> processedPurchases = new HashSet<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    
    public DeliveryManager(MysterriaDelivery plugin, DeliveryConfig config, QueueManager queueManager) {
        this.plugin = plugin;
        this.config = config;
        this.queueManager = queueManager;
    }
    
    public void reload(DeliveryConfig newConfig) {
        this.config = newConfig;
    }
    
    public CompletableFuture<DeliveryResponse> processPurchase(PurchaseRequest request) {
        if (processedPurchases.contains(request.getPurchaseId())) {
            return CompletableFuture.completedFuture(
                DeliveryResponse.success(request.getPurchaseId(), "Purchase already processed")
            );
        }
        
        UUID playerUuid = UUID.fromString(request.getMinecraftUuid());
        Player player = Bukkit.getPlayer(playerUuid);
        
        switch (request.getServiceType()) {
            case ITEM, KEY, TOOL -> {
                if (player == null || !player.isOnline()) {
                    queueManager.queueDelivery(request);
                    return CompletableFuture.completedFuture(
                        DeliveryResponse.queued(request.getPurchaseId(), "Player offline, delivery queued")
                    );
                }
                return deliverItem(player, request);
            }
            case SUBSCRIPTION -> {
                return deliverSubscription(playerUuid, request);
            }
            case PERMISSION -> {
                return deliverPermission(playerUuid, request);
            }
            case DISCORD_ROLE -> {
                return deliverDiscordRole(request);
            }
            default -> {
                return CompletableFuture.completedFuture(
                    DeliveryResponse.error(request.getPurchaseId(), "Unknown service type: " + request.getServiceType())
                );
            }
        }
    }
    
    private CompletableFuture<DeliveryResponse> deliverItem(Player player, PurchaseRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> commands = extractCommands(request.getMetadata());
                
                if (commands.isEmpty()) {
                    plugin.getLogger().warning("No commands found in metadata for purchase: " + request.getPurchaseId());
                    return DeliveryResponse.error(request.getPurchaseId(), "No delivery commands configured");
                }
                
                int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (String commandTemplate : commands) {
                        String command = commandTemplate
                            .replace("{player}", player.getName())
                            .replace("{uuid}", player.getUniqueId().toString())
                            .replace("{quantity}", String.valueOf(quantity))
                            .replace("{service_name}", request.getServiceName());
                        
                        for (Map.Entry<String, Object> entry : request.getMetadata().entrySet()) {
                            command = command.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
                        }
                        
                        plugin.getLogger().info("Executing command: " + command);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    }
                    
                    announceDelivery(player, request);
                });
                
                processedPurchases.add(request.getPurchaseId());
                return DeliveryResponse.success(request.getPurchaseId(), "Item delivered successfully");
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to deliver item", e);
                return DeliveryResponse.error(request.getPurchaseId(), "Delivery failed: " + e.getMessage());
            }
        });
    }
    
    private CompletableFuture<DeliveryResponse> deliverSubscription(UUID playerUuid, PurchaseRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String groupName = extractGroupName(request.getMetadata());
                if (groupName == null) {
                    return DeliveryResponse.error(request.getPurchaseId(), "No group specified in metadata");
                }
                
                Duration duration = parseDuration(request);
                
                plugin.getLuckPerms().getUserManager().modifyUser(playerUuid, user -> {
                    Node node = InheritanceNode.builder(groupName)
                        .expiry(duration)
                        .build();
                    user.data().add(node);
                });
                
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> announceDelivery(player, request));
                }
                
                processedPurchases.add(request.getPurchaseId());
                plugin.getLogger().info("Granted subscription " + groupName + " to " + playerUuid + " for " + duration);
                
                return DeliveryResponse.success(request.getPurchaseId(), "Subscription granted successfully");
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to deliver subscription", e);
                return DeliveryResponse.error(request.getPurchaseId(), "Subscription delivery failed: " + e.getMessage());
            }
        });
    }
    
    private CompletableFuture<DeliveryResponse> deliverPermission(UUID playerUuid, PurchaseRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> permissions = extractPermissions(request.getMetadata());
                if (permissions.isEmpty()) {
                    return DeliveryResponse.error(request.getPurchaseId(), "No permissions specified in metadata");
                }
                
                Duration duration = parseDuration(request);
                
                plugin.getLuckPerms().getUserManager().modifyUser(playerUuid, user -> {
                    for (String permission : permissions) {
                        Node node = PermissionNode.builder(permission)
                            .value(true)
                            .expiry(duration)
                            .build();
                        user.data().add(node);
                    }
                });
                
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> announceDelivery(player, request));
                }
                
                processedPurchases.add(request.getPurchaseId());
                plugin.getLogger().info("Granted permissions " + permissions + " to " + playerUuid + " for " + duration);
                
                return DeliveryResponse.success(request.getPurchaseId(), "Permissions granted successfully");
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to deliver permissions", e);
                return DeliveryResponse.error(request.getPurchaseId(), "Permission delivery failed: " + e.getMessage());
            }
        });
    }
    
    private CompletableFuture<DeliveryResponse> deliverDiscordRole(PurchaseRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            plugin.getLogger().info("Discord role purchase: " + request.getServiceName() + " by " + request.getNickname());
            
            Player player = Bukkit.getPlayer(UUID.fromString(request.getMinecraftUuid()));
            if (player != null) {
                Bukkit.getScheduler().runTask(plugin, () -> announceDelivery(player, request));
            }
            
            processedPurchases.add(request.getPurchaseId());
            
            return DeliveryResponse.success(request.getPurchaseId(), "Discord role logged successfully");
        });
    }
    
    private void announceDelivery(Player purchaser, PurchaseRequest request) {
        if (!config.isAnnouncementsEnabled()) {
            return;
        }
        
        TranslationManager tm = plugin.getTranslationManager();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            Locale locale = player.locale();
            String langCode = locale.getLanguage().equals("uk") ? "uk" : "en";
            
            String messageKey = "announcement." + request.getServiceType().name().toLowerCase();
            String message = tm.getMessage(langCode, messageKey)
                .replace("{player}", purchaser.getName())
                .replace("{service}", request.getServiceName());
            
            Component component = miniMessage.deserialize(message);
            
            if (config.isAnnouncementGlobal()) {
                player.sendMessage(component);
            } else if (player.equals(purchaser)) {
                player.sendMessage(component);
            }
        }
    }
    
    public void processQueuedDelivery(QueuedDelivery queued) {
        Player player = Bukkit.getPlayer(queued.getPlayerUuid());
        if (player != null && player.isOnline()) {
            deliverItem(player, queued.getPurchaseRequest()).thenAccept(response -> {
                if (response.isSuccess()) {
                    queueManager.removeFromQueue(queued.getPurchaseId());
                } else {
                    queued.setRetryCount(queued.getRetryCount() + 1);
                    if (queued.getRetryCount() >= config.getMaxRetries()) {
                        plugin.getLogger().severe("Failed to deliver purchase after " + config.getMaxRetries() + " retries: " + queued.getPurchaseId());
                        queueManager.removeFromQueue(queued.getPurchaseId());
                    }
                }
            });
        }
    }
    
    @SuppressWarnings("unchecked")
    private List<String> extractCommands(Map<String, Object> metadata) {
        Object commands = metadata.get("commands");
        if (commands instanceof List) {
            return (List<String>) commands;
        } else if (commands instanceof String) {
            return List.of((String) commands);
        }
        return new ArrayList<>();
    }
    
    private String extractGroupName(Map<String, Object> metadata) {
        Object group = metadata.get("group");
        return group != null ? group.toString() : null;
    }
    
    @SuppressWarnings("unchecked")
    private List<String> extractPermissions(Map<String, Object> metadata) {
        Object permissions = metadata.get("permissions");
        if (permissions instanceof List) {
            return (List<String>) permissions;
        } else if (permissions instanceof String) {
            return List.of((String) permissions);
        }
        return new ArrayList<>();
    }
    
    private Duration parseDuration(PurchaseRequest request) {
        if (request.getExpiresAt() != null) {
            try {
                LocalDateTime expiresAt = LocalDateTime.parse(request.getExpiresAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return Duration.between(LocalDateTime.now(), expiresAt);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse expiresAt: " + request.getExpiresAt());
            }
        }
        
        Object durationObj = request.getMetadata().get("duration");
        if (durationObj != null) {
            return Duration.ofDays(Long.parseLong(durationObj.toString()));
        }
        
        return Duration.ofDays(30);
    }
}