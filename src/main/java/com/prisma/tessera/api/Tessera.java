package com.prisma.tessera.api;

import com.prisma.tessera.TesseraPlugin;
import com.prisma.tessera.blocks.BlockTemplate;
import com.prisma.tessera.furniture.FurnitureType;
import com.prisma.tessera.gui.ItemBrowserGUI;
import com.prisma.tessera.items.ItemTemplate;
import com.prisma.tessera.items.ItemUtils;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Tessera {

    private Tessera() {
    }

    public static boolean isLoaded() {
        return TesseraPlugin.get() != null && TesseraPlugin.get().isEnabled();
    }

    @Nullable
    public static ItemStack getItem(@NotNull String id) {
        return TesseraPlugin.get().getItemRegistry().getItem(id);
    }

    @Nullable
    public static ItemTemplate getTemplate(@NotNull String id) {
        return TesseraPlugin.get().getItemRegistry().getTemplate(id);
    }

    public static boolean exists(@NotNull String id) {
        return TesseraPlugin.get().getItemRegistry().exists(id);
    }

    @NotNull
    public static Set<String> getAllIds() {
        return TesseraPlugin.get().getItemRegistry().getIds();
    }

    public static boolean isCustomItem(@NotNull ItemStack itemStack) {
        return ItemUtils.isTesseraItem(itemStack);
    }

    @Nullable
    public static String getCustomItemId(@NotNull ItemStack itemStack) {
        return ItemUtils.getTesseraId(itemStack);
    }

    public static boolean isCustomBlock(@NotNull Block block) {
        return TesseraPlugin.get().getBlockRegistry().isCustomBlock(block);
    }

    @Nullable
    public static String getCustomBlockId(@NotNull Block block) {
        return TesseraPlugin.get().getBlockRegistry().getBlockId(block);
    }

    @Nullable
    public static BlockTemplate getBlockTemplate(@NotNull String id) {
        return TesseraPlugin.get().getBlockRegistry().getTemplate(id);
    }

    public static void placeBlock(@NotNull Location loc, @NotNull String id) {
        TesseraPlugin.get().getBlockRegistry().placeCustomBlock(loc, id);
    }

    public static void removeBlock(@NotNull Location loc) {
        TesseraPlugin.get().getBlockRegistry().removeCustomBlock(loc);
    }

    public static boolean isFurniture(@NotNull Entity entity) {
        return TesseraPlugin.get().getFurnitureManager().isFurniture(entity);
    }

    @Nullable
    public static String getFurnitureId(@NotNull Entity entity) {
        return TesseraPlugin.get().getFurnitureManager().getFurnitureId(entity);
    }

    @Nullable
    public static FurnitureType getFurnitureType(@NotNull String id) {
        return TesseraPlugin.get().getFurnitureManager().getType(id);
    }

    @Nullable
    public static Entity spawnFurniture(@NotNull Location loc, @NotNull String id, float yaw) {
        return TesseraPlugin.get().getFurnitureManager().spawnFurniture(loc, id, yaw, null);
    }

    public static void openMenu(@NotNull Player player) {
        ItemBrowserGUI.open(player);
    }

    @NotNull
    public static String replaceGlyphs(@NotNull String input) {
        return TesseraPlugin.get().getFontManager().replaceGlyphs(input);
    }

    @NotNull
    public static Component format(@NotNull String miniMessage) {
        return TesseraPlugin.get().getFontManager().format(miniMessage);
    }

    public static boolean toggleHud(@NotNull Player player) {
        return TesseraPlugin.get().getHudManager().toggleHud(player);
    }
}
