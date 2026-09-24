package com.prisma.tessera.hud;

import com.prisma.tessera.TesseraPlugin;
import com.prisma.tessera.fonts.FontManager;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class HudManager implements Listener {

    public static final NamespacedKey HUD_ENABLED_KEY = new NamespacedKey("tessera", "hud_enabled");
    public static final NamespacedKey BANNER_ENABLED_KEY = new NamespacedKey("tessera", "banner_enabled");

    private static HudManager instance;

    private final TesseraPlugin plugin;
    private final Map<String, HudElement> huds = new LinkedHashMap<>();
    private final Set<UUID> disabledPlayers = new HashSet<>();
    private final Set<UUID> disabledBannerPlayers = new HashSet<>();
    private final Map<UUID, BossBar> activeBossBars = new ConcurrentHashMap<>();

    private BukkitTask tickerTask;

    public HudManager(@NotNull TesseraPlugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static HudManager get() {
        return instance;
    }

    public void loadAll() {
        huds.clear();
        for (YamlConfiguration config : plugin.getConfigManager().getConfigsInDirectory("hud").values()) {
            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section != null) {
                    register(parseHud(key, section));
                }
            }
        }
    }

    private HudElement parseHud(String id, ConfigurationSection section) {
        String displayText = section.getString("display_text", section.getString("text", ""));
        String permission = section.getString("permission");
        boolean enabledByDefault = section.getBoolean("enabled_by_default", true);
        int updateInterval = section.getInt("update_interval", 2);
        return new HudElement(id, displayText, permission, enabledByDefault, updateInterval, section);
    }

    public void register(@NotNull HudElement hud) {
        huds.put(hud.getId(), hud);
    }

    public void unregister(@NotNull String id) {
        huds.remove(id);
    }

    @Nullable
    public HudElement get(@NotNull String id) {
        return huds.get(id);
    }

    @NotNull
    public Collection<HudElement> getAll() {
        return Collections.unmodifiableCollection(huds.values());
    }

    public void start() {
        stop();
        tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 2L);
    }

    public void stop() {
        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
        }
        for (Map.Entry<UUID, BossBar> entry : activeBossBars.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null) {
                p.hideBossBar(entry.getValue());
            }
        }
        activeBossBars.clear();
    }

    private void tick() {
        boolean bannerGlobal = plugin.getConfig().getBoolean("hud.top_banner.enabled", true);
        String bannerTemplate = plugin.getConfig().getString("hud.top_banner.title",
                "<white>:top_banner:</white>");

for (Player player : Bukkit.getOnlinePlayers()) {
            if (isHudEnabled(player) && !huds.isEmpty()) {
                for (HudElement hud : huds.values()) {
                    if (hud.getPermission() != null && !player.hasPermission(hud.getPermission())) {
                        continue;
                    }
                    Component rendered = renderHud(player, hud.getDisplayText());
                    player.sendActionBar(rendered);
                    break;
                }
}

            if (bannerGlobal && isBannerEnabled(player)) {
                Component bannerComp = renderHud(player, bannerTemplate);
                BossBar bar = activeBossBars.computeIfAbsent(player.getUniqueId(), uuid -> {
                    BossBar.Color color = BossBar.Color.BLUE;
                    try {
                        String colorStr = plugin.getConfig().getString("hud.top_banner.color", "BLUE");
                        color = BossBar.Color.valueOf(colorStr.toUpperCase());
                    } catch (Exception ignored) {
                    }
                    BossBar newBar = BossBar.bossBar(bannerComp, 0.0f, color, BossBar.Overlay.PROGRESS);
                    player.showBossBar(newBar);
                    return newBar;
                });
                bar.name(bannerComp);
            } else {
                BossBar existing = activeBossBars.remove(player.getUniqueId());
                if (existing != null) {
                    player.hideBossBar(existing);
                }
            }
        }
    }

    /**
     * A bossbar is tied to the connection that saw it. Keeping it after quit means a rejoining player
     * gets the cached bar, which is never shown to the new connection, so the banner disappears.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        activeBossBars.remove(id);
        disabledPlayers.remove(id);
        disabledBannerPlayers.remove(id);
    }

    @NotNull
    public Component renderHud(@NotNull Player player, @NotNull String template) {
        String text = template;

        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 20.0;

        text = text.replace("<player>", player.getName())
                .replace("<health>", String.valueOf((int) player.getHealth()))
                .replace("<max_health>", String.valueOf((int) maxHealth))
                .replace("<food>", String.valueOf(player.getFoodLevel()))
                .replace("<level>", String.valueOf(player.getLevel()))
                .replace("<ping>", String.valueOf(player.getPing()))
                .replace("<world>", player.getWorld().getName())
                .replace("<x>", String.valueOf(player.getLocation().getBlockX()))
                .replace("<y>", String.valueOf(player.getLocation().getBlockY()))
                .replace("<z>", String.valueOf(player.getLocation().getBlockZ()));

        FontManager fontManager = FontManager.get();
        if (fontManager != null) {
            return fontManager.format(text);
        }
        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(text);
    }

    public boolean isHudEnabled(@NotNull Player player) {
        if (disabledPlayers.contains(player.getUniqueId())) {
            return false;
        }
        Byte stored = player.getPersistentDataContainer().get(HUD_ENABLED_KEY, PersistentDataType.BYTE);
        if (stored != null) {
            return stored == (byte) 1;
        }
        return true;
    }

    public void setHudEnabled(@NotNull Player player, boolean enabled) {
        if (enabled) {
            disabledPlayers.remove(player.getUniqueId());
            player.getPersistentDataContainer().set(HUD_ENABLED_KEY, PersistentDataType.BYTE, (byte) 1);
        } else {
            disabledPlayers.add(player.getUniqueId());
            player.getPersistentDataContainer().set(HUD_ENABLED_KEY, PersistentDataType.BYTE, (byte) 0);
        }
    }

    public boolean toggleHud(@NotNull Player player) {
        boolean newState = !isHudEnabled(player);
        setHudEnabled(player, newState);
        return newState;
    }

    public boolean isBannerEnabled(@NotNull Player player) {
        if (disabledBannerPlayers.contains(player.getUniqueId())) {
            return false;
        }
        Byte stored = player.getPersistentDataContainer().get(BANNER_ENABLED_KEY, PersistentDataType.BYTE);
        if (stored != null) {
            return stored == (byte) 1;
        }
        return true;
    }

    public void setBannerEnabled(@NotNull Player player, boolean enabled) {
        if (enabled) {
            disabledBannerPlayers.remove(player.getUniqueId());
            player.getPersistentDataContainer().set(BANNER_ENABLED_KEY, PersistentDataType.BYTE, (byte) 1);
        } else {
            disabledBannerPlayers.add(player.getUniqueId());
            player.getPersistentDataContainer().set(BANNER_ENABLED_KEY, PersistentDataType.BYTE, (byte) 0);
            BossBar bar = activeBossBars.remove(player.getUniqueId());
            if (bar != null) {
                player.hideBossBar(bar);
            }
        }
    }

    public boolean toggleBanner(@NotNull Player player) {
        boolean newState = !isBannerEnabled(player);
        setBannerEnabled(player, newState);
        return newState;
    }
}
