package com.prisma.tessera.gui;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GUIButton {

    private final ItemStack icon;
    private BiConsumer<Player, InventoryClickEvent> clickAction;

    public GUIButton(@NotNull ItemStack icon) {
        this.icon = icon.clone();
    }

    public static GUIButton of(@NotNull ItemStack icon) {
        return new GUIButton(icon);
    }

    public static GUIButton of(@NotNull ItemStack icon, @Nullable BiConsumer<Player, InventoryClickEvent> action) {
        GUIButton button = new GUIButton(icon);
        button.clickAction = action;
        return button;
    }

    public static GUIButton of(@NotNull ItemStack icon, @Nullable Consumer<Player> playerAction) {
        GUIButton button = new GUIButton(icon);
        if (playerAction != null) {
            button.clickAction = (player, event) -> playerAction.accept(player);
        }
        return button;
    }

    public GUIButton onClick(@Nullable BiConsumer<Player, InventoryClickEvent> action) {
        this.clickAction = action;
        return this;
    }

    public GUIButton onClick(@Nullable Consumer<Player> playerAction) {
        if (playerAction != null) {
            this.clickAction = (player, event) -> playerAction.accept(player);
        } else {
            this.clickAction = null;
        }
        return this;
    }

    @NotNull
    public ItemStack getIcon() {
        return icon.clone();
    }

    public void execute(@NotNull Player player, @NotNull InventoryClickEvent event) {
        if (clickAction != null) {
            clickAction.accept(player, event);
        }
    }
}
