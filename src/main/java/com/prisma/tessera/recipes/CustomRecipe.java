package com.prisma.tessera.recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CustomRecipe {

    private final NamespacedKey key;
    private final RecipeType type;
    private final ItemStack result;
    private final String group;
    private final String permission;

    private final String[] shape;
    private final Map<Character, RecipeChoice> shapedIngredients;

    private final List<RecipeChoice> shapelessIngredients;

    private final RecipeChoice sourceChoice;
    private final float experience;
    private final int cookingTime;

    public CustomRecipe(
            @NotNull NamespacedKey key,
            @NotNull RecipeType type,
            @NotNull ItemStack result,
            @Nullable String group,
            @Nullable String permission,
            @Nullable String[] shape,
            @Nullable Map<Character, RecipeChoice> shapedIngredients,
            @Nullable List<RecipeChoice> shapelessIngredients,
            @Nullable RecipeChoice sourceChoice,
            float experience,
            int cookingTime
    ) {
        this.key = key;
        this.type = type;
        this.result = result.clone();
        this.group = group != null ? group : "";
        this.permission = permission;
        this.shape = shape;
        this.shapedIngredients = shapedIngredients != null ? new HashMap<>(shapedIngredients) : new HashMap<>();
        this.shapelessIngredients = shapelessIngredients != null ? new ArrayList<>(shapelessIngredients) : new ArrayList<>();
        this.sourceChoice = sourceChoice;
        this.experience = experience;
        this.cookingTime = cookingTime;
    }

    public NamespacedKey getKey() {
        return key;
    }

    public RecipeType getType() {
        return type;
    }

    public ItemStack getResult() {
        return result.clone();
    }

    public String getGroup() {
        return group;
    }

    @Nullable
    public String getPermission() {
        return permission;
    }

    @Nullable
    public String[] getShape() {
        return shape != null ? shape.clone() : null;
    }

    public Map<Character, RecipeChoice> getShapedIngredients() {
        return Collections.unmodifiableMap(shapedIngredients);
    }

    public List<RecipeChoice> getShapelessIngredients() {
        return Collections.unmodifiableList(shapelessIngredients);
    }

    @Nullable
    public RecipeChoice getSourceChoice() {
        return sourceChoice;
    }

    public float getExperience() {
        return experience;
    }

    public int getCookingTime() {
        return cookingTime;
    }

    @Nullable
    public Recipe toBukkitRecipe() {
        switch (type) {
            case SHAPED -> {
                if (shape == null || shape.length == 0) {
                    return null;
                }
                ShapedRecipe recipe = new ShapedRecipe(key, result);
                recipe.shape(shape);
                for (Map.Entry<Character, RecipeChoice> entry : shapedIngredients.entrySet()) {
                    recipe.setIngredient(entry.getKey(), entry.getValue());
                }
                if (!group.isEmpty()) {
                    recipe.setGroup(group);
                }
                return recipe;
            }
            case SHAPELESS -> {
                ShapelessRecipe recipe = new ShapelessRecipe(key, result);
                for (RecipeChoice choice : shapelessIngredients) {
                    recipe.addIngredient(choice);
                }
                if (!group.isEmpty()) {
                    recipe.setGroup(group);
                }
                return recipe;
            }
            case FURNACE -> {
                if (sourceChoice == null) return null;
                FurnaceRecipe recipe = new FurnaceRecipe(key, result, sourceChoice, experience, cookingTime > 0 ? cookingTime : 200);
                if (!group.isEmpty()) recipe.setGroup(group);
                return recipe;
            }
            case BLASTING -> {
                if (sourceChoice == null) return null;
                BlastingRecipe recipe = new BlastingRecipe(key, result, sourceChoice, experience, cookingTime > 0 ? cookingTime : 100);
                if (!group.isEmpty()) recipe.setGroup(group);
                return recipe;
            }
            case SMOKING -> {
                if (sourceChoice == null) return null;
                SmokingRecipe recipe = new SmokingRecipe(key, result, sourceChoice, experience, cookingTime > 0 ? cookingTime : 100);
                if (!group.isEmpty()) recipe.setGroup(group);
                return recipe;
            }
            case CAMPFIRE -> {
                if (sourceChoice == null) return null;
                CampfireRecipe recipe = new CampfireRecipe(key, result, sourceChoice, experience, cookingTime > 0 ? cookingTime : 600);
                if (!group.isEmpty()) recipe.setGroup(group);
                return recipe;
            }
            case STONECUTTING -> {
                if (sourceChoice == null) return null;
                StonecuttingRecipe recipe = new StonecuttingRecipe(key, result, sourceChoice);
                if (!group.isEmpty()) recipe.setGroup(group);
                return recipe;
            }
            default -> {
                return null;
            }
        }
    }
}
