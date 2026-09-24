package com.prisma.tessera.blocks;

import com.prisma.tessera.TesseraPlugin;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BlockRegistry {

    public static final NamespacedKey BLOCK_ID_KEY = new NamespacedKey("tessera", "block_id");

    private static BlockRegistry instance;

    private final TesseraPlugin plugin;
    private final Map<String, BlockTemplate> blocks = new LinkedHashMap<>();

    public BlockRegistry(@NotNull TesseraPlugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static BlockRegistry get() {
        return instance;
    }

    public void loadAll() {
        blocks.clear();
        for (YamlConfiguration config : plugin.getConfigManager().getConfigsInDirectory("blocks").values()) {
            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section != null) {
                    register(key, new BlockParser(section).parse());
                }
            }
        }
    }

    public void register(@NotNull String id, @NotNull BlockTemplate template) {
        blocks.put(id, template);
    }

    public void unregister(@NotNull String id) {
        blocks.remove(id);
    }

    @Nullable
    public BlockTemplate getTemplate(@NotNull String id) {
        return blocks.get(id);
    }

    public boolean exists(@NotNull String id) {
        return blocks.containsKey(id);
    }

    @NotNull
    public Collection<BlockTemplate> getAll() {
        return Collections.unmodifiableCollection(blocks.values());
    }

    @NotNull
    public Set<String> getIds() {
        return Collections.unmodifiableSet(blocks.keySet());
    }

    private NamespacedKey getChunkKey(int x, int y, int z) {
        int relX = (x % 16 + 16) % 16;
        int relZ = (z % 16 + 16) % 16;
        return new NamespacedKey(plugin, "b_" + relX + "_" + y + "_" + relZ);
    }

    public boolean isCustomBlock(@NotNull Block block) {
        return getBlockId(block) != null;
    }

    @Nullable
    public String getBlockId(@NotNull Block block) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer chunkPdc = chunk.getPersistentDataContainer();
        NamespacedKey key = getChunkKey(block.getX(), block.getY(), block.getZ());
        String id = chunkPdc.get(key, PersistentDataType.STRING);
        if (id != null) {
            // WorldEdit, /setblock or fire replace the block without a break event, which leaves the entry behind.
            BlockTemplate template = blocks.get(id);
            if (template != null && block.getType() != template.getMaterial()) {
                chunkPdc.remove(key);
                return null;
            }
            return id;
        }

        if (block.getState() instanceof TileState tileState) {
            return tileState.getPersistentDataContainer().get(BLOCK_ID_KEY, PersistentDataType.STRING);
        }
        return null;
    }

    public void placeCustomBlock(@NotNull Location loc, @NotNull String id) {
        BlockTemplate template = getTemplate(id);
        Block block = loc.getBlock();
        Material material = template != null ? template.getMaterial() : Material.NOTE_BLOCK;
        block.setType(material, false);

        Chunk chunk = block.getChunk();
        PersistentDataContainer chunkPdc = chunk.getPersistentDataContainer();
        NamespacedKey key = getChunkKey(block.getX(), block.getY(), block.getZ());
        chunkPdc.set(key, PersistentDataType.STRING, id);

        if (block.getState() instanceof TileState tileState) {
            tileState.getPersistentDataContainer().set(BLOCK_ID_KEY, PersistentDataType.STRING, id);
            tileState.update(true, false);
        }
    }

    public void removeCustomBlock(@NotNull Location loc) {
        Block block = loc.getBlock();
        Chunk chunk = block.getChunk();
        PersistentDataContainer chunkPdc = chunk.getPersistentDataContainer();
        NamespacedKey key = getChunkKey(block.getX(), block.getY(), block.getZ());
        chunkPdc.remove(key);

        if (block.getState() instanceof TileState tileState) {
            tileState.getPersistentDataContainer().remove(BLOCK_ID_KEY);
            tileState.update(true, false);
        }
        block.setType(Material.AIR);
    }
}
