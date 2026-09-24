package com.prisma.tessera.hud;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class HudElement {

    private final String id;
    private final String displayText;
    private final String permission;
    private final boolean enabledByDefault;
    private final int updateInterval;
    private final ConfigurationSection section;

    public HudElement(
            @NotNull String id,
            @NotNull String displayText,
            @Nullable String permission,
            boolean enabledByDefault,
            int updateInterval,
            @Nullable ConfigurationSection section
    ) {
        this.id = id;
        this.displayText = displayText;
        this.permission = permission;
        this.enabledByDefault = enabledByDefault;
        this.updateInterval = Math.max(1, updateInterval);
        this.section = section;
    }

    public String getId() {
        return id;
    }

    public String getDisplayText() {
        return displayText;
    }

    @Nullable
    public String getPermission() {
        return permission;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    @Nullable
    public ConfigurationSection getSection() {
        return section;
    }
}
