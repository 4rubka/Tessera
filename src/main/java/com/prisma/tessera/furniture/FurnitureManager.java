package com.prisma.tessera.furniture;

import com.prisma.tessera.TesseraPlugin;
import com.prisma.tessera.api.Tessera;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class FurnitureManager {

    public static final NamespacedKey FURNITURE_ID_KEY = new NamespacedKey("tessera", "furniture_id");
    public static final NamespacedKey FURNITURE_ROOT_KEY = new NamespacedKey("tessera", "furniture_root");
    public static final NamespacedKey FURNITURE_SEAT_KEY = new NamespacedKey("tessera", "furniture_seat");

    private static FurnitureManager instance;

    private final TesseraPlugin plugin;
    private final Map<String, FurnitureType> furnitureTypes = new LinkedHashMap<>();

    public FurnitureManager(@NotNull TesseraPlugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static FurnitureManager get() {
        return instance;
    }

    public void loadAll() {
        furnitureTypes.clear();
        for (YamlConfiguration config : plugin.getConfigManager().getConfigsInDirectory("furniture").values()) {
            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section != null) {
                    register(key, new FurnitureParser(section).parse());
                }
            }
        }
    }

    public void register(@NotNull String id, @NotNull FurnitureType type) {
        furnitureTypes.put(id, type);
    }

    public void unregister(@NotNull String id) {
        furnitureTypes.remove(id);
    }

    @Nullable
    public FurnitureType getType(@NotNull String id) {
        return furnitureTypes.get(id);
    }

    public boolean exists(@NotNull String id) {
        return furnitureTypes.containsKey(id);
    }

    @NotNull
    public Collection<FurnitureType> getAll() {
        return Collections.unmodifiableCollection(furnitureTypes.values());
    }

    @NotNull
    public Set<String> getIds() {
        return Collections.unmodifiableSet(furnitureTypes.keySet());
    }

    public boolean isFurniture(@NotNull Entity entity) {
        return entity.getPersistentDataContainer().has(FURNITURE_ID_KEY, PersistentDataType.STRING);
    }

    @Nullable
    public String getFurnitureId(@NotNull Entity entity) {
        return entity.getPersistentDataContainer().get(FURNITURE_ID_KEY, PersistentDataType.STRING);
    }

    @Nullable
    public String getFurnitureRootId(@NotNull Entity entity) {
        return entity.getPersistentDataContainer().get(FURNITURE_ROOT_KEY, PersistentDataType.STRING);
    }

    public boolean isSeat(@NotNull Entity entity) {
        return entity.getPersistentDataContainer().has(FURNITURE_SEAT_KEY, PersistentDataType.BYTE);
    }

    @Nullable
    public Entity spawnFurniture(@NotNull Location location, @NotNull String id, float yaw, @Nullable Player player) {
        FurnitureType type = getType(id);
        if (type == null) {
            return null;
        }

        Location centerLoc = location.clone();
        centerLoc.setX(Math.floor(centerLoc.getX()) + 0.5);
        centerLoc.setZ(Math.floor(centerLoc.getZ()) + 0.5);

        float finalYaw = yaw;
        if (type.isCardinalRotation()) {
            finalYaw = Math.round(yaw / 90.0f) * 90.0f;
        }
        centerLoc.setYaw(finalYaw);
        centerLoc.setPitch(0.0f);

        ItemStack itemStack = Tessera.getItem(type.getItemId());
        if (itemStack == null) {
            itemStack = new ItemStack(org.bukkit.Material.OAK_PLANKS);
        }

        Entity baseEntity;
        if (type.getDisplayType() == FurnitureDisplayType.ARMOR_STAND) {
            ItemStack helmetItem = itemStack.clone();
            baseEntity = centerLoc.getWorld().spawn(centerLoc, ArmorStand.class, stand -> {
                stand.setVisible(false);
                stand.setGravity(false);
                stand.setMarker(true);
                stand.setCustomNameVisible(false);
                stand.getEquipment().setHelmet(helmetItem);
                stand.setCanMove(false);
            });
        } else {
            ItemStack displayItem = itemStack.clone();
            baseEntity = centerLoc.getWorld().spawn(centerLoc, ItemDisplay.class, display -> {
                display.setItemStack(displayItem);
                display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                Vector3f translation = new Vector3f(type.getTranslationX(), type.getTranslationY(), type.getTranslationZ());
                Vector3f scale = new Vector3f(type.getScaleX(), type.getScaleY(), type.getScaleZ());
                Transformation transformation = new Transformation(translation, new AxisAngle4f(), scale, new AxisAngle4f());
                display.setTransformation(transformation);
            });
        }

        UUID rootUuid = baseEntity.getUniqueId();
        PersistentDataContainer rootPdc = baseEntity.getPersistentDataContainer();
        rootPdc.set(FURNITURE_ID_KEY, PersistentDataType.STRING, id);
        rootPdc.set(FURNITURE_ROOT_KEY, PersistentDataType.STRING, rootUuid.toString());

        centerLoc.getWorld().spawn(centerLoc, Interaction.class, interaction -> {
            interaction.setInteractionWidth(type.getHitboxWidth());
            interaction.setInteractionHeight(type.getHitboxHeight());
            interaction.setResponsive(true);
            PersistentDataContainer pdc = interaction.getPersistentDataContainer();
            pdc.set(FURNITURE_ID_KEY, PersistentDataType.STRING, id);
            pdc.set(FURNITURE_ROOT_KEY, PersistentDataType.STRING, rootUuid.toString());
        });

        if (type.getPlaceSound() != null) {
            playSound(centerLoc, type.getPlaceSound());
        }

        return baseEntity;
    }

    public void removeFurniture(@NotNull Entity entity, boolean dropItem) {
        // Two hits in one tick on different parts would otherwise drop the item twice.
        if (!entity.isValid()) {
            return;
        }
        String rootUuidStr = getFurnitureRootId(entity);
        String fid = getFurnitureId(entity);
        if (rootUuidStr == null || fid == null) {
            return;
        }

        FurnitureType type = getType(fid);
        Location loc = entity.getLocation();

        double searchRadius = type != null ? Math.max(type.getHitboxWidth(), type.getHitboxHeight()) + 2.0 : 3.0;
        for (Entity nearby : loc.getWorld().getNearbyEntities(loc, searchRadius, searchRadius, searchRadius)) {
            String nearbyRoot = getFurnitureRootId(nearby);
            // Seats carry their furniture's root id, so only this piece's own seat is removed.
            if (rootUuidStr.equals(nearbyRoot)) {
                nearby.eject();
                nearby.remove();
            }
        }
        entity.remove();

        if (dropItem && type != null && !type.isCancelDrop()) {
            ItemStack drop = Tessera.getItem(type.getDropItemId());
            if (drop != null) {
                loc.getWorld().dropItemNaturally(loc.clone().add(0, 0.5, 0), drop);
            }
        }

        if (type != null && type.getBreakSound() != null) {
            playSound(loc, type.getBreakSound());
        }
    }

    public boolean sitPlayer(@NotNull Player player, @NotNull Entity furnitureEntity) {
        String fid = getFurnitureId(furnitureEntity);
        if (fid == null) {
            return false;
        }
        FurnitureType type = getType(fid);
        if (type == null || !type.isSeat()) {
            return false;
        }

        String rootId = getFurnitureRootId(furnitureEntity);
        Location seatLoc = furnitureEntity.getLocation().clone().add(0, type.getSeatHeight(), 0);
        if (rootId != null) {
            for (Entity nearby : seatLoc.getWorld().getNearbyEntities(seatLoc, 1.5, 1.5, 1.5)) {
                if (isSeat(nearby) && rootId.equals(getFurnitureRootId(nearby)) && !nearby.getPassengers().isEmpty()) {
                    return false;
                }
            }
        }
        ArmorStand seat = seatLoc.getWorld().spawn(seatLoc, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.setSmall(true);
            // Not saved with the chunk, so a crash while someone sits does not leave a stand behind.
            stand.setPersistent(false);
            stand.getPersistentDataContainer().set(FURNITURE_SEAT_KEY, PersistentDataType.BYTE, (byte) 1);
            if (rootId != null) {
                stand.getPersistentDataContainer().set(FURNITURE_ROOT_KEY, PersistentDataType.STRING, rootId);
            }
        });

        return seat.addPassenger(player);
    }

    private void playSound(Location loc, String soundName) {
        if (loc == null || loc.getWorld() == null || soundName == null || soundName.isBlank()) return;
        try {
            loc.getWorld().playSound(loc, com.prisma.tessera.utils.SoundKeys.resolve(soundName), 1.0f, 1.0f);
        } catch (Exception ignored) {
        }
    }
}
