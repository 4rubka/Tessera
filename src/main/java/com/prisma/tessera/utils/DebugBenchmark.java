package com.prisma.tessera.utils;

import com.prisma.tessera.TesseraPlugin;
import com.prisma.tessera.blocks.BlockRegistry;
import com.prisma.tessera.furniture.FurnitureManager;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public final class DebugBenchmark {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void runBenchmark(@NotNull CommandSender sender) {
        sender.sendMessage(MM.deserialize("<gradient:#00c6ff:#0072ff><bold>Starting Tessera PDC Benchmark...</bold></gradient>"));

        ItemStack testItem = new ItemStack(Material.STICK);
        ItemMeta meta = testItem.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey testKey = new NamespacedKey("tessera_benchmark", "test_key");

        final int iterations = 10_000;

        long startWrite = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            pdc.set(testKey, PersistentDataType.STRING, "tessera_val_" + i);
        }
        long endWrite = System.nanoTime();
        double writeMicros = (endWrite - startWrite) / 1_000.0;
        double writeAvg = writeMicros / iterations;

        long startRead = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            String val = pdc.get(testKey, PersistentDataType.STRING);
        }
        long endRead = System.nanoTime();
        double readMicros = (endRead - startRead) / 1_000.0;
        double readAvg = readMicros / iterations;

        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory() / (1024 * 1024);

        double[] tps = Bukkit.getTPS();
        double mspt = Bukkit.getAverageTickTime();

        sender.sendMessage(MM.deserialize("<gradient:#00c6ff:#0072ff><bold>════════ Benchmark Results (" + iterations + " ops) ════════</bold></gradient>"));
        sender.sendMessage(MM.deserialize("<gray>PDC Write: <yellow>" + String.format("%.2f", writeMicros / 1000.0) + "ms</yellow> <dark_gray>(avg: " + String.format("%.3f", writeAvg) + "µs/op)</dark_gray></gray>"));
        sender.sendMessage(MM.deserialize("<gray>PDC Read:  <green>" + String.format("%.2f", readMicros / 1000.0) + "ms</green> <dark_gray>(avg: " + String.format("%.3f", readAvg) + "µs/op)</dark_gray></gray>"));
        sender.sendMessage(MM.deserialize("<gradient:#00c6ff:#0072ff>── Server Performance ──</gradient>"));
        sender.sendMessage(MM.deserialize("<gray>TPS (1m, 5m, 15m): <green>" + String.format("%.2f", tps[0]) + "</green>, <green>" + String.format("%.2f", tps[1]) + "</green>, <green>" + String.format("%.2f", tps[2]) + "</green></gray>"));
        sender.sendMessage(MM.deserialize("<gray>MSPT: <aqua>" + String.format("%.2f", mspt) + "ms</aqua></gray>"));
        sender.sendMessage(MM.deserialize("<gray>Heap Memory: <yellow>" + usedMemory + "MB</yellow> used / <gray>" + maxMemory + "MB</gray> max <dark_gray>(" + freeMemory + "MB free)</dark_gray></gray>"));
        sender.sendMessage(MM.deserialize("<gradient:#00c6ff:#0072ff><bold>════════════════════════════════════════════</bold></gradient>"));
    }

    public static void reportEntities(@NotNull CommandSender sender) {
        FurnitureManager fm = TesseraPlugin.get().getFurnitureManager();
        int totalDisplays = 0;
        int totalInteractions = 0;
        int totalSeats = 0;

        for (World world : Bukkit.getWorlds()) {
            int wDisplays = 0;
            int wInteractions = 0;
            for (Entity entity : world.getEntities()) {
                if (fm.isFurniture(entity)) {
                    if (entity instanceof ItemDisplay) wDisplays++;
                    else if (entity instanceof org.bukkit.entity.Interaction) wInteractions++;
                }
                if (entity.getPersistentDataContainer().has(SeatManager.SEAT_MARKER_KEY, PersistentDataType.BYTE)) {
                    totalSeats++;
                }
            }
            totalDisplays += wDisplays;
            totalInteractions += wInteractions;
        }

        sender.sendMessage(MM.deserialize("<gradient:#00c6ff:#0072ff><bold>════ Tessera Live Entity Monitor ════</bold></gradient>"));
        sender.sendMessage(MM.deserialize("<gray>Furniture Displays: <yellow>" + totalDisplays + "</yellow></gray>"));
        sender.sendMessage(MM.deserialize("<gray>Furniture Hitboxes: <aqua>" + totalInteractions + "</aqua></gray>"));
        sender.sendMessage(MM.deserialize("<gray>Active Player Seats: <green>" + totalSeats + "</green></gray>"));
        sender.sendMessage(MM.deserialize("<gray>Total Tracked Worlds: <white>" + Bukkit.getWorlds().size() + "</white></gray>"));
        sender.sendMessage(MM.deserialize("<gradient:#00c6ff:#0072ff><bold>═════════════════════════════════════</bold></gradient>"));
    }
}
