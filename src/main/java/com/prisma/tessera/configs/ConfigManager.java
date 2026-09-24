package com.prisma.tessera.configs;

import com.prisma.tessera.TesseraPlugin;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ConfigManager {

    private final TesseraPlugin plugin;
    private final Map<String, YamlConfiguration> configs = new HashMap<>();

    public ConfigManager(TesseraPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        saveDefaultResource("glyphs/default_glyphs.yml");
        saveDefaultResource("items/ruby.yml");
        saveDefaultResource("blocks/ruby_block.yml");
        saveDefaultResource("furniture/oak_chair.yml");
        saveDefaultResource("recipes/ruby_recipes.yml");
        saveDefaultResource("hud/default_hud.yml");

        loadModuleDirectory("items");
        loadModuleDirectory("blocks");
        loadModuleDirectory("furniture");
        loadModuleDirectory("recipes");
        loadModuleDirectory("fonts");
        loadModuleDirectory("glyphs");
        loadModuleDirectory("hud");
    }

    private void saveDefaultResource(String path) {
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            try {
                plugin.saveResource(path, false);
            } catch (Exception ignored) {
            }
        }
    }

    private void loadModuleDirectory(String module) {
        File dir = new File(plugin.getDataFolder(), module);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        loadDirectoryRecursive(dir, module);
    }

    private void loadDirectoryRecursive(File dir, String basePrefix) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                loadDirectoryRecursive(file, basePrefix + "/" + file.getName());
            } else if (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml")) {
                String key = basePrefix + "/" + file.getName();
                configs.put(key, YamlConfiguration.loadConfiguration(file));
            }
        }
    }

    public YamlConfiguration getConfig(String path) {
        return configs.get(path);
    }

    public Map<String, YamlConfiguration> getConfigsInDirectory(String directory) {
        Map<String, YamlConfiguration> result = new HashMap<>();
        for (Map.Entry<String, YamlConfiguration> entry : configs.entrySet()) {
            if (entry.getKey().startsWith(directory + "/")) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public void reload() {
        configs.clear();
        load();
    }
}
