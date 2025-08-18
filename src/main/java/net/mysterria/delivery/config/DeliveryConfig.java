package net.mysterria.delivery.config;

import lombok.Getter;
import net.mysterria.delivery.MysterriaDelivery;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
public class DeliveryConfig {

    private final boolean announcementsEnabled;
    private final boolean announcementGlobal;
    private final int maxRetries;
    private final long deliveryDelayTicks;

    public DeliveryConfig(MysterriaDelivery plugin) {
        FileConfiguration config = plugin.getConfig();

        this.announcementsEnabled = config.getBoolean("announcements.enabled", true);
        this.announcementGlobal = config.getBoolean("announcements.global", true);
        this.maxRetries = config.getInt("delivery.maxRetries", 3);
        this.deliveryDelayTicks = config.getLong("delivery.delayTicks", 60L);
    }
}