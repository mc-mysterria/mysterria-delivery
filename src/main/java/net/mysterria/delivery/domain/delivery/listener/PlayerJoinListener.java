package net.mysterria.delivery.domain.delivery.listener;

import lombok.RequiredArgsConstructor;
import net.mysterria.delivery.MysterriaDelivery;
import net.mysterria.delivery.domain.delivery.service.DeliveryService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@RequiredArgsConstructor
public class PlayerJoinListener implements Listener {

    private final MysterriaDelivery plugin;
    private final DeliveryService deliveryService;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                deliveryService.deliverQueued(player);
            }
        }, plugin.getConfig().getLong("delivery.delayTicks"));
    }
}
