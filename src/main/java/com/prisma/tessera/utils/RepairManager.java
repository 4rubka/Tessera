package com.prisma.tessera.utils;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public final class RepairManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void repairHand(@NotNull Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.isEmpty() || item.getType().isAir()) {
            player.sendMessage(MM.deserialize("<red>You are not holding any item to repair!</red>"));
            return;
        }

        if (repairItem(item)) {
            player.playSound(player.getLocation(), "block.anvil.use", 1.0f, 1.2f);
            player.sendMessage(MM.deserialize("<green>Successfully repaired your held item to full durability!</green>"));
        } else {
            player.sendMessage(MM.deserialize("<yellow>This item cannot take damage or is already at full durability.</yellow>"));
        }
    }

    public static void repairAll(@NotNull Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !item.isEmpty() && !item.getType().isAir()) {
                if (repairItem(item)) {
                    count++;
                }
            }
        }

        if (count > 0) {
            player.playSound(player.getLocation(), "block.anvil.use", 1.0f, 1.2f);
            player.sendMessage(MM.deserialize("<green>Successfully repaired <yellow>" + count + "</yellow> damaged items in your inventory!</green>"));
        } else {
            player.sendMessage(MM.deserialize("<yellow>No damaged items were found in your inventory.</yellow>"));
        }
    }

    public static boolean repairItem(@NotNull ItemStack item) {
        boolean repaired = false;

        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable dmg) {
            if (dmg.hasDamage()) {
                dmg.setDamage(0);
                item.setItemMeta(meta);
                repaired = true;
            }
        }

        try {
            if (item.hasData(DataComponentTypes.DAMAGE)) {
                Integer currentDmg = item.getData(DataComponentTypes.DAMAGE);
                if (currentDmg != null && currentDmg > 0) {
                    item.setData(DataComponentTypes.DAMAGE, 0);
                    repaired = true;
                }
            }
        } catch (Throwable ignored) {
        }

        return repaired;
    }
}
