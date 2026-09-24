package com.prisma.tessera.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public final class BlockParser {

    private final ConfigurationSection section;

    public BlockParser(@NotNull ConfigurationSection section) {
        this.section = section;
    }

    public BlockTemplate parse() {
        String id = section.getName();
        String materialName = section.getString("block_material", section.getString("material", "NOTE_BLOCK"));
        Material material = Material.matchMaterial(materialName.toUpperCase());
        if (material == null) {
            material = Material.NOTE_BLOCK;
        }

        String displayName = section.getString("display_name", id);
        float hardness = (float) section.getDouble("hardness", 1.0);
        String requiredTool = section.getString("required_tool", "ANY");
        String minimumTier = section.getString("minimum_tier", "NONE");

        ConfigurationSection soundSection = section.getConfigurationSection("sounds");
        String placeSound = soundSection != null ? soundSection.getString("place") : section.getString("place_sound");
        String breakSound = soundSection != null ? soundSection.getString("break") : section.getString("break_sound");
        String stepSound = soundSection != null ? soundSection.getString("step") : section.getString("step_sound");
        String hitSound = soundSection != null ? soundSection.getString("hit") : section.getString("hit_sound");

        int lightLevel = section.getInt("light_level", 0);
        boolean cancelDrop = section.getBoolean("cancel_drop", false);
        int customModelData = section.getInt("custom_model_data", 0);

        List<BlockTemplate.BlockDrop> drops = parseDrops(id);

        return new BlockTemplate(
                id,
                material,
                displayName,
                hardness,
                requiredTool,
                minimumTier,
                placeSound,
                breakSound,
                stepSound,
                hitSound,
                lightLevel,
                cancelDrop,
                customModelData,
                drops,
                section
        );
    }

    private List<BlockTemplate.BlockDrop> parseDrops(String blockId) {
        List<BlockTemplate.BlockDrop> drops = new ArrayList<>();

        if (section.isList("drops")) {
            List<?> rawList = section.getList("drops");
            if (rawList != null) {
                for (Object item : rawList) {
                    if (item instanceof Map<?, ?> map) {
                        Object itemObj = map.get("item");
                        String itemId = itemObj != null ? itemObj.toString() : blockId;
                        int min = map.containsKey("min") ? Integer.parseInt(map.get("min").toString()) : 1;
                        int max = map.containsKey("max") ? Integer.parseInt(map.get("max").toString()) : min;
                        double chance = map.containsKey("chance") ? Double.parseDouble(map.get("chance").toString()) : 1.0;
                        drops.add(new BlockTemplate.BlockDrop(itemId, min, max, chance));
                    } else if (item instanceof String str) {
                        drops.add(new BlockTemplate.BlockDrop(str, 1, 1, 1.0));
                    }
                }
            }
        } else if (section.isConfigurationSection("drops")) {
            ConfigurationSection dropSec = section.getConfigurationSection("drops");
            if (dropSec != null) {
                for (String key : dropSec.getKeys(false)) {
                    ConfigurationSection sub = dropSec.getConfigurationSection(key);
                    if (sub != null) {
                        String itemId = sub.getString("item", key);
                        int min = sub.getInt("min", 1);
                        int max = sub.getInt("max", min);
                        double chance = sub.getDouble("chance", 1.0);
                        drops.add(new BlockTemplate.BlockDrop(itemId, min, max, chance));
                    }
                }
            }
        }

        if (drops.isEmpty() && !section.getBoolean("cancel_drop", false)) {
            drops.add(new BlockTemplate.BlockDrop(blockId, 1, 1, 1.0));
        }

        return drops;
    }
}
