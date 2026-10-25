package net.mysterria.delivery.domain.announcement.service;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.mysterria.delivery.MysterriaDelivery;
import net.mysterria.delivery.config.DeliveryConfig;
import net.mysterria.delivery.domain.delivery.model.source.ServiceCategory;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnouncementService {

    private static final String DEFAULT_LANGUAGE = "en";

    private final MysterriaDelivery plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, YamlConfiguration> translations = new HashMap<>();
    private DeliveryConfig config;

    public AnnouncementService(MysterriaDelivery plugin, DeliveryConfig config) {
        this.plugin = plugin;
        reload(config);
    }

    public void reload(DeliveryConfig config) {
        this.config = config;
        translations.clear();
        for (String language : new String[]{"en", "uk"}) {
            YamlConfiguration translation = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "translations/" + language + ".yml"));
            try (Reader defaults = new InputStreamReader(plugin.getResource("translations/" + language + ".yml"), StandardCharsets.UTF_8)) {
                translation.setDefaults(YamlConfiguration.loadConfiguration(defaults));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            translations.put(language, translation);
        }
    }

    public void announcePurchase(Player purchaser, ServiceCategory category, String service) {
        announce(purchaser, "announcement." + category.getTranslationKey(), Map.of("player", purchaser.getName(), "service", String.valueOf(service)));
    }

    public void announceVote(Player voter, String amount) {
        announce(voter, "announcement.vote", Map.of("player", voter.getName(), "amount", String.valueOf(amount)));
    }

    private void announce(Player subject, String key, Map<String, String> placeholders) {
        if (!config.isAnnouncementsEnabled()) {
            return;
        }
        for (Player viewer : config.isAnnouncementGlobal() ? Bukkit.getOnlinePlayers() : List.of(subject)) {
            String language = viewer.locale().getLanguage().equals("uk") ? "uk" : DEFAULT_LANGUAGE;
            String message = translations.get(language).getString(key, translations.get(DEFAULT_LANGUAGE).getString("announcement.fallback"));
            for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
                message = message.replace("{" + placeholder.getKey() + "}", miniMessage.escapeTags(placeholder.getValue()));
            }
            viewer.sendMessage(miniMessage.deserialize(message));
        }
    }
}
