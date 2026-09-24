package com.prisma.tessera.gui;

import com.prisma.tessera.TesseraPlugin;
import com.prisma.tessera.items.ItemBuilder;
import com.prisma.tessera.recipes.CustomRecipe;
import com.prisma.tessera.recipes.RecipeManager;
import com.prisma.tessera.recipes.RecipeType;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RecipeGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int[] MATRIX_SLOTS = {
            10, 11, 12,
            19, 20, 21,
            28, 29, 30
    };

    public static void open(@NotNull Player player, @NotNull String itemId) {
        RecipeManager rm = TesseraPlugin.get().getRecipeManager();
        List<CustomRecipe> recipes = rm.getRecipesForResult(itemId);

        if (recipes.isEmpty()) {
            CustomRecipe single = rm.getRecipe(itemId);
            if (single != null) {
                recipes = List.of(single);
            }
        }

        if (recipes.isEmpty()) {
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
            player.sendMessage(MM.deserialize("<red>No crafting recipes found for <yellow>" + itemId + "</yellow>.</red>"));
            return;
        }

        open(player, recipes, 0, itemId);
    }

    public static void open(@NotNull Player player, @NotNull List<CustomRecipe> recipes, int index, @Nullable String searchId) {
        if (recipes.isEmpty()) {
            return;
        }

        int currentIndex = Math.max(0, Math.min(index, recipes.size() - 1));
        CustomRecipe recipe = recipes.get(currentIndex);
        ItemStack result = recipe.getResult();

        String itemName = result.getItemMeta() != null && result.getItemMeta().hasDisplayName()
                ? MM.serialize(result.getItemMeta().displayName())
                : result.getType().name();

        String title = "<white>:tessera_badge:</white> <gradient:#00c6ff:#0072ff><bold>Recipe Codex</bold></gradient> <dark_gray>»</dark_gray> " +
                itemName + " <dark_gray>(<yellow>" + (currentIndex + 1) + "</yellow>/<gray>" + recipes.size() + "</gray>)</dark_gray>";

        TesseraGUI gui = new TesseraGUI(title, 5);

        ItemStack bg = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName("<gray> </gray>").build();
        ItemStack matrixEmpty = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE).setName("<dark_gray>Empty Slot</dark_gray>").build();

        for (int i = 0; i < 45; i++) {
            gui.setButton(i, GUIButton.of(bg));
        }

        ItemStack bannerItem = new ItemBuilder(Material.CYAN_BANNER)
                .setName("<white>:tessera_banner:</white>")
                .setLore(List.of(
                        "<gradient:#00c6ff:#0072ff><bold>✦ TESSERA RECIPE WORKBENCH ✦</bold></gradient>",
                        "<gray>Crafting specification for: </gray>" + itemName,
                        "<gray>Recipe Type: <aqua>" + recipe.getType().name() + "</aqua></gray>",
                        "<dark_gray>────────────────────────────────────</dark_gray>",
                        "<gray>❖ Ingredients: <yellow>" + (recipe.getType() == RecipeType.SHAPED ? recipe.getShapedIngredients().size() : recipe.getShapelessIngredients().size()) + "</yellow> components</gray>",
                        "<dark_gray>────────────────────────────────────</dark_gray>",
                        "<yellow>▶ Click to return to Catalog</yellow>"
                ))
                .build();
        gui.setButton(4, GUIButton.of(bannerItem, p -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            ItemBrowserGUI.open(p);
        }));

        for (int slot : MATRIX_SLOTS) {
            gui.setButton(slot, GUIButton.of(matrixEmpty));
        }

        RecipeType type = recipe.getType();
        if (type == RecipeType.SHAPED) {
            renderShaped(gui, recipe);
        } else if (type == RecipeType.SHAPELESS) {
            renderShapeless(gui, recipe);
        } else {
            renderSmeltingOrCutting(gui, recipe);
        }

        Material stationMat = switch (type) {
            case SHAPED, SHAPELESS -> Material.CRAFTING_TABLE;
            case FURNACE -> Material.FURNACE;
            case BLASTING -> Material.BLAST_FURNACE;
            case SMOKING -> Material.SMOKER;
            case CAMPFIRE -> Material.CAMPFIRE;
            case STONECUTTING -> Material.STONECUTTER;
            case SMITHING -> Material.SMITHING_TABLE;
            default -> Material.CRAFTING_TABLE;
        };

        ItemStack stationItem = new ItemBuilder(stationMat)
                .setName("<yellow><bold>" + type.name().replace('_', ' ') + "</bold></yellow>")
                .setLore(List.of("<gray>Station required to craft this item.</gray>"))
                .build();
        gui.setButton(14, GUIButton.of(stationItem));

        ItemStack arrowItem = new ItemBuilder(Material.ARROW)
                .setName("<aqua><bold>➜ Crafted Result</bold></aqua>")
                .build();
        gui.setButton(23, GUIButton.of(arrowItem));

        gui.setButton(25, GUIButton.of(result, (p, click) -> {
            if (p.hasPermission("tessera.admin")) {
                p.getInventory().addItem(result.clone());
                p.playSound(p.getLocation(), "entity.item.pickup", 1.0f, 1.2f);
                p.sendMessage(MM.deserialize("<green>Gave <yellow>" + result.getAmount() + "x</yellow> crafted item.</green>"));
            }
        }));

        ItemBuilder infoBuilder = new ItemBuilder(Material.KNOWLEDGE_BOOK)
                .setName("<gradient:#00c6ff:#0072ff><bold>Recipe Details</bold></gradient>")
                .addLore("<gray>Type: <aqua>" + type.name() + "</aqua></gray>");

        if (!recipe.getGroup().isEmpty()) {
            infoBuilder.addLore("<gray>Group: <yellow>" + recipe.getGroup() + "</yellow></gray>");
        }
        if (recipe.getCookingTime() > 0) {
            infoBuilder.addLore("<gray>Cook Time: <gold>" + (recipe.getCookingTime() / 20.0) + "s</gold> <dark_gray>(" + recipe.getCookingTime() + " ticks)</dark_gray></gray>");
        }
        if (recipe.getExperience() > 0) {
            infoBuilder.addLore("<gray>XP Yield: <green>" + recipe.getExperience() + " XP</green></gray>");
        }
        if (recipe.getPermission() != null && !recipe.getPermission().isEmpty()) {
            infoBuilder.addLore("<gray>Permission: <red>" + recipe.getPermission() + "</red></gray>");
        }
        gui.setButton(32, GUIButton.of(infoBuilder.build()));

        if (currentIndex > 0) {
            ItemStack prevItem = new ItemBuilder(Material.SPECTRAL_ARROW)
                    .setName("<aqua>« Previous Recipe</aqua>")
                    .setLore(List.of("<gray>Recipe " + currentIndex + " of " + recipes.size() + "</gray>"))
                    .build();
            gui.setButton(18, GUIButton.of(prevItem, (p, click) -> {
                p.playSound(p.getLocation(), "ui.button.click", 0.8f, 1.2f);
                open(p, recipes, currentIndex - 1, searchId);
            }));
        }

        if (currentIndex < recipes.size() - 1) {
            ItemStack nextItem = new ItemBuilder(Material.SPECTRAL_ARROW)
                    .setName("<aqua>Next Recipe »</aqua>")
                    .setLore(List.of("<gray>Recipe " + (currentIndex + 2) + " of " + recipes.size() + "</gray>"))
                    .build();
            gui.setButton(26, GUIButton.of(nextItem, (p, click) -> {
                p.playSound(p.getLocation(), "ui.button.click", 0.8f, 1.2f);
                open(p, recipes, currentIndex + 1, searchId);
            }));
        }

        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .setName("<gray>« Back to Catalog</gray>")
                .build();
        gui.setButton(36, GUIButton.of(backItem, (p, click) -> {
            p.playSound(p.getLocation(), "ui.button.click", 0.8f, 1.0f);
            ItemBrowserGUI.open(p);
        }));

        if (player.hasPermission("tessera.admin")) {
            ItemStack giveBtn = new ItemBuilder(Material.LIME_DYE)
                    .setName("<green><bold>Click to Obtain Result</bold></green>")
                    .setLore(List.of("<gray>Takes result stack directly into your inventory.</gray>"))
                    .build();
            gui.setButton(40, GUIButton.of(giveBtn, (p, click) -> {
                p.getInventory().addItem(result.clone());
                p.playSound(p.getLocation(), "entity.item.pickup", 1.0f, 1.2f);
                p.sendMessage(MM.deserialize("<green>Added crafted item to inventory.</green>"));
            }));
        }

        ItemStack closeBtn = new ItemBuilder(Material.BARRIER)
                .setName("<red><bold>Close</bold></red>")
                .build();
        gui.setButton(44, GUIButton.of(closeBtn, (p, click) -> {
            p.playSound(p.getLocation(), "ui.button.click", 0.8f, 0.8f);
            p.closeInventory();
        }));

        gui.open(player);
    }

    private static void renderShaped(TesseraGUI gui, CustomRecipe recipe) {
        String[] shape = recipe.getShape();
        if (shape == null || shape.length == 0) return;

        Map<Character, RecipeChoice> ingredients = recipe.getShapedIngredients();

        for (int r = 0; r < shape.length && r < 3; r++) {
            String row = shape[r];
            for (int c = 0; c < row.length() && c < 3; c++) {
                char ch = row.charAt(c);
                if (ch != ' ' && ingredients.containsKey(ch)) {
                    RecipeChoice choice = ingredients.get(ch);
                    ItemStack display = resolveChoice(choice);
                    if (display != null) {
                        int slot = MATRIX_SLOTS[r * 3 + c];
                        gui.setButton(slot, GUIButton.of(display));
                    }
                }
            }
        }
    }

    private static void renderShapeless(TesseraGUI gui, CustomRecipe recipe) {
        List<RecipeChoice> choices = recipe.getShapelessIngredients();
        for (int i = 0; i < choices.size() && i < MATRIX_SLOTS.length; i++) {
            RecipeChoice choice = choices.get(i);
            ItemStack display = resolveChoice(choice);
            if (display != null) {
                gui.setButton(MATRIX_SLOTS[i], GUIButton.of(display));
            }
        }
    }

    private static void renderSmeltingOrCutting(TesseraGUI gui, CustomRecipe recipe) {
        RecipeChoice source = recipe.getSourceChoice();
        if (source != null) {
            ItemStack display = resolveChoice(source);
            if (display != null) {
                
                gui.setButton(19, GUIButton.of(display));
            }
        }
    }

    @Nullable
    private static ItemStack resolveChoice(@Nullable RecipeChoice choice) {
        if (choice == null) return null;
        if (choice instanceof RecipeChoice.ExactChoice exact) {
            List<ItemStack> list = exact.getChoices();
            return !list.isEmpty() ? list.get(0).clone() : null;
        } else if (choice instanceof RecipeChoice.MaterialChoice mat) {
            List<Material> list = mat.getChoices();
            return !list.isEmpty() ? new ItemStack(list.get(0)) : null;
        }
        return choice.getItemStack();
    }
}
