package com.prisma.tessera.recipes;

import com.prisma.tessera.TesseraPlugin;
import com.prisma.tessera.api.Tessera;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RecipeParser {

    private final TesseraPlugin plugin;
    private final String id;
    private final ConfigurationSection section;

    public RecipeParser(@NotNull TesseraPlugin plugin, @NotNull String id, @NotNull ConfigurationSection section) {
        this.plugin = plugin;
        this.id = id;
        this.section = section;
    }

    @Nullable
    public CustomRecipe parse() {
        String typeStr = section.getString("type", "SHAPED").toUpperCase();
        RecipeType type;
        try {
            type = RecipeType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            type = RecipeType.SHAPED;
        }

        ItemStack result = parseResult();
        if (result == null) {
            return null;
        }

        NamespacedKey key = new NamespacedKey(plugin, id);
        String group = section.getString("group", "");
        String permission = section.getString("permission");

        if (type == RecipeType.SHAPED) {
            List<String> shapeList = section.getStringList("shape");
            if (shapeList.isEmpty()) {
                return null;
            }
            String[] shape = shapeList.toArray(new String[0]);

            ConfigurationSection ingSec = section.getConfigurationSection("ingredients");
            Map<Character, RecipeChoice> ingredients = new HashMap<>();
            if (ingSec != null) {
                for (String charKey : ingSec.getKeys(false)) {
                    if (charKey.length() == 1) {
                        char c = charKey.charAt(0);
                        String itemDef = ingSec.getString(charKey);
                        RecipeChoice choice = parseChoice(itemDef);
                        if (choice != null) {
                            ingredients.put(c, choice);
                        }
                    }
                }
            }
            return new CustomRecipe(key, type, result, group, permission, shape, ingredients, null, null, 0, 0);
        } else if (type == RecipeType.SHAPELESS) {
            List<String> ingList = section.getStringList("ingredients");
            List<RecipeChoice> choices = new ArrayList<>();
            for (String itemDef : ingList) {
                RecipeChoice choice = parseChoice(itemDef);
                if (choice != null) {
                    choices.add(choice);
                }
            }
            if (choices.isEmpty()) {
                return null;
            }
            return new CustomRecipe(key, type, result, group, permission, null, null, choices, null, 0, 0);
        } else {
            
            String inputDef = section.getString("source", section.getString("ingredient"));
            RecipeChoice sourceChoice = parseChoice(inputDef);
            float exp = (float) section.getDouble("experience", 0.0);
            int cookingTime = section.getInt("cooking_time", 200);
            return new CustomRecipe(key, type, result, group, permission, null, null, null, sourceChoice, exp, cookingTime);
        }
    }

    @Nullable
    private ItemStack parseResult() {
        if (section.isConfigurationSection("result")) {
            ConfigurationSection resSec = section.getConfigurationSection("result");
            if (resSec != null) {
                String itemId = resSec.getString("item", id);
                int amount = resSec.getInt("amount", 1);
                return createItem(itemId, amount);
            }
        } else if (section.isString("result")) {
            return createItem(section.getString("result"), 1);
        }
        return createItem(id, 1);
    }

    @Nullable
    private ItemStack createItem(String itemId, int amount) {
        if (itemId == null) return null;
        if (itemId.startsWith("tessera:")) {
            itemId = itemId.substring("tessera:".length());
        }
        ItemStack custom = Tessera.getItem(itemId);
        if (custom != null) {
            custom.setAmount(amount);
            return custom;
        }
        Material mat = Material.matchMaterial(itemId.toUpperCase());
        if (mat != null) {
            return new ItemStack(mat, amount);
        }
        return null;
    }

    @Nullable
    private RecipeChoice parseChoice(@Nullable String itemDef) {
        if (itemDef == null || itemDef.isEmpty()) {
            return null;
        }
        if (itemDef.startsWith("tessera:")) {
            String customId = itemDef.substring("tessera:".length());
            ItemStack stack = Tessera.getItem(customId);
            if (stack != null) {
                return new RecipeChoice.ExactChoice(stack);
            }
        }
        
        if (Tessera.exists(itemDef)) {
            ItemStack stack = Tessera.getItem(itemDef);
            if (stack != null) {
                return new RecipeChoice.ExactChoice(stack);
            }
        }
        
        String matName = itemDef.startsWith("minecraft:") ? itemDef.substring("minecraft:".length()) : itemDef;
        Material mat = Material.matchMaterial(matName.toUpperCase());
        if (mat != null) {
            return new RecipeChoice.MaterialChoice(mat);
        }
        return null;
    }
}
