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
        return "<gray>[<gold>Mysterria</gold>]</gray> <white>Purchase delivered!</white>";
    }
    
    public void reload() {
        translations.clear();
        loadTranslations();
    }
}