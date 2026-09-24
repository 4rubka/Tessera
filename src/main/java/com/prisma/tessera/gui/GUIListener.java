package com.prisma.tessera.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class GUIListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TesseraInventory tesseraInv)) {
            return;
        }

        TesseraGUI gui = tesseraInv.getGui();
        if (gui.isCancelClicks()) {
            event.setCancelled(true);
        }

        if (event.getRawSlot() >= 0 && event.getRawSlot() < gui.getSize()) {
            GUIButton button = gui.getButton(event.getRawSlot());
            if (button != null && event.getWhoClicked() instanceof Player player) {
                button.execute(player, event);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof TesseraInventory tesseraInv)) {
            return;
        }

        TesseraGUI gui = tesseraInv.getGui();
        for (int slot : event.getRawSlots()) {
            if (slot < gui.getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof TesseraInventory tesseraInv) {
            if (event.getPlayer() instanceof Player player) {
                tesseraInv.getGui().handleClose(player);
            }
        }
    }
}
