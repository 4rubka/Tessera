package com.prisma.tessera.furniture;

import com.prisma.tessera.TesseraPlugin;
import com.prisma.tessera.items.ItemUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class FurniturePlaceListener implements Listener {

    private final TesseraPlugin plugin;
    private final FurnitureManager manager;

    public FurniturePlaceListener(TesseraPlugin plugin, FurnitureManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPlaceFurniture(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        String id = ItemUtils.getTesseraId(item);
        if (id == null || !manager.exists(id)) {
            return;
        }

        event.setCancelled(true);

        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }

        BlockFace face = event.getBlockFace();
        Block placeBlock = clicked.getRelative(face);

        Player player = event.getPlayer();
        float yaw = player.getLocation().getYaw() + 180.0f;

        Location spawnLoc = placeBlock.getLocation();
        Entity furniture = manager.spawnFurniture(spawnLoc, id, yaw, player);

        if (furniture != null && player.getGameMode() != GameMode.CREATIVE) {
            item.subtract(1);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Entity entity = event.getRightClicked();
        if (!manager.isFurniture(entity)) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            manager.sitPlayer(player, entity);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Entity entity = event.getRightClicked();
        if (!manager.isFurniture(entity)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFurnitureDamage(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (!manager.isFurniture(entity)) {
            return;
        }

        event.setCancelled(true);

        if (event.getDamager() instanceof Player player) {
            boolean drop = player.getGameMode() != GameMode.CREATIVE;
            manager.removeFurniture(entity, drop);
        }
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        Entity dismounted = event.getDismounted();
        if (manager.isSeat(dismounted)) {
            dismounted.remove();
        }
    }
}
