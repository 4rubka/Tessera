package com.prisma.tessera.items;

import com.prisma.tessera.TesseraPlugin;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ItemRegistry {

    private final TesseraPlugin plugin;
    private final Map<String, ItemTemplate> items = new LinkedHashMap<>();

    public ItemRegistry(TesseraPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        for (YamlConfiguration config : plugin.getConfigManager().getConfigsInDirectory("items").values()) {
            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section != null) {
                    register(key, new ItemParser(section).parse());
                }
            }
        }
    }

    public void register(@NotNull String id, @NotNull ItemTemplate template) {
        items.put(id, template);
    }

    public void unregister(@NotNull String id) {
        items.remove(id);
    }

    @Nullable
    public ItemTemplate getTemplate(@NotNull String id) {
        return items.get(id);
    }

    @Nullable
    public ItemStack getItem(@NotNull String id) {
        ItemTemplate template = items.get(id);
        return template != null ? template.build() : null;
    }

    public boolean exists(@NotNull String id) {
        return items.containsKey(id);
    }

    @NotNull
    public Set<String> getIds() {
        return Collections.unmodifiableSet(items.keySet());
    }

    @NotNull
    public Collection<ItemTemplate> getAll() {
        return Collections.unmodifiableCollection(items.values());
    }

    @Nullable
    public ItemTemplate getByItemStack(@NotNull ItemStack itemStack) {
        String id = ItemUtils.getTesseraId(itemStack);
        return id != null ? items.get(id) : null;
    }
}
