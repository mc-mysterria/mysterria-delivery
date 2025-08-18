package net.mysterria.delivery.listener;

import net.mysterria.delivery.MysterriaDelivery;
import net.mysterria.delivery.manager.DeliveryManager;
import net.mysterria.delivery.manager.QueueManager;
import net.mysterria.delivery.model.QueuedDelivery;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.Objects;

public class PlayerJoinListener implements Listener {

    private final QueueManager queueManager;
    private final DeliveryManager deliveryManager;

    public PlayerJoinListener(QueueManager queueManager, DeliveryManager deliveryManager) {
        this.queueManager = queueManager;
        this.deliveryManager = deliveryManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        List<QueuedDelivery> queued = queueManager.getPlayerQueue(player.getUniqueId());

        if (!queued.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(MysterriaDelivery.getInstance(), () -> {
                for (QueuedDelivery delivery : queued) {
                    deliveryManager.processQueuedDelivery(delivery);
                }
            }, 60L);
        }
    }
}