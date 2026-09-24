package com.prisma.tessera.items;

import org.bukkit.configuration.ConfigurationSection;

public final class ItemParser {

    private final ConfigurationSection section;

    public ItemParser(ConfigurationSection section) {
        this.section = section;
    }

    public ItemTemplate parse() {
        String id = section.getName();
        return new ItemTemplate(id, section);
    }
}
