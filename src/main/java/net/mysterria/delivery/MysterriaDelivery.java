package net.mysterria.delivery;

import dev.ua.ikeepcalm.catwalk.hub.webserver.services.CatWalkWebserverService;
import lombok.Getter;
import net.luckperms.api.LuckPerms;
import net.mysterria.delivery.command.admin.DeliveryCommand;
import net.mysterria.delivery.config.DeliveryConfig;
import net.mysterria.delivery.domain.announcement.service.AnnouncementService;
import net.mysterria.delivery.domain.delivery.endpoint.DeliveryEndpoints;
import net.mysterria.delivery.domain.delivery.listener.PlayerJoinListener;
import net.mysterria.delivery.domain.delivery.service.DeliveryLedger;
import net.mysterria.delivery.domain.delivery.service.DeliveryService;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

@Getter
public class MysterriaDelivery extends JavaPlugin {

    private DeliveryLedger ledger;
    private AnnouncementService announcements;
    private DeliveryService deliveryService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        for (String language : List.of("en", "uk")) {
            if (!new File(getDataFolder(), "translations/" + language + ".yml").exists()) {
                saveResource("translations/" + language + ".yml", false);
            }
        }

        LuckPerms luckPerms = getServer().getServicesManager().load(LuckPerms.class);
        CatWalkWebserverService webserver = getServer().getServicesManager().load(CatWalkWebserverService.class);
        if (luckPerms == null || webserver == null) {
            getLogger().severe("LuckPerms or CatWalk is not available, disabling");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        DeliveryConfig config = DeliveryConfig.load(getConfig());
        ledger = new DeliveryLedger(getDataFolder(), getLogger());
        announcements = new AnnouncementService(this, config);
        deliveryService = new DeliveryService(this, luckPerms, ledger, announcements, config);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, deliveryService), this);
        new DeliveryCommand(this);
        webserver.registerHandlers(new DeliveryEndpoints(deliveryService, getLogger()));

        if (config.getAllowedCommands().isEmpty()) {
            getLogger().warning("delivery.allowed-commands is empty: anyone holding the CatWalk key can run any console command through /delivery/item and /delivery/vote");
        }
    }

    public void reload() {
        reloadConfig();
        DeliveryConfig config = DeliveryConfig.load(getConfig());
        announcements.reload(config);
        deliveryService.setConfig(config);
    }
}
