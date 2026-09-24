package com.prisma.tessera.gui;

import com.prisma.tessera.TesseraPlugin;
import com.prisma.tessera.items.ItemBuilder;
import java.util.List;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class SettingsGUI {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void open(Player player) {
        TesseraPlugin plugin = TesseraPlugin.get();
        TesseraGUI gui = new TesseraGUI("<white>:tessera_badge:</white> <gradient:#00c6ff:#0072ff><bold>PLAYER PREFERENCES</bold></gradient> <dark_gray>»</dark_gray> <gray>Settings</gray>", 5);

        ItemStack borderDark = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName("<gray> </gray>").build();
        ItemStack borderCyan = new ItemBuilder(Material.CYAN_STAINED_GLASS_PANE).setName("<gray> </gray>").build();
        ItemStack borderBlue = new ItemBuilder(Material.BLUE_STAINED_GLASS_PANE).setName("<gray> </gray>").build();

        for (int i = 0; i < 45; i++) {
            gui.setButton(i, GUIButton.of(borderDark));
        }
        for (int i = 0; i < 9; i++) {
            gui.setButton(i, GUIButton.of(i % 2 == 0 ? borderCyan : borderBlue));
        }
        for (int i = 36; i < 45; i++) {
            gui.setButton(i, GUIButton.of(i % 2 == 0 ? borderBlue : borderCyan));
        }

        ItemStack bannerItem = new ItemBuilder(Material.CYAN_BANNER)
                .setName("<white>:tessera_banner:</white>")
                .setLore(List.of(
                        "<gradient:#00c6ff:#0072ff><bold>✦ TESSERA PERSONAL SETTINGS ✦</bold></gradient>",
                        "<gray>Customize on-screen HUD and visual elements</gray>",
                        "<dark_gray>────────────────────────────────────────────</dark_gray>",
                        "<gray>Preferences are saved to your player session.</gray>",
                        "",
                        "<yellow>Click any option below to toggle</yellow>"
                ))
                .build();
        gui.setButton(4, GUIButton.of(bannerItem));

        boolean hudActive = plugin.getHudManager() != null && plugin.getHudManager().isHudEnabled(player);
        ItemStack hudItem = new ItemBuilder(hudActive ? Material.EXPERIENCE_BOTTLE : Material.GLASS_BOTTLE)
                .setName(hudActive ? "<green><bold>📊 On-Screen Status HUD: ON</bold></green>" : "<red><bold>📊 On-Screen Status HUD: OFF</bold></red>")
                .setLore(List.of(
                        "<gray>Displays custom hearts, mana, and</gray>",
                        "<gray>status indicators on your actionbar.</gray>",
                        "",
                        "<yellow>▶ Click to toggle HUD</yellow>"
                ))
                .build();
        gui.setButton(20, GUIButton.of(hudItem, p -> {
            if (plugin.getHudManager() != null) {
                boolean next = plugin.getHudManager().toggleHud(p);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, next ? 1.5f : 0.8f);
                p.sendMessage(MM.deserialize(next ? "<green>On-screen status HUD enabled.</green>" : "<red>On-screen status HUD disabled.</red>"));
                open(p);
            }
        }));

        boolean bannerActive = plugin.getHudManager() != null && plugin.getHudManager().isBannerEnabled(player);
        ItemStack bannerToggleItem = new ItemBuilder(bannerActive ? Material.NAME_TAG : Material.LEAD)
                .setName(bannerActive ? "<green><bold>🏷 Top BossBar Banner: ON</bold></green>" : "<red><bold>🏷 Top BossBar Banner: OFF</bold></red>")
                .setLore(List.of(
                        "<gray>Displays sleek server branding</gray>",
                        "<gray>and notices in top bossbar.</gray>",
                        "",
                        "<yellow>▶ Click to toggle Banner</yellow>"
                ))
                .build();
        gui.setButton(22, GUIButton.of(bannerToggleItem, p -> {
            if (plugin.getHudManager() != null) {
                boolean next = plugin.getHudManager().toggleBanner(p);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, next ? 1.5f : 0.8f);
                p.sendMessage(MM.deserialize(next ? "<green>Top BossBar banner enabled.</green>" : "<red>Top BossBar banner disabled.</red>"));
                open(p);
            }
        }));

        ItemStack packItem = new ItemBuilder(Material.ENDER_CHEST)
                .setName("<gradient:#00c6ff:#0072ff><bold>📦 Re-Download Resource Pack</bold></gradient>")
                .setLore(List.of(
                        "<gray>Force client to re-request and</gray>",
                        "<gray>apply the official server resource pack.</gray>",
                        "",
                        "<yellow>▶ Click to download pack</yellow>"
                ))
                .build();
        gui.setButton(24, GUIButton.of(packItem, p -> {
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.8f);
            if (plugin.getPackServer() != null && plugin.getPackServer().isRunning()) {
                p.setResourcePack(plugin.getPackServer().getPackUrl(), plugin.getPackServer().getSha1Hex(), true);
                p.sendMessage(MM.deserialize("<green>Sent official server pack download request!</green>"));
            }
        }));

        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .setName("<yellow>◀ Return to Main Hub</yellow>")
                .build();
        gui.setButton(36, GUIButton.of(backItem, p -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
            MainMenuGUI.open(p);
        }));

        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .setName("<red><bold>✖ Close Menu</bold></red>")
                .build();
        gui.setButton(40, GUIButton.of(closeItem, p -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 0.8f);
            p.closeInventory();
        }));

        gui.open(player);
    }
}
