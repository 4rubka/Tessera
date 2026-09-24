package com.prisma.tessera.utils;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

public final class HatManager {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void toggleHat(@NotNull Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack handItem = inv.getItemInMainHand();
        ItemStack helmetItem = inv.getHelmet();

        if ((handItem.isEmpty() || handItem.getType().isAir()) && (helmetItem == null || helmetItem.isEmpty() || helmetItem.getType().isAir())) {
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            player.sendMessage(MM.deserialize("<red>Hold an item in your hand to wear it as a hat!</red>"));
            return;
        }

        if (handItem.isEmpty() || handItem.getType().isAir()) {
            inv.setItemInMainHand(helmetItem);
            inv.setHelmet(null);
            player.playSound(player.getLocation(), "item.armor.equip_generic", 1.0f, 1.0f);
            player.sendMessage(MM.deserialize("<green>Took off your hat and placed it in your hand.</green>"));
            return;
        }

        ItemStack wearStack = handItem.clone();
        wearStack.setAmount(1);

        if (handItem.getAmount() > 1) {
            handItem.setAmount(handItem.getAmount() - 1);
            inv.setItemInMainHand(handItem);
            if (helmetItem != null && !helmetItem.getType().isAir()) {
                inv.addItem(helmetItem);
            }
        } else {
            inv.setItemInMainHand(helmetItem != null ? helmetItem : ItemStack.empty());
        }

        inv.setHelmet(wearStack);
        player.playSound(player.getLocation(), "item.armor.equip_diamond", 1.0f, 1.2f);
        player.sendMessage(MM.deserialize("<gradient:#00c6ff:#0072ff>✦ Successfully equipped your cosmetic hat! ✦</gradient>"));
    }
}
