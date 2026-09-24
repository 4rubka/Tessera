package com.prisma.tessera.recipes;

import com.prisma.tessera.items.ItemUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Keyed;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public final class RecipeListener implements Listener {

    private final RecipeManager recipeManager;

    public RecipeListener(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null) {
            return;
        }

        CraftingInventory inv = event.getInventory();

        if (recipe instanceof Keyed keyed) {
            
            if (recipeManager.isVanillaDisabled(keyed.getKey())) {
                inv.setResult(null);
                return;
            }

            if ("minecraft".equals(keyed.getKey().getNamespace())) {
                for (ItemStack matrixItem : inv.getMatrix()) {
                    if (matrixItem != null && ItemUtils.isTesseraItem(matrixItem)) {
                        inv.setResult(null);
                        return;
                    }
                }
            }

            if ("tessera".equals(keyed.getKey().getNamespace())) {
                CustomRecipe customRecipe = recipeManager.getRecipe(keyed.getKey());
                if (customRecipe != null && customRecipe.getPermission() != null) {
                    for (HumanEntity viewer : event.getViewers()) {
                        if (!viewer.hasPermission(customRecipe.getPermission())) {
                            inv.setResult(null);
                            return;
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe instanceof Keyed keyed && "tessera".equals(keyed.getKey().getNamespace())) {
            CustomRecipe customRecipe = recipeManager.getRecipe(keyed.getKey());
            if (customRecipe != null && customRecipe.getPermission() != null) {
                HumanEntity who = event.getWhoClicked();
                if (!who.hasPermission(customRecipe.getPermission())) {
                    event.setCancelled(true);
                    if (who instanceof Player player) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have permission to craft this item!"));
                    }
                }
            }
        }
    }
}
