package com.prisma.tessera.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class ItemBuilder {

    public static final NamespacedKey TESSERA_ID_KEY = new NamespacedKey("tessera", "item_id");

    private final ItemStack itemStack;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.meta = itemStack.getItemMeta();
    }

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        this.meta = this.itemStack.getItemMeta();
    }

    public ItemBuilder setTesseraId(String id) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(TESSERA_ID_KEY, PersistentDataType.STRING, id);
        return this;
    }

    private static Component parseComponent(String miniMessage) {
        String msg = miniMessage;
        if (!msg.contains("<!italic>") && !msg.contains("<italic:false>") && !msg.contains("<italic>") && !msg.contains("<i>")) {
            msg = "<!italic>" + msg;
        }
        Component comp;
        if (com.prisma.tessera.fonts.FontManager.get() != null) {
            comp = com.prisma.tessera.fonts.FontManager.get().format(msg);
        } else {
            comp = MiniMessage.miniMessage().deserialize(msg);
        }
        return comp.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
    }

    public ItemBuilder setName(String miniMessage) {
        meta.displayName(parseComponent(miniMessage));
        return this;
    }

    public ItemBuilder setLore(List<String> miniMessageLines) {
        List<Component> lore = new ArrayList<>();
        for (String line : miniMessageLines) {
            lore.add(parseComponent(line));
        }
        meta.lore(lore);
        return this;
    }

    public ItemBuilder addLore(String miniMessageLine) {
        List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(parseComponent(miniMessageLine));
        meta.lore(lore);
        return this;
    }

    public ItemBuilder setCustomModelData(int cmd) {
        meta.setCustomModelData(cmd);
        return this;
    }

    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemBuilder addItemFlags(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder setUnbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }

    public ItemBuilder setMaxStackSize(int max) {
        meta.setMaxStackSize(max);
        return this;
    }

    public <T> ItemBuilder setPersistentData(NamespacedKey key, PersistentDataType<T, T> type, T value) {
        meta.getPersistentDataContainer().set(key, type, value);
        return this;
    }

    public ItemStack build() {
        itemStack.setItemMeta(meta);
        return itemStack;
    }
}
