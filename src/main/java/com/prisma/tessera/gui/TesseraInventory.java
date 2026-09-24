package com.prisma.tessera.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class TesseraInventory implements InventoryHolder {

    private final TesseraGUI gui;
    private Inventory inventory;

    public TesseraInventory(@NotNull TesseraGUI gui) {
        this.gui = gui;
    }

    public void setInventory(@NotNull Inventory inventory) {
        this.inventory = inventory;
    }

    @NotNull
    public TesseraGUI getGui() {
        return gui;
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }
}
