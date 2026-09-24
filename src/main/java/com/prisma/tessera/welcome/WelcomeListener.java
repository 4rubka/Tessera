package com.prisma.tessera.welcome;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class WelcomeListener implements Listener {

    private final WelcomeManager welcomeManager;

    public WelcomeListener(WelcomeManager welcomeManager) {
        this.welcomeManager = welcomeManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        welcomeManager.handlePlayerJoin(player);
    }
}
