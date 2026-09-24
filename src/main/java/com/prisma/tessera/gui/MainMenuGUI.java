package com.prisma.tessera.gui;

import com.prisma.tessera.TesseraPlugin;
import com.prisma.tessera.items.ItemBuilder;
import java.util.List;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class MainMenuGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void open(Player player) {
        TesseraPlugin plugin = TesseraPlugin.get();
        TesseraGUI gui = new TesseraGUI("<white>:tessera_badge:</white> <gradient:#00c6ff:#0072ff><bold>TESSERA HUB</bold></gradient> <dark_gray>»</dark_gray> <gray>Main Menu</gray>", 6);

        ItemStack borderCyan = new ItemBuilder(Material.CYAN_STAINED_GLASS_PANE).setName("<gray> </gray>").build();
        ItemStack borderBlue = new ItemBuilder(Material.BLUE_STAINED_GLASS_PANE).setName("<gray> </gray>").build();
        ItemStack borderDark = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName("<gray> </gray>").build();

        for (int i = 0; i < 54; i++) {
            gui.setButton(i, GUIButton.of(borderDark));
        }

        for (int i = 0; i < 9; i++) {
            gui.setButton(i, GUIButton.of(i % 2 == 0 ? borderCyan : borderBlue));
        }
        
        for (int i = 45; i < 54; i++) {
            gui.setButton(i, GUIButton.of(i % 2 == 0 ? borderBlue : borderCyan));
        }

        int itemsCount = plugin.getItemRegistry() != null ? plugin.getItemRegistry().getAll().size() : 0;
        int blocksCount = plugin.getBlockRegistry() != null ? plugin.getBlockRegistry().getAll().size() : 0;
        int furnitureCount = plugin.getFurnitureManager() != null ? plugin.getFurnitureManager().getAll().size() : 0;
        int recipesCount = plugin.getRecipeManager() != null ? plugin.getRecipeManager().getAll().size() : 0;

        ItemStack bannerItem = new ItemBuilder(Material.CYAN_BANNER)
                .setName("<white>:tessera_banner:</white>")
                .setLore(List.of(
                        "<gradient:#00c6ff:#0072ff><bold>✦ T E S S E R A   E N G I N E ✦</bold></gradient>",
                        "<gray>Official custom objects & modern mechanics</gray>",
                        "<dark_gray>────────────────────────────────────────────</dark_gray>",
                        "<gray>❖ Custom Items:</gray> <yellow>" + itemsCount + "</yellow>",
                        "<gray>❖ Custom Blocks:</gray> <yellow>" + blocksCount + "</yellow>",
                        "<gray>❖ Interactive Furniture:</gray> <yellow>" + furnitureCount + "</yellow>",
                        "<gray>❖ Crafting Recipes:</gray> <yellow>" + recipesCount + "</yellow>",
                        "<dark_gray>────────────────────────────────────────────</dark_gray>",
                        "<aqua>Select a category below to explore all objects!</aqua>"
                ))
                .build();
        gui.setButton(4, GUIButton.of(bannerItem, p -> {
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        }));

        ItemStack weaponsItem = new ItemBuilder(Material.DIAMOND_SWORD)
                .setName("<gradient:#00c6ff:#0072ff><bold>⚔ Custom Items & Weapons</bold></gradient>")
                .setLore(List.of(
                        "<gray>Explore swords, bows, shields, tools</gray>",
                        "<gray>and consumable items with custom mechanics.</gray>",
                        "",
                        "<yellow>▶ Click to open Items Catalog</yellow>"
                ))
                .build();
        gui.setButton(20, GUIButton.of(weaponsItem, p -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            ItemBrowserGUI.open(p, ItemBrowserGUI.Category.ITEMS, 0);
        }));

        ItemStack blocksItem = new ItemBuilder(Material.CHISELED_STONE_BRICKS)
                .setName("<gradient:#00c6ff:#0072ff><bold>🧱 Custom Blocks & Ores</bold></gradient>")
                .setLore(List.of(
                        "<gray>Explore custom 3D blocks, ores, tiles,</gray>",
                        "<gray>and directional structures with hardness.</gray>",
                        "",
                        "<yellow>▶ Click to open Blocks Catalog</yellow>"
                ))
                .build();
        gui.setButton(21, GUIButton.of(blocksItem, p -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            ItemBrowserGUI.open(p, ItemBrowserGUI.Category.BLOCKS, 0);
        }));

        ItemStack furnitureItem = new ItemBuilder(Material.ARMOR_STAND)
                .setName("<gradient:#00c6ff:#0072ff><bold>🪑 Interactive Furniture</bold></gradient>")
                .setLore(List.of(
                        "<gray>Chairs, tables, sofas, lamps and</gray>",
                        "<gray>decorative models with player seating support.</gray>",
                        "",
                        "<yellow>▶ Click to open Furniture Catalog</yellow>"
                ))
                .build();
        gui.setButton(22, GUIButton.of(furnitureItem, p -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            ItemBrowserGUI.open(p, ItemBrowserGUI.Category.FURNITURE, 0);
        }));

        ItemStack recipesItem = new ItemBuilder(Material.KNOWLEDGE_BOOK)
                .setName("<gradient:#00c6ff:#0072ff><bold>📜 Recipe Codex</bold></gradient>")
                .setLore(List.of(
                        "<gray>Inspect all custom crafting recipes,</gray>",
                        "<gray>shaped and shapeless workbench matrixes.</gray>",
                        "",
                        "<yellow>▶ Click to browse recipes in catalog</yellow>"
                ))
                .build();
        gui.setButton(23, GUIButton.of(recipesItem, p -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            ItemBrowserGUI.open(p, ItemBrowserGUI.Category.ALL, 0);
        }));

        ItemStack allCatalogItem = new ItemBuilder(Material.NETHER_STAR)
                .setName("<gradient:#00c6ff:#0072ff><bold>✦ Master Catalog (All)</bold></gradient>")
                .setLore(List.of(
                        "<gray>View all registered custom objects</gray>",
                        "<gray>in a multi-page interactive browser.</gray>",
                        "",
                        "<yellow>▶ Click to open Master Browser</yellow>"
                ))
                .build();
        gui.setButton(24, GUIButton.of(allCatalogItem, p -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            ItemBrowserGUI.open(p, ItemBrowserGUI.Category.ALL, 0);
        }));

        ItemStack settingsItem = new ItemBuilder(Material.COMPARATOR)
                .setName("<gradient:#00c6ff:#0072ff><bold>⚙ Player Preferences & HUD</bold></gradient>")
                .setLore(List.of(
                        "<gray>Toggle screen status HUD, top BossBar,</gray>",
                        "<gray>audio feedback, and re-request pack.</gray>",
                        "",
                        "<yellow>▶ Click to open Settings</yellow>"
                ))
                .build();
        gui.setButton(30, GUIButton.of(settingsItem, p -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            SettingsGUI.open(p);
        }));

        boolean serverRunning = plugin.getPackServer() != null && plugin.getPackServer().isRunning();
        ItemStack packItem = new ItemBuilder(serverRunning ? Material.EMERALD : Material.REDSTONE)
                .setName(serverRunning ? "<green><bold>📦 Server Pack: ACTIVE</bold></green>" : "<red><bold>📦 Server Pack: OFFLINE</bold></red>")
                .setLore(List.of(
                        "<gray>URL: <white>" + (plugin.getPackServer() != null ? plugin.getPackServer().getPackUrl() : "None") + "</white></gray>",
                        "<gray>SHA-1: <white>" + (plugin.getPackServer() != null && plugin.getPackServer().getSha1Hex() != null ?
                                plugin.getPackServer().getSha1Hex().substring(0, Math.min(10, plugin.getPackServer().getSha1Hex().length())) + "..." : "None") + "</white></gray>",
                        "",
                        "<yellow>▶ Click to re-download resource pack</yellow>"
                ))
                .build();
        gui.setButton(31, GUIButton.of(packItem, p -> {
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.8f);
            if (plugin.getPackServer() != null && plugin.getPackServer().isRunning()) {
                p.setResourcePack(plugin.getPackServer().getPackUrl(), plugin.getPackServer().getSha1Hex(), true);
                p.sendMessage(MM.deserialize("<green>Sent official Tessera server pack to your client!</green>"));
            }
        }));

        if (player.hasPermission("tessera.admin")) {
            ItemStack adminItem = new ItemBuilder(Material.BEACON)
                    .setName("<gradient:#ff416c:#ff4b2b><bold>👑 Admin Control Center</bold></gradient>")
                    .setLore(List.of(
                            "<gray>Manage server pack, reload engines,</gray>",
                            "<gray>purge orphaned entities, run benchmarks.</gray>",
                            "",
                            "<red>▶ Click to open Admin Dashboard</red>"
                    ))
                    .build();
            gui.setButton(32, GUIButton.of(adminItem, p -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.5f);
                AdminDashboardGUI.open(p);
            }));
        } else {
            ItemStack lockItem = new ItemBuilder(Material.GRAY_DYE)
                    .setName("<dark_gray>👑 Admin Control Center (Locked)")
                    .setLore(List.of("<gray>Requires <red>tessera.admin</red> permission.</gray>"))
                    .build();
            gui.setButton(32, GUIButton.of(lockItem));
        }

        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .setName("<red><bold>✖ Close Menu</bold></red>")
                .build();
        gui.setButton(49, GUIButton.of(closeItem, p -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 0.8f);
            p.closeInventory();
        }));

        gui.open(player);
    }
}
