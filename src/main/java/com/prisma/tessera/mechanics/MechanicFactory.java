package com.prisma.tessera.mechanics;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

public abstract class MechanicFactory {

    private final Map<String, Mechanic> mechanics = new HashMap<>();
    private final String mechanicId;

    protected MechanicFactory(String mechanicId) {
        this.mechanicId = mechanicId;
    }

    public String getMechanicId() {
        return mechanicId;
    }

    public abstract Mechanic parse(ConfigurationSection section);

    public void register(String itemId, Mechanic mechanic) {
        mechanics.put(itemId, mechanic);
    }

    @Nullable
    public Mechanic getMechanic(String itemId) {
        return mechanics.get(itemId);
    }

    public boolean hasMechanic(String itemId) {
        return mechanics.containsKey(itemId);
    }

    public Map<String, Mechanic> getAll() {
        return mechanics;
    }

    public void onUnregister() {
    }
}
