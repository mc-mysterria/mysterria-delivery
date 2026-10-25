package net.mysterria.delivery.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Locale;

@Getter
@RequiredArgsConstructor
public class DeliveryConfig {

    private final boolean announcementsEnabled;
    private final boolean announcementGlobal;
    private final int maxRetries;
    private final long joinDelayTicks;
    private final List<String> allowedCommands;

    public static DeliveryConfig load(FileConfiguration config) {
        return new DeliveryConfig(
                config.getBoolean("announcements.enabled"),
                config.getBoolean("announcements.global"),
                config.getInt("delivery.maxRetries"),
                config.getLong("delivery.delayTicks"),
                config.getStringList("delivery.allowed-commands").stream().map(command -> command.toLowerCase(Locale.ROOT)).toList()
        );
    }

    public boolean isCommandAllowed(String command) {
        if (allowedCommands.isEmpty()) {
            return true;
        }
        String label = command.strip().replaceFirst("^/", "").split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        return allowedCommands.contains(label);
    }
}
