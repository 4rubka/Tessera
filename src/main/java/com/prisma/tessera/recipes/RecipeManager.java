package com.prisma.tessera.recipes;

import com.prisma.tessera.TesseraPlugin;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RecipeManager {

    private static RecipeManager instance;

    private final TesseraPlugin plugin;
    private final Map<NamespacedKey, CustomRecipe> recipes = new LinkedHashMap<>();
    private final Set<NamespacedKey> disabledVanilla = new HashSet<>();

    public RecipeManager(@NotNull TesseraPlugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static RecipeManager get() {
        return instance;
    }

    public void loadAll() {
        unregisterAll();

        File recipesDir = new File(plugin.getDataFolder(), "recipes");
        if (!recipesDir.exists()) {
            recipesDir.mkdirs();
        }

        for (YamlConfiguration config : plugin.getConfigManager().getConfigsInDirectory("recipes").values()) {
            
            List<String> disabledList = config.getStringList("disabled_vanilla");
            for (String keyStr : disabledList) {
                removeVanillaRecipe(NamespacedKey.fromString(keyStr));
            }

            for (String key : config.getKeys(false)) {
                if (key.equals("disabled_vanilla")) continue;
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section != null) {
                    CustomRecipe recipe = new RecipeParser(plugin, key, section).parse();
                    if (recipe != null) {
                        registerRecipe(recipe);
                    }
                }
            }
        }

        for (YamlConfiguration config : plugin.getConfigManager().getConfigsInDirectory("items").values()) {
            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section != null && section.isConfigurationSection("recipe")) {
                    ConfigurationSection recipeSec = section.getConfigurationSection("recipe");
                    if (recipeSec != null) {
                        CustomRecipe recipe = new RecipeParser(plugin, key + "_recipe", recipeSec).parse();
                        if (recipe != null) {
                            registerRecipe(recipe);
                        }
                    }
                }
            }
        }
    }

    public void registerRecipe(@NotNull CustomRecipe recipe) {
        recipes.put(recipe.getKey(), recipe);
        Recipe bukkitRecipe = recipe.toBukkitRecipe();
        if (bukkitRecipe != null) {
            try {
                Bukkit.addRecipe(bukkitRecipe);
            } catch (IllegalStateException e) {
                
                Bukkit.removeRecipe(recipe.getKey());
                Bukkit.addRecipe(bukkitRecipe);
            }
        }
    }

    public void unregisterRecipe(@NotNull NamespacedKey key) {
        recipes.remove(key);
        Bukkit.removeRecipe(key);
    }

    public void removeVanillaRecipe(@Nullable NamespacedKey key) {
        if (key != null) {
            disabledVanilla.add(key);
            Bukkit.removeRecipe(key);
        }
    }

    public boolean isVanillaDisabled(@NotNull NamespacedKey key) {
        return disabledVanilla.contains(key);
    }

    public void unregisterAll() {
        for (NamespacedKey key : recipes.keySet()) {
            Bukkit.removeRecipe(key);
        }
        recipes.clear();
        disabledVanilla.clear();
    }

    @Nullable
    public CustomRecipe getRecipe(@NotNull NamespacedKey key) {
        return recipes.get(key);
    }

    @NotNull
    public Collection<CustomRecipe> getAll() {
        return Collections.unmodifiableCollection(recipes.values());
    }

    @NotNull
    public Set<NamespacedKey> getKeys() {
        return Collections.unmodifiableSet(recipes.keySet());
    }

    @Nullable
    public CustomRecipe getRecipe(@NotNull String keyStr) {
        NamespacedKey key = NamespacedKey.fromString(keyStr, plugin);
        return key != null ? recipes.get(key) : null;
    }

    @NotNull
    public List<CustomRecipe> getRecipesForResult(@NotNull String itemId) {
        List<CustomRecipe> resultList = new java.util.ArrayList<>();
        for (CustomRecipe r : recipes.values()) {
            ItemStack stack = r.getResult();
            String id = com.prisma.tessera.items.ItemUtils.getTesseraId(stack);
            if (id != null && id.equalsIgnoreCase(itemId)) {
                resultList.add(r);
            } else if (id == null && stack.getType().name().equalsIgnoreCase(itemId)) {
                resultList.add(r);
            }
        }
        return resultList;
    }

    @NotNull
    public List<CustomRecipe> getRecipesFor(@NotNull ItemStack item) {
        String id = com.prisma.tessera.items.ItemUtils.getTesseraId(item);
        if (id != null) {
            return getRecipesForResult(id);
        }
        return getRecipesForResult(item.getType().name());
    }
}
