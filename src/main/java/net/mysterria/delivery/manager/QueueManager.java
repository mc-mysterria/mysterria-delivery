package net.mysterria.delivery.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mysterria.delivery.MysterriaDelivery;
import net.mysterria.delivery.model.PurchaseRequest;
import net.mysterria.delivery.model.QueuedDelivery;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class QueueManager {
    
    private final MysterriaDelivery plugin;
    private final Map<String, QueuedDelivery> queue = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    private final File queueFile;
    
    public QueueManager(MysterriaDelivery plugin) {
        this.plugin = plugin;
        this.queueFile = new File(plugin.getDataFolder(), "queue.json");
    }
    
    public void queueDelivery(PurchaseRequest request) {
        UUID playerUuid = UUID.fromString(request.getMinecraftUuid());
        
        QueuedDelivery queued = QueuedDelivery.builder()
            .purchaseId(request.getPurchaseId())
            .playerUuid(playerUuid)
            .playerName(request.getNickname())
            .purchaseRequest(request)
            .queuedAt(LocalDateTime.now())
            .retryCount(0)
            .build();
        
        queue.put(request.getPurchaseId(), queued);
        saveQueue();
        
        plugin.getLogger().info("Queued delivery for offline player: " + request.getNickname());
    }
    
    public List<QueuedDelivery> getPlayerQueue(UUID playerUuid) {
        return queue.values().stream()
            .filter(q -> q.getPlayerUuid().equals(playerUuid))
            .toList();
    }
    
    public void removeFromQueue(String purchaseId) {
        queue.remove(purchaseId);
        saveQueue();
    }
    
    public void saveQueue() {
        try (FileWriter writer = new FileWriter(queueFile)) {
            gson.toJson(queue.values(), writer);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save queue", e);
        }
    }
    
    public void loadQueue() {
        if (!queueFile.exists()) {
            return;
        }
        
        try (FileReader reader = new FileReader(queueFile)) {
            Type type = new TypeToken<List<QueuedDelivery>>(){}.getType();
            List<QueuedDelivery> loaded = gson.fromJson(reader, type);
            
            if (loaded != null) {
                queue.clear();
                for (QueuedDelivery queued : loaded) {
                    queue.put(queued.getPurchaseId(), queued);
                }
                plugin.getLogger().info("Loaded " + queue.size() + " queued deliveries");
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load queue", e);
        }
    }
}