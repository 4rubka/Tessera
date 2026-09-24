package com.prisma.tessera.utils;

import com.prisma.tessera.items.ItemUtils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

public final class ItemInspector {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void inspect(@NotNull CommandSender sender, @NotNull ItemStack item) {
        if (item.isEmpty() || item.getType().isAir()) {
            sender.sendMessage(MM.deserialize("<red>No item in hand to inspect.</red>"));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        String tesseraId = ItemUtils.getTesseraId(item);

        sender.sendMessage(MM.deserialize("<gradient:#00c6ff:#0072ff><bold>════════ Tessera Item Inspector ════════</bold></gradient>"));
        sender.sendMessage(MM.deserialize("<gray>Type: <white>" + item.getType().name() + "</white>  <dark_gray>│</dark_gray>  Amount: <yellow>" + item.getAmount() + "</yellow></gray>"));

        if (meta != null && meta.hasDisplayName()) {
            sender.sendMessage(MM.deserialize("<gray>Display Name: </gray>").append(meta.displayName()));
        }

        if (tesseraId != null) {
            Component idComp = MM.deserialize("<gray>Tessera ID: <aqua><bold>" + tesseraId + "</bold></aqua> <dark_gray>[Click to Copy]</dark_gray></gray>")
                    .clickEvent(ClickEvent.copyToClipboard(tesseraId))
                    .hoverEvent(HoverEvent.showText(MM.deserialize("<green>Click to copy ID: " + tesseraId + "</green>")));
            sender.sendMessage(idComp);
        } else {
            sender.sendMessage(MM.deserialize("<gray>Tessera ID: <dark_gray>None (Vanilla or External)</dark_gray></gray>"));
        }

        if (meta != null && meta.hasCustomModelData()) {
            sender.sendMessage(MM.deserialize("<gray>CustomModelData: <gold>" + meta.getCustomModelData() + "</gold></gray>"));
        }

        if (meta instanceof Damageable dmg) {
            int maxDurability = item.getType().getMaxDurability();
            if (maxDurability > 0) {
                int currentDamage = dmg.getDamage();
                int remaining = maxDurability - currentDamage;
                sender.sendMessage(MM.deserialize("<gray>Durability: <green>" + remaining + "</green><gray>/</gray><dark_green>" + maxDurability + "</dark_green> <dark_gray>(Damage: " + currentDamage + ")</dark_gray></gray>"));
            }
        }

        if (meta != null && meta.isUnbreakable()) {
            sender.sendMessage(MM.deserialize("<gray>Unbreakable: <green>true</green></gray>"));
        }

        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            Set<NamespacedKey> keys = pdc.getKeys();
            if (!keys.isEmpty()) {
                sender.sendMessage(MM.deserialize("<gradient:#00c6ff:#0072ff>── Persistent Data Container (" + keys.size() + ") ──</gradient>"));
                for (NamespacedKey k : keys) {
                    Component keyComp = MM.deserialize("<dark_gray> » </dark_gray><yellow>" + k.getNamespace() + ":" + k.getKey() + "</yellow>")
                            .hoverEvent(HoverEvent.showText(MM.deserialize("<gray>NamespacedKey: " + k + "</gray>")));
                    sender.sendMessage(keyComp);
                }
            }
        }

        try {
            if (item.hasData(DataComponentTypes.MAX_STACK_SIZE)) {
                sender.sendMessage(MM.deserialize("<gray>DataComponent MaxStackSize: <aqua>" + item.getData(DataComponentTypes.MAX_STACK_SIZE) + "</aqua></gray>"));
            }
            if (item.hasData(DataComponentTypes.MAX_DAMAGE)) {
                sender.sendMessage(MM.deserialize("<gray>DataComponent MaxDamage: <aqua>" + item.getData(DataComponentTypes.MAX_DAMAGE) + "</aqua></gray>"));
            }
            if (item.hasData(DataComponentTypes.FOOD)) {
                sender.sendMessage(MM.deserialize("<gray>DataComponent Food: <aqua>Present</aqua></gray>"));
            }
        } catch (Throwable ignored) {
            
        }

        sender.sendMessage(MM.deserialize("<gradient:#00c6ff:#0072ff><bold>════════════════════════════════════════</bold></gradient>"));
    }
}
