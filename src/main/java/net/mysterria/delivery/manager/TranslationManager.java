package net.mysterria.delivery.manager;

import net.mysterria.delivery.MysterriaDelivery;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TranslationManager {

    private final MysterriaDelivery plugin;
    private final Map<String, FileConfiguration> translations = new HashMap<>();

    public TranslationManager(MysterriaDelivery plugin) {
        this.plugin = plugin;
        loadTranslations();
    }

    private void loadTranslations() {
        File translationsFolder = new File(plugin.getDataFolder(), "translations");
        if (!translationsFolder.exists()) {
            translationsFolder.mkdirs();
        }

        loadTranslation("en");
        loadTranslation("uk");
    }

    private void loadTranslation(String langCode) {
        File file = new File(plugin.getDataFolder(), "translations/" + langCode + ".yml");
        if (file.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            translations.put(langCode, config);
        }
    }

    public String getMessage(String langCode, String key) {
        FileConfiguration config = translations.get(langCode);
        if (config == null) {
            config = translations.get("en");
        }

        if (config != null) {
            return config.getString(key, getDefaultMessage(key));
        }

        return getDefaultMessage(key);
    }

    private String getDefaultMessage(String key) {
        return switch (key) {
            case "announcement.items" -> "<gray>[<gold>Mysterria</gold>]</gray> <green>{player}</green> <white>has purchased</white> <aqua>{service}</aqua><white>! Thank you for supporting the server!</white>";
            case "announcement.subscriptions" -> "<gray>[<gold>Mysterria</gold>]</gray> <green>{player}</green> <white>has subscribed to</white> <yellow>{service}</yellow><white>! Thank you for supporting Mysterria!</white>";
            case "announcement.permissions" -> "<gray>[<gold>Mysterria</gold>]</gray> <green>{player}</green> <white>has unlocked</white> <aqua>{service}</aqua><white>! Thank you for your support!</white>";
            case "announcement.discord_roles" -> "<gray>[<gold>Mysterria</gold>]</gray> <green>{player}</green> <white>has received the</white> <blue>{service}</blue> <white>Discord role! Thank you for supporting the community!</white>";
            case "announcement.dungeon_keys" -> "<gray>[<gold>Mysterria</gold>]</gray> <green>{player}</green> <white>has obtained</white> <light_purple>{service}</light_purple><white>! Thank you for your support!</white>";
            case "announcement.battlepass" -> "<gray>[<gold>Mysterria</gold>]</gray> <green>{player}</green> <white>has acquired the</white> <gold>{service}</gold><white>! We appreciate your support!</white>";
            case "announcement.cosmetics" -> "<gray>[<gold>Mysterria</gold>]</gray> <green>{player}</green> <white>has unlocked</white> <light_purple>{service}</light_purple><white>! Thank you for supporting the server!</white>";
            case "announcement.other" -> "<gray>[<gold>Mysterria</gold>]</gray> <green>{player}</green> <white>has purchased</white> <white>{service}</white><white>! Thank you for your support!</white>";
            case "announcement.fallback" -> "<gray>[<gold>Mysterria</gold>]</gray> <green>{player}</green> <white>supported the server by purchasing</white> <aqua>{service}</aqua><white>. Thank you!</white>";
            case "announcement.vote" -> "<gray>[<gold>Mysterria</gold>]</gray> <green>{player}</green> <white>has voted for </white><blue>Mysterria</blue> <white>and received {amount} Brilliant Emporium points for that!</white>";
            case "delivery.success" -> "<gray>[<gold>Mysterria</gold>]</gray> <green>Your purchase has been delivered successfully!</green>";
            case "delivery.queued" -> "<gray>[<gold>Mysterria</gold>]</gray> <yellow>You have pending deliveries that will be processed shortly.</yellow>";
            case "delivery.failed" -> "<gray>[<gold>Mysterria</gold>]</gray> <red>Failed to deliver your purchase. Please contact an administrator.</red>";
            default -> "<gray>[<gold>Mysterria</gold>]</gray> <green>{player}</green> <white>supported the server by purchasing</white> <aqua>{service}</aqua><white>. Thank you!</white>";
        };
    }

    public void reload() {
        translations.clear();
        loadTranslations();
    }
}