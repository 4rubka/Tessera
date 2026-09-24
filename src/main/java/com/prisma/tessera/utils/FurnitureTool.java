package com.prisma.tessera.utils;

import com.prisma.tessera.TesseraPlugin;
import com.prisma.tessera.furniture.FurnitureManager;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public final class FurnitureTool {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void listNearby(@NotNull Player player, double radius) {
        FurnitureManager fm = TesseraPlugin.get().getFurnitureManager();
        Location pLoc = player.getLocation();
        double r = Math.max(1.0, Math.min(50.0, radius));

        List<Entity> nearbyFurniture = new ArrayList<>();
        for (Entity e : player.getWorld().getNearbyEntities(pLoc, r, r, r)) {
            if (fm.isFurniture(e) && fm.getFurnitureRootId(e) != null && fm.getFurnitureRootId(e).equals(e.getUniqueId().toString())) {
                nearbyFurniture.add(e);
            }
        }

        if (nearbyFurniture.isEmpty()) {
            player.sendMessage(MM.deserialize("<yellow>No Tessera furniture found within a " + (int) r + " block radius.</yellow>"));
            return;
        }

        player.sendMessage(MM.deserialize("<gradient:#00c6ff:#0072ff><bold>════ Nearby Furniture (" + nearbyFurniture.size() + ") ════</bold></gradient>"));
        for (Entity f : nearbyFurniture) {
            String fid = fm.getFurnitureId(f);
            double dist = Math.round(f.getLocation().distance(pLoc) * 10.0) / 10.0;
            Location loc = f.getLocation();

            player.sendMessage(MM.deserialize("<dark_gray> » </dark_gray><aqua><bold>" + fid + "</bold></aqua> <gray>at [" +
                    loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "] (" + dist + "m)</gray>"));

            f.setGlowing(true);
            f.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, loc.clone().add(0, 0.8, 0), 10, 0.2, 0.2, 0.2, 0.05);

            TesseraPlugin.get().getServer().getScheduler().runTaskLater(TesseraPlugin.get(), () -> {
                if (f.isValid()) {
                    f.setGlowing(false);
                }
            }, 200L);
        }
        player.sendMessage(MM.deserialize("<gray>Highlighted furniture with glowing outline for 10 seconds.</gray>"));
        player.sendMessage(MM.deserialize("<gradient:#00c6ff:#0072ff><bold>═══════════════════════════════</bold></gradient>"));
    }

    public static void removeNearby(@NotNull Player player, double radius) {
        FurnitureManager fm = TesseraPlugin.get().getFurnitureManager();
        Location pLoc = player.getLocation();
        double r = Math.max(1.0, Math.min(25.0, radius));

        int removed = 0;
        for (Entity e : player.getWorld().getNearbyEntities(pLoc, r, r, r)) {
            if (fm.isFurniture(e)) {
                fm.removeFurniture(e, false);
                removed++;
            }
        }

        if (removed > 0) {
            player.playSound(player.getLocation(), "entity.item.break", 1.0f, 1.0f);
            player.sendMessage(MM.deserialize("<green>Successfully removed <yellow>" + removed + "</yellow> furniture entities within " + (int) r + " blocks.</green>"));
        } else {
            player.sendMessage(MM.deserialize("<yellow>No furniture found within a " + (int) r + " block radius to remove.</yellow>"));
        }
    }
}
