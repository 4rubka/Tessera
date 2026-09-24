package com.prisma.tessera.utils;

import com.prisma.tessera.TesseraPlugin;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public final class SeatManager implements Listener {

    public static final NamespacedKey SEAT_MARKER_KEY = new NamespacedKey("tessera", "is_seat");
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static SeatManager instance;

    private final TesseraPlugin plugin;
    private final Map<UUID, UUID> playerToSeatMap = new ConcurrentHashMap<>();

    public SeatManager(@NotNull TesseraPlugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static SeatManager get() {
        return instance;
    }

    public boolean sitOnBlock(@NotNull Player player, @NotNull Block block) {
        // Seat on top of the block. Blocks higher than a jump or with no room above are refused,
        // otherwise sitting and standing up moves the player over walls and through windows.
        double top = block.getBlockData() instanceof Stairs stairs && stairs.getHalf() == Bisected.Half.BOTTOM
                ? block.getY() + 0.5 : block.getBoundingBox().getMaxY();
        if (block.isPassable() || top - player.getLocation().getY() > 1.0
                || !block.getRelative(BlockFace.UP).isPassable() || !block.getRelative(BlockFace.UP, 2).isPassable()) {
            player.sendMessage(MM.deserialize("<red>You can't sit there.</red>"));
            return false;
        }
        Location seatLoc = block.getLocation().add(0.5, 0, 0.5);
        seatLoc.setY(top);
        return sitAt(player, seatLoc);
    }

    public boolean sitAt(@NotNull Player player, @NotNull Location location) {
        if (player.isInsideVehicle()) {
            player.sendMessage(MM.deserialize("<red>You are already sitting or riding something!</red>"));
            return false;
        }

        ArmorStand seat = location.getWorld().spawn(location, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setInvulnerable(true);
            as.setMarker(true);
            as.setSmall(true);
            as.setPersistent(false);
            as.getPersistentDataContainer().set(SEAT_MARKER_KEY, PersistentDataType.BYTE, (byte) 1);
        });

        seat.addPassenger(player);
        playerToSeatMap.put(player.getUniqueId(), seat.getUniqueId());
        player.playSound(player.getLocation(), "entity.item.pickup", 0.5f, 0.7f);
        player.sendActionBar(MM.deserialize("<gray>Press <yellow>Shift</yellow> to stand up.</gray>"));
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDismount(EntityDismountEvent event) {
        Entity dismounted = event.getDismounted();
        if (dismounted.getPersistentDataContainer().has(SEAT_MARKER_KEY, PersistentDataType.BYTE)) {
            dismounted.remove();
            if (event.getEntity() instanceof Player player) {
                playerToSeatMap.remove(player.getUniqueId());
                
                player.teleport(player.getLocation().add(0, 0.5, 0));
            }
        }
    }

    public int cleanupAllOrphans() {
        int count = 0;
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                var pdc = entity.getPersistentDataContainer();
                boolean seat = pdc.has(SEAT_MARKER_KEY, PersistentDataType.BYTE)
                        || pdc.has(com.prisma.tessera.furniture.FurnitureManager.FURNITURE_SEAT_KEY, PersistentDataType.BYTE);
                if (seat) {
                    if (entity.getPassengers().isEmpty()) {
                        entity.remove();
                        count++;
                    }
                }
            }
        }
        playerToSeatMap.clear();
        return count;
    }

    public void cleanUp() {
        for (UUID seatId : playerToSeatMap.values()) {
            Entity entity = plugin.getServer().getEntity(seatId);
            if (entity != null) {
                entity.remove();
            }
        }
        playerToSeatMap.clear();
    }
}
