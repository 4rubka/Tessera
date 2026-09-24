package com.prisma.tessera.mechanics;

import com.prisma.tessera.TesseraPlugin;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public final class MechanicsManager {

    private final TesseraPlugin plugin;
    private final Map<String, MechanicFactory> factories = new HashMap<>();

    public MechanicsManager(TesseraPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerDefaults() {
    }

    public void registerFactory(String mechanicId, MechanicFactory factory) {
        factories.put(mechanicId, factory);
    }

    @Nullable
    public MechanicFactory getFactory(String mechanicId) {
        return factories.get(mechanicId);
    }

    public Map<String, MechanicFactory> getAll() {
        return java.util.Collections.unmodifiableMap(factories);
    }

    public void unregisterAll() {
        for (MechanicFactory factory : factories.values()) {
            factory.onUnregister();
        }
        factories.clear();
    }
}
