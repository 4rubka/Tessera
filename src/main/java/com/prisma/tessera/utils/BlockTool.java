package com.prisma.tessera.utils;

import com.prisma.tessera.TesseraPlugin;
import com.prisma.tessera.blocks.BlockRegistry;
import com.prisma.tessera.blocks.BlockTemplate;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class BlockTool {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void inspectTarget(@NotNull Player player) {
        Block block = player.getTargetBlockExact(6, FluidCollisionMode.NEVER);
        if (block == null || block.getType().isAir()) {
            player.sendMessage(MM.deserialize("<red>No block targeted in your crosshairs (within 6 blocks).</red>"));
            return;
        }

        BlockRegistry registry = TesseraPlugin.get().getBlockRegistry();
        String customId = registry.getBlockId(block);

        player.sendMessage(MM.deserialize("<gradient:#00c6ff:#0072ff><bold>════════ Block Inspector ════════</bold></gradient>"));
        player.sendMessage(MM.deserialize("<gray>Location: <white>" + block.getWorld().getName() + " [" +
                block.getX() + ", " + block.getY() + ", " + block.getZ() + "]</white></gray>"));
        player.sendMessage(MM.deserialize("<gray>Vanilla Material: <yellow>" + block.getType().name() + "</yellow></gray>"));

        if (customId != null) {
            BlockTemplate tpl = registry.getTemplate(customId);
            player.sendMessage(MM.deserialize("<gray>Tessera Block ID: <aqua><bold>" + customId + "</bold></aqua></gray>"));
            if (tpl != null) {
                player.sendMessage(MM.deserialize("<gray>Hardness: <gold>" + tpl.getHardness() + "</gold>  <dark_gray>│</dark_gray>  Tool: <gold>" + tpl.getRequiredTool() + "</gold> (" + tpl.getMinimumTier() + ")</gray>"));
                if (!tpl.getDrops().isEmpty()) {
                    var drop = tpl.getDrops().get(0);
                    player.sendMessage(MM.deserialize("<gray>Drop Item: <yellow>" + drop.itemId() + "</yellow> <dark_gray>(x" + drop.minAmount() + (drop.maxAmount() > drop.minAmount() ? "-" + drop.maxAmount() : "") + ")</dark_gray></gray>"));
                }
            }
        } else {
            player.sendMessage(MM.deserialize("<gray>Tessera Status: <dark_gray>Vanilla Block</dark_gray></gray>"));
            player.sendMessage(MM.deserialize("<gray>Biome: <white>" + block.getBiome().key().asString() + "</white>  <dark_gray>│</dark_gray>  Light: <yellow>" + block.getLightLevel() + "</yellow></gray>"));
        }
        player.sendMessage(MM.deserialize("<gradient:#00c6ff:#0072ff><bold>═════════════════════════════════</bold></gradient>"));
    }

    public static void removeTarget(@NotNull Player player) {
        Block block = player.getTargetBlockExact(6, FluidCollisionMode.NEVER);
        if (block == null || block.getType().isAir()) {
            player.sendMessage(MM.deserialize("<red>No block targeted in your crosshairs.</red>"));
            return;
        }

        BlockRegistry registry = TesseraPlugin.get().getBlockRegistry();
        String customId = registry.getBlockId(block);

        if (customId == null) {
            player.sendMessage(MM.deserialize("<red>Targeted block is not a registered Tessera custom block.</red>"));
            return;
        }

        registry.removeCustomBlock(block.getLocation());
        player.playSound(player.getLocation(), "block.stone.break", 1.0f, 1.0f);
        player.sendMessage(MM.deserialize("<green>Successfully removed custom block <yellow>" + customId + "</yellow> and cleared PDC.</green>"));
    }
}
