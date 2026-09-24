package com.prisma.tessera.items;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

public final class ItemUtils {

    @Nullable
    public static String getTesseraId(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return null;
        }
        PersistentDataContainer pdc = itemStack.getItemMeta().getPersistentDataContainer();
        return pdc.get(ItemBuilder.TESSERA_ID_KEY, PersistentDataType.STRING);
    }

    public static boolean isTesseraItem(ItemStack itemStack) {
        return getTesseraId(itemStack) != null;
    }
}
