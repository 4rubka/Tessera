package com.prisma.tessera.blocks;

import com.prisma.tessera.TesseraPlugin;
import com.prisma.tessera.items.ItemUtils;
import java.util.Iterator;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class BlockListener implements Listener {

    private final TesseraPlugin plugin;
    private final BlockRegistry registry;

    public BlockListener(TesseraPlugin plugin, BlockRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        String id = ItemUtils.getTesseraId(item);
        if (id == null) {
            return;
        }

        BlockTemplate template = registry.getTemplate(id);
        if (template == null) {
            return;
        }

        Block block = event.getBlockPlaced();
        registry.placeCustomBlock(block.getLocation(), id);

        if (template.getPlaceSound() != null) {
            playSound(block.getLocation(), template.getPlaceSound());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String id = registry.getBlockId(block);
        if (id == null) {
            return;
        }

        BlockTemplate template = registry.getTemplate(id);
        event.setDropItems(false);
        event.setExpToDrop(0);

        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE && template != null) {
            ItemStack tool = player.getInventory().getItemInMainHand();
            List<ItemStack> drops = template.computeDrops(tool);
            for (ItemStack drop : drops) {
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), drop);
            }
        }

        if (template != null && template.getBreakSound() != null) {
            playSound(block.getLocation(), template.getBreakSound());
        }

        registry.removeCustomBlock(block.getLocation());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        String id = registry.getBlockId(block);
        if (id == null) {
            return;
        }

        if (block.getType().name().contains("NOTE_BLOCK")) {
            event.setCancelled(true);
        }

        BlockTemplate template = registry.getTemplate(id);
        if (template != null && template.getHitSound() != null) {
            playSound(block.getLocation(), template.getHitSound());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onNotePlay(NotePlayEvent event) {
        if (registry.isCustomBlock(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (registry.isCustomBlock(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (registry.isCustomBlock(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    private void handleExplosion(List<Block> blockList) {
        Iterator<Block> iterator = blockList.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            String id = registry.getBlockId(block);
            if (id != null) {
                iterator.remove();
                BlockTemplate template = registry.getTemplate(id);
                if (template != null) {
                    List<ItemStack> drops = template.computeDrops(null);
                    for (ItemStack drop : drops) {
                        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), drop);
                    }
                }
                registry.removeCustomBlock(block.getLocation());
            }
        }
    }

    private void playSound(Location loc, String soundName) {
        if (loc == null || loc.getWorld() == null || soundName == null || soundName.isBlank()) return;
        try {
            loc.getWorld().playSound(loc, com.prisma.tessera.utils.SoundKeys.resolve(soundName), 1.0f, 1.0f);
        } catch (Exception ignored) {
        }
    }
}
