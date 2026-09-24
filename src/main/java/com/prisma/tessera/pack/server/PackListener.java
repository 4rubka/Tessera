package com.prisma.tessera.pack.server;

import com.prisma.tessera.TesseraPlugin;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class PackListener implements Listener {

    private final TesseraPlugin plugin;
    private final PackServer packServer;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Set<UUID> loadingPlayers = Collections.synchronizedSet(new HashSet<>());

    public PackListener(TesseraPlugin plugin, PackServer packServer) {
        this.plugin = plugin;
        this.packServer = packServer;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        boolean autoApply = plugin.getConfig().getBoolean("pack.server.auto_apply.enabled", true);
        if (!autoApply) return;

        Player player = event.getPlayer();
        int delayTicks = plugin.getConfig().getInt("pack.server.auto_apply.delay_ticks", 10);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                sendResourcePack(player);
            }
        }, Math.max(1, delayTicks));
    }

    public void sendToAllOnline() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendResourcePack(p);
        }
    }

    public void sendResourcePack(Player player) {
        String url = packServer.getPackUrl();
        if (url == null || url.isBlank()) {
            return;
        }

        byte[] sha1 = packServer.getSha1Bytes();
        boolean required = plugin.getConfig().getBoolean("pack.server.required", true);
        String promptMsg = packMessage("prompt",
                "<gradient:#00c6ff:#0072ff><b>Tessera</b></gradient>\n<gray>Please accept the server resource pack to view custom items, blocks & furniture!</gray>");
        Component prompt = mm.deserialize(promptMsg);

        if (plugin.getConfig().getBoolean("pack.protect_player.enabled", true)) {
            loadingPlayers.add(player.getUniqueId());

            if (plugin.getConfig().getBoolean("pack.protect_player.blindness", true)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 60, 1, false, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 60, 255, false, false, false));
            }

            String titleStr = packMessage("loading_title", "<gradient:#00c6ff:#0072ff><b>Tessera</b></gradient>");
            String subTitleStr = packMessage("loading_subtitle", "<gray>Loading resource pack...</gray>");
            Title loadingTitle = Title.title(
                    mm.deserialize(titleStr),
                    mm.deserialize(subTitleStr),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(30), Duration.ofMillis(500))
            );
            player.showTitle(loadingTitle);

            // A client that never answers would otherwise stay invulnerable and frozen.
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) releasePlayer(player);
            }, 20L * 60);
        }

        try {
            if (sha1 != null && sha1.length == 20) {
                player.setResourcePack(url, sha1, prompt, required);
            } else {
                player.setResourcePack(url);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[PackListener] Could not send resource pack to " + player.getName() + ": " + e.getMessage());
            releasePlayer(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        PlayerResourcePackStatusEvent.Status status = event.getStatus();

        switch (status) {
            case ACCEPTED -> {
                
                player.sendActionBar(mm.deserialize("<aqua>⏳ Downloading server resource pack...</aqua>"));
            }
            case SUCCESSFULLY_LOADED -> {
                releasePlayer(player);
                String loadedSound = plugin.getConfig().getString("pack.sounds.on_loaded", "entity.player.levelup");
                try {
                    player.playSound(player.getLocation(), com.prisma.tessera.utils.SoundKeys.resolve(loadedSound), 1.0f, 1.2f);
                } catch (Exception ignored) {
                }

                String successMsg = packMessage("loaded_actionbar", "<green>✔ Resource pack loaded successfully!</green>");
                player.sendActionBar(mm.deserialize(successMsg));

                String chatMsg = packMessage("loaded_chat", "");
                if (!chatMsg.isBlank()) {
                    player.sendMessage(mm.deserialize(chatMsg));
                }
            }
            case DECLINED -> {
                releasePlayer(player);
                boolean kickOnDecline = plugin.getConfig().getBoolean("pack.server.kick_on_decline", true);
                if (kickOnDecline) {
                    String kickReason = packMessage("kick_declined",
                            "<gradient:#ff416c:#ff4b2b><b>Resource Pack Required</b></gradient>\n\n<gray>You must accept the resource pack to play on this server.\nGo to Multiplayer -> Edit Server -> Server Resource Packs: Enabled.</gray>");
                    player.kick(mm.deserialize(kickReason));
                } else {
                    String warnMsg = packMessage("declined_warning",
                            "<red>⚠ You declined the resource pack. Custom items and textures will not display correctly.</red>");
                    player.sendMessage(mm.deserialize(warnMsg));
                }
            }
            case FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD -> {
                releasePlayer(player);
                boolean kickOnFail = plugin.getConfig().getBoolean("pack.server.kick_on_fail", false);
                if (kickOnFail) {
                    String failReason = packMessage("kick_failed",
                            "<red>Failed to download the resource pack. Please check your connection and reconnect.</red>");
                    player.kick(mm.deserialize(failReason));
                } else {
                    String failWarn = packMessage("failed_warning",
                            "<red>⚠ Failed to download resource pack. You can retry with /tessera pack reload.</red>");
                    player.sendMessage(mm.deserialize(failWarn));
                }
            }
            case DISCARDED -> releasePlayer(player);
        }
    }

    private void releasePlayer(Player player) {
        if (loadingPlayers.remove(player.getUniqueId())) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.clearTitle();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!loadingPlayers.contains(event.getPlayer().getUniqueId())) return;
        boolean lockMovement = plugin.getConfig().getBoolean("pack.protect_player.lock_movement", true);
        if (!lockMovement) return;

        if (event.getFrom().getX() != event.getTo().getX() ||
            event.getFrom().getY() != event.getTo().getY() ||
            event.getFrom().getZ() != event.getTo().getZ()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (loadingPlayers.contains(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        loadingPlayers.remove(event.getPlayer().getUniqueId());
    }

    /** Messages live under messages.pack in config.yml; pack.messages is still read for older configs. */
    private String packMessage(String key, String fallback) {
        return plugin.getConfig().getString("messages.pack." + key,
                plugin.getConfig().getString("pack.messages." + key, fallback));
    }
}
