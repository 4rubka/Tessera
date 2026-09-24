package com.prisma.tessera.welcome;

import com.prisma.tessera.TesseraPlugin;
import java.time.Duration;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;

public final class WelcomeManager {

    private final TesseraPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final NamespacedKey welcomedKey;

    public WelcomeManager(TesseraPlugin plugin) {
        this.plugin = plugin;
        this.welcomedKey = new NamespacedKey(plugin, "first_join_welcomed");
    }

    public void handlePlayerJoin(Player player) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("welcome.enabled", true)) {
            return;
        }

        int delayTicks = config.getInt("welcome.delay_ticks", 25);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            boolean isFirstJoin = !player.hasPlayedBefore() &&
                    !player.getPersistentDataContainer().has(welcomedKey, PersistentDataType.BYTE);

            if (isFirstJoin) {
                sendFirstJoinWelcome(player, config);
                player.getPersistentDataContainer().set(welcomedKey, PersistentDataType.BYTE, (byte) 1);
            } else {
                sendRegularWelcome(player, config);
            }
        }, Math.max(1, delayTicks));
    }

    private void sendFirstJoinWelcome(Player player, FileConfiguration config) {
        if (!config.getBoolean("welcome.first_join.enabled", true)) {
            return;
        }

        String soundName = config.getString("welcome.first_join.sound.type", "UI_TOAST_CHALLENGE_COMPLETE");
        float volume = (float) config.getDouble("welcome.first_join.sound.volume", 1.0);
        float pitch = (float) config.getDouble("welcome.first_join.sound.pitch", 1.0);
        playSound(player, soundName, volume, pitch);

        String titleStr = format(config.getString("welcome.first_join.title", "<gradient:#00c6ff:#0072ff><b>WELCOME!</b></gradient>"), player);
        String subTitleStr = format(config.getString("welcome.first_join.subtitle", "<gray>Welcome to our server, <yellow>%player%</yellow>!</gray>"), player);
        int fadeIn = config.getInt("welcome.first_join.fade_in_ticks", 15);
        int stay = config.getInt("welcome.first_join.stay_ticks", 80);
        int fadeOut = config.getInt("welcome.first_join.fade_out_ticks", 20);

        player.showTitle(Title.title(
                mm.deserialize(titleStr),
                mm.deserialize(subTitleStr),
                Title.Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L))
        ));

        String actionbarStr = format(config.getString("welcome.first_join.actionbar", "<gradient:#f12711:#f5af19>✨ Have fun on your first adventure!</gradient>"), player);
        if (!actionbarStr.isBlank()) {
            player.sendActionBar(mm.deserialize(actionbarStr));
        }

        List<String> chatLines = config.getStringList("welcome.first_join.chat_lines");
        if (chatLines.isEmpty()) {
            chatLines = List.of(
                    "<gradient:#00c6ff:#0072ff><b>========================================</b></gradient>",
                    " <gradient:#00c6ff:#0072ff><b>TESSERA ENGINE</b></gradient> <dark_gray>»</dark_gray> <gray>Welcome <aqua>%player%</aqua> to the server!</gray>",
                    " <gray>Type <click:run_command:'/tessera menu'><hover:show_text:'<green>Click to open catalog!'><yellow><u>/tessera menu</u></yellow></hover></click> to view all custom items and blocks.</gray>",
                    " <gray>Need help? Visit our Discord or ask a staff member.</gray>",
                    "<gradient:#00c6ff:#0072ff><b>========================================</b></gradient>"
            );
        }
        for (String line : chatLines) {
            player.sendMessage(mm.deserialize(format(line, player)));
        }

        if (config.getBoolean("welcome.first_join.broadcast.enabled", true)) {
            String broadcastStr = format(config.getString("welcome.first_join.broadcast.message",
                    "<gradient:#00c6ff:#0072ff><b>Welcome</b></gradient> <aqua>%player%</aqua> <gray>joined the server for the first time! Say hello! 👋</gray>"), player);
            Bukkit.broadcast(mm.deserialize(broadcastStr));
        }

        if (config.getBoolean("welcome.first_join.book.enabled", true)) {
            giveWelcomeBook(player, config);
        }
    }

    private void sendRegularWelcome(Player player, FileConfiguration config) {
        if (!config.getBoolean("welcome.regular_join.enabled", true)) {
            return;
        }

        String soundName = config.getString("welcome.regular_join.sound.type", "ENTITY_EXPERIENCE_ORB_PICKUP");
        float volume = (float) config.getDouble("welcome.regular_join.sound.volume", 0.8);
        float pitch = (float) config.getDouble("welcome.regular_join.sound.pitch", 1.2);
        playSound(player, soundName, volume, pitch);

        String titleStr = format(config.getString("welcome.regular_join.title", "<gradient:#00c6ff:#0072ff><b>WELCOME BACK</b></gradient>"), player);
        String subTitleStr = format(config.getString("welcome.regular_join.subtitle", "<gray>Hello, <aqua>%player%</aqua>! Enjoy your game.</gray>"), player);
        int fadeIn = config.getInt("welcome.regular_join.fade_in_ticks", 10);
        int stay = config.getInt("welcome.regular_join.stay_ticks", 50);
        int fadeOut = config.getInt("welcome.regular_join.fade_out_ticks", 15);

        player.showTitle(Title.title(
                mm.deserialize(titleStr),
                mm.deserialize(subTitleStr),
                Title.Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L))
        ));

        String actionbarStr = format(config.getString("welcome.regular_join.actionbar", "<gray>Welcome back, <yellow>%player%</yellow>!</gray>"), player);
        if (!actionbarStr.isBlank()) {
            player.sendActionBar(mm.deserialize(actionbarStr));
        }

        List<String> chatLines = config.getStringList("welcome.regular_join.chat_lines");
        for (String line : chatLines) {
            player.sendMessage(mm.deserialize(format(line, player)));
        }

        if (config.getBoolean("welcome.regular_join.broadcast.enabled", false)) {
            String broadcastStr = format(config.getString("welcome.regular_join.broadcast.message",
                    "<gray>[<green>+</green>] <yellow>%player%</yellow> joined the game.</gray>"), player);
            Bukkit.broadcast(mm.deserialize(broadcastStr));
        }
    }

    private void giveWelcomeBook(Player player, FileConfiguration config) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) return;

        String title = format(config.getString("welcome.first_join.book.title", "<gradient:#00c6ff:#0072ff>Starter Guide</gradient>"), player);
        String author = config.getString("welcome.first_join.book.author", "Tessera Server");

        meta.title(mm.deserialize(title));
        meta.author(mm.deserialize(author));

        List<String> pages = config.getStringList("welcome.first_join.book.pages");
        if (pages.isEmpty()) {
            pages = List.of(
                    "<bold><gradient:#00c6ff:#0072ff>Welcome to Tessera!</gradient></bold>\n\n<gray>This server features custom items, blocks, furniture, and unique mechanics.\n\nType <click:run_command:'/tessera menu'><hover:show_text:'Open Menu!'><blue><u>/tessera menu</u></blue></hover></click> to view our items!</gray>",
                    "<bold><underlined>Useful Commands:</underlined></bold>\n\n<dark_gray>•</dark_gray> <dark_blue>/tessera menu</dark_blue> - Browse items\n<dark_gray>•</dark_gray> <dark_blue>/tessera hud</dark_blue> - Toggle HUD\n<dark_gray>•</dark_gray> <dark_blue>/spawn</dark_blue> - Return to spawn\n\n<gray>Have fun playing!</gray>"
            );
        }

        for (String page : pages) {
            meta.addPages(mm.deserialize(format(page, player)));
        }

        book.setItemMeta(meta);
        player.getInventory().addItem(book);
    }

    private void playSound(Player player, String soundName, float volume, float pitch) {
        if (soundName == null || soundName.isBlank()) return;
        try {
            player.playSound(player.getLocation(), com.prisma.tessera.utils.SoundKeys.resolve(soundName), volume, pitch);
        } catch (Exception ignored) {
        }
    }

    private String format(String template, Player player) {
        if (template == null) return "";
        String result = template
                .replace("%player%", player.getName())
                .replace("%displayname%", player.displayName() != null ? mm.serialize(player.displayName()) : player.getName())
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%max_players%", String.valueOf(Bukkit.getMaxPlayers()));

        if (plugin.getFontManager() != null) {
            result = plugin.getFontManager().replaceGlyphs(result);
        }
        return result;
    }
}
