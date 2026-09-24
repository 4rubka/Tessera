package com.prisma.tessera.items;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public final class ItemTemplate {

    private final String id;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final int customModelData;
    private final boolean unbreakable;
    private final int maxStackSize;
    private final ConfigurationSection section;

    public ItemTemplate(String id, ConfigurationSection section) {
        this.id = id;
        this.section = section;
        this.material = Material.valueOf(section.getString("material", "STONE").toUpperCase());
        this.displayName = section.getString("display_name", id);
        this.lore = section.getStringList("lore");
        this.customModelData = section.getInt("custom_model_data", 0);
        this.unbreakable = section.getBoolean("unbreakable", false);
        this.maxStackSize = section.getInt("max_stack_size", -1);
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public ConfigurationSection getSection() {
        return section;
    }

    public ItemStack build() {
        ItemBuilder builder = new ItemBuilder(material);
        builder.setTesseraId(id);
        if (displayName != null && !displayName.isEmpty()) {
            builder.setName(displayName);
        }
        if (lore != null && !lore.isEmpty()) {
            builder.setLore(lore);
        }
        if (customModelData > 0) {
            builder.setCustomModelData(customModelData);
        }
        if (unbreakable) {
            builder.setUnbreakable(true);
        }
        if (maxStackSize > 0) {
            builder.setMaxStackSize(maxStackSize);
        }
        return builder.build();
    }
}
