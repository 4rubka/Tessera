package com.prisma.tessera.mechanics;

import org.bukkit.configuration.ConfigurationSection;

public abstract class Mechanic {

    private final MechanicFactory factory;
    private final String itemId;
    private final ConfigurationSection section;

    protected Mechanic(MechanicFactory factory, ConfigurationSection section) {
        this.factory = factory;
        this.section = section;
        String id = "unknown";
        if (section != null) {
            if (section.getParent() != null && section.getParent().getParent() != null) {
                id = section.getParent().getParent().getName();
            } else if (section.getParent() != null) {
                id = section.getParent().getName();
            } else {
                id = section.getName();
            }
        }
        this.itemId = id;
    }

    public String getItemId() {
        return itemId;
    }

    public MechanicFactory getFactory() {
        return factory;
    }

    public ConfigurationSection getSection() {
        return section;
    }
}
