package com.prisma.tessera.gui;

import com.prisma.tessera.TesseraPlugin;
import com.prisma.tessera.items.ItemBuilder;
import com.prisma.tessera.utils.DebugBenchmark;
import com.prisma.tessera.utils.FurnitureTool;
import com.prisma.tessera.utils.SeatManager;
import java.util.List;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class AdminDashboardGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void open(Player player) {
        if (!player.hasPermission("tessera.admin")) {
            player.sendMessage(MM.deserialize("<red>You do not have permission to access the Admin Control Center.</red>"));
            return;
        }

        TesseraPlugin plugin = TesseraPlugin.get();
        TesseraGUI gui = new TesseraGUI("<white>:tessera_badge:</white> <gradient:#ff416c:#ff4b2b><bold>ADMIN CONTROL CENTER</bold></gradient> <dark_gray>»</dark_gray> <gray>Dashboard</gray>", 6);

        ItemStack borderDark = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName("<gray> </gray>").build();
        ItemStack borderRed = new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setName("<gray> </gray>").build();
        ItemStack borderCyan = new ItemBuilder(Material.CYAN_STAINED_GLASS_PANE).setName("<gray> </gray>").build();

        for (int i = 0; i < 54; i++) {
            gui.setButton(i, GUIButton.of(borderDark));
        }
        for (int i = 0; i < 9; i++) {
            gui.setButton(i, GUIButton.of(i % 2 == 0 ? borderRed : borderCyan));
        }
        for (int i = 45; i < 54; i++) {
            gui.setButton(i, GUIButton.of(i % 2 == 0 ? borderCyan : borderRed));
        }

        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long maxMb = rt.maxMemory() / 1024 / 1024;
        boolean serverRunning = plugin.getPackServer() != null && plugin.getPackServer().isRunning();
        String sha1 = plugin.getPackServer() != null && plugin.getPackServer().getSha1Hex() != null
                ? plugin.getPackServer().getSha1Hex().substring(0, Math.min(10, plugin.getPackServer().getSha1Hex().length())) + "..."
                : "None";

        ItemStack bannerItem = new ItemBuilder(Material.NETHER_STAR)
                .setName("<white>:tessera_banner:</white>")
                .setLore(List.of(
                        "<gradient:#00c6ff:#0072ff><bold>✦ TESSERA SERVER CONTROLS ✦</bold></gradient>",
                        "<gray>Server Engine Control & Performance Telemetry</gray>",
                        "<dark_gray>────────────────────────────────────────────</dark_gray>",
                        "<gray>❖ Memory Usage:</gray> <yellow>" + usedMb + " MB / " + maxMb + " MB</yellow>",
                        "<gray>❖ Server Pack SHA-1:</gray> <aqua>" + sha1 + "</aqua>",
                        "<gray>❖ Pack Server:</gray> " + (serverRunning ? "<green>ACTIVE</green>" : "<red>OFFLINE</red>"),
                        "<gray>❖ Registered Mechanics:</gray> <yellow>" + (plugin.getMechanicsManager() != null ? plugin.getMechanicsManager().getAll().size() : 0) + "</yellow>",
                        "<dark_gray>────────────────────────────────────────────</dark_gray>",
                        "<yellow>Select an administrative action below</yellow>"
                ))
                .build();
        gui.setButton(4, GUIButton.of(bannerItem));

        ItemStack reloadItem = new ItemBuilder(Material.BLAZE_POWDER)
                .setName("<gradient:#00c6ff:#0072ff><bold>🔄 Reload Entire Engine</bold></gradient>")
                .setLore(List.of(
                        "<gray>Reloads items, blocks, furniture,</gray>",
                        "<gray>recipes, glyphs, and localizations.</gray>",
                        "",
                        "<yellow>▶ Click to execute reload</yellow>"
                ))
                .build();
        gui.setButton(19, GUIButton.of(reloadItem, p -> {
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
            plugin.getConfigManager().reload();
            plugin.reloadAllData();
            p.sendMessage(MM.deserialize("<green>Engine reloaded successfully!</green>"));
            open(p);
        }));

        ItemStack pushPackItem = new ItemBuilder(Material.ENDER_CHEST)
                .setName("<gradient:#00c6ff:#0072ff><bold>📦 Push Resource Pack to All</bold></gradient>")
                .setLore(List.of(
                        "<gray>Re-generates server pack, updates SHA-1,</gray>",
                        "<gray>and prompts all online players to download.</gray>",
                        "",
                        "<yellow>▶ Click to push pack</yellow>"
                ))
                .build();
        gui.setButton(20, GUIButton.of(pushPackItem, p -> {
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
            plugin.getPackGenerator().generate();
            if (plugin.getPackServer() != null && plugin.getPackServer().isRunning()) {
                String url = plugin.getPackServer().getPackUrl();
                String hash = plugin.getPackServer().getSha1Hex();
                for (Player online : plugin.getServer().getOnlinePlayers()) {
                    online.setResourcePack(url, hash, true);
                }
                p.sendMessage(MM.deserialize("<green>Pushed updated resource pack to " + plugin.getServer().getOnlinePlayers().size() + " players!</green>"));
            }
            open(p);
        }));

        ItemStack importItem = new ItemBuilder(Material.HOPPER)
                .setName("<gradient:#00c6ff:#0072ff><bold>📥 Run Smart Pack Importer</bold></gradient>")
                .setLore(List.of(
                        "<gray>Scans <white>plugins/Tessera/imports/</white></gray>",
                        "<gray>auto-converts zips and merges into server pack.</gray>",
                        "",
                        "<yellow>▶ Click to import pending packs</yellow>"
                ))
                .build();
        gui.setButton(21, GUIButton.of(importItem, p -> {
            p.playSound(p.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.8f, 1.2f);
            p.sendMessage(MM.deserialize("<aqua>Starting smart pack ingestion...</aqua>"));
            if (plugin.getSmartPackImporter() != null) {
                org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    java.io.File[] zips = plugin.getSmartPackImporter().getImportsDir().listFiles(
                            (dir, name) -> name.toLowerCase(java.util.Locale.ROOT).endsWith(".zip"));
                    int importCount = zips != null ? zips.length : 0;
                    if (zips != null) {
                        for (java.io.File zip : zips) {
                            plugin.getSmartPackImporter().importZip(zip, true, true);
                        }
                    }
                    final int finalCount = importCount;
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        p.sendMessage(MM.deserialize("<green>Smart pack import finished! Processed " + finalCount + " packs.</green>"));
                        open(p);
                    });
                });
            }
        }));

        ItemStack purgeFurnItem = new ItemBuilder(Material.TNT)
                .setName("<gradient:#ff416c:#ff4b2b><bold>🧹 Purge Nearby Furniture (25m)</bold></gradient>")
                .setLore(List.of(
                        "<gray>Removes corrupted or orphaned furniture</gray>",
                        "<gray>entities within a 25 block radius.</gray>",
                        "",
                        "<red>▶ Click to purge nearby furniture</red>"
                ))
                .build();
        gui.setButton(22, GUIButton.of(purgeFurnItem, p -> {
            FurnitureTool.removeNearby(p, 25.0);
        }));

        ItemStack cleanSeatsItem = new ItemBuilder(Material.OAK_STAIRS)
                .setName("<gradient:#00c6ff:#0072ff><bold>🪑 Clean Orphaned Seats</bold></gradient>")
                .setLore(List.of(
                        "<gray>Safely purges any empty temporary seat</gray>",
                        "<gray>armor stands server-wide.</gray>",
                        "",
                        "<yellow>▶ Click to clean seats</yellow>"
                ))
                .build();
        gui.setButton(23, GUIButton.of(cleanSeatsItem, p -> {
            int cleaned = SeatManager.get() != null ? SeatManager.get().cleanupAllOrphans() : 0;
            p.playSound(p.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 0.8f, 1.2f);
            p.sendMessage(MM.deserialize("<green>Purged <yellow>" + cleaned + "</yellow> orphaned seat entities server-wide!</green>"));
        }));

        ItemStack benchItem = new ItemBuilder(Material.CLOCK)
                .setName("<gradient:#00c6ff:#0072ff><bold>⚡ Performance Benchmark</bold></gradient>")
                .setLore(List.of(
                        "<gray>Runs precision benchmark measuring</gray>",
                        "<gray>item lookup, registry queries and PDC speed.</gray>",
                        "",
                        "<yellow>▶ Click to run benchmark</yellow>"
                ))
                .build();
        gui.setButton(24, GUIButton.of(benchItem, p -> {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
            DebugBenchmark.runBenchmark(p);
        }));

        boolean bannerEnabled = plugin.getConfig().getBoolean("hud.top_banner.enabled", false);
        ItemStack toggleBannerItem = new ItemBuilder(bannerEnabled ? Material.LIME_DYE : Material.GRAY_DYE)
                .setName(bannerEnabled ? "<green><bold>🏷 Top BossBar: ENABLED</bold></green>" : "<red><bold>🏷 Top BossBar: DISABLED</bold></red>")
                .setLore(List.of(
                        "<gray>Config value: <white>hud.top_banner.enabled</white></gray>",
                        "",
                        "<yellow>▶ Click to toggle globally</yellow>"
                ))
                .build();
        gui.setButton(25, GUIButton.of(toggleBannerItem, p -> {
            boolean next = !bannerEnabled;
            plugin.getConfig().set("hud.top_banner.enabled", next);
            plugin.saveConfig();
            if (plugin.getHudManager() != null) {
                for (org.bukkit.entity.Player online : plugin.getServer().getOnlinePlayers()) {
                    plugin.getHudManager().setBannerEnabled(online, next);
                }
            }
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, next ? 1.5f : 0.8f);
            p.sendMessage(MM.deserialize(next ? "<green>Top BossBar banner enabled globally.</green>" : "<red>Top BossBar banner disabled globally.</red>"));
            open(p);
        }));

        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .setName("<yellow>◀ Return to Main Hub</yellow>")
                .build();
        gui.setButton(45, GUIButton.of(backItem, p -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
            MainMenuGUI.open(p);
        }));

        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .setName("<red><bold>✖ Close Menu</bold></red>")
                .build();
        gui.setButton(49, GUIButton.of(closeItem, p -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 0.8f);
            p.closeInventory();
        }));

        ItemStack refreshItem = new ItemBuilder(Material.SUNFLOWER)
                .setName("<gold><bold>↻ Refresh Dashboard</bold></gold>")
                .build();
        gui.setButton(53, GUIButton.of(refreshItem, p -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            open(p);
        }));

        gui.open(player);
    }
}
