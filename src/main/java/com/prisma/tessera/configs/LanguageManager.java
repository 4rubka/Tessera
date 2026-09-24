package com.prisma.tessera.configs;

import com.prisma.tessera.TesseraPlugin;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;

public final class LanguageManager {

    private final TesseraPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private YamlConfiguration activeLangConfig;
    private YamlConfiguration fallbackLangConfig;
    private String currentLanguage = "en";

    public LanguageManager(TesseraPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        
        saveDefaultLanguageFile("lang/en.yml");
        saveDefaultLanguageFile("lang/uk.yml");
        saveDefaultLanguageFile("lang/de.yml");
        saveDefaultLanguageFile("lang/es.yml");

        this.currentLanguage = plugin.getConfig().getString("core.language", "en").toLowerCase();

        InputStream fallbackStream = plugin.getResource("lang/en.yml");
        if (fallbackStream != null) {
            this.fallbackLangConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(fallbackStream, StandardCharsets.UTF_8));
        }

        File langFile = new File(plugin.getDataFolder(), "lang/" + currentLanguage + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("[LanguageManager] Language file '" + currentLanguage + ".yml' not found, defaulting to 'en.yml'.");
            this.currentLanguage = "en";
            langFile = new File(plugin.getDataFolder(), "lang/en.yml");
        }

        if (langFile.exists()) {
            this.activeLangConfig = YamlConfiguration.loadConfiguration(langFile);
            plugin.getLogger().info("[LanguageManager] Loaded language: " + currentLanguage);
        } else {
            this.activeLangConfig = fallbackLangConfig != null ? fallbackLangConfig : new YamlConfiguration();
        }
    }

    private void saveDefaultLanguageFile(String resourcePath) {
        File targetFile = new File(plugin.getDataFolder(), resourcePath);
        if (!targetFile.exists()) {
            targetFile.getParentFile().mkdirs();
            try (InputStream in = plugin.getResource(resourcePath)) {
                if (in != null) {
                    java.nio.file.Files.copy(in, targetFile.toPath());
                }
            } catch (Exception ignored) {
            }
        }
    }

    public String getRaw(String path, String fallback) {
        if (activeLangConfig != null && activeLangConfig.contains(path)) {
            return activeLangConfig.getString(path);
        }
        if (fallbackLangConfig != null && fallbackLangConfig.contains(path)) {
            return fallbackLangConfig.getString(path);
        }
        return fallback;
    }

    public Component getComponent(String path, String fallback) {
        String raw = getRaw(path, fallback);
        return mm.deserialize(raw);
    }

    public Component getComponent(String path, String fallback, Map<String, String> placeholders) {
        String raw = getRaw(path, fallback);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                raw = raw.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return mm.deserialize(raw);
    }

    public void reload() {
        load();
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }
}
