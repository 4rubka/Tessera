package com.prisma.tessera.utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;

/**
 * Turns a sound name from config into a sound key the client understands.
 *
 * Config uses enum-style names such as BLOCK_NOTE_BLOCK_CHIME. Replacing every underscore with a
 * dot gives block.note.block.chime, which does not exist (the key is block.note_block.chime), so
 * the client silently plays nothing. The registry is the only reliable source for the mapping.
 */
public final class SoundKeys {

    private static Map<String, String> byEnumName;

    private SoundKeys() {
    }

    @NotNull
    public static String resolve(@NotNull String name) {
        String trimmed = name.trim();
        if (trimmed.indexOf('.') >= 0 || trimmed.indexOf(':') >= 0) {
            return trimmed.toLowerCase(Locale.ROOT);
        }
        String key = table().get(trimmed.toUpperCase(Locale.ROOT));
        return key != null ? key : trimmed.toLowerCase(Locale.ROOT).replace('_', '.');
    }

    private static synchronized Map<String, String> table() {
        if (byEnumName == null) {
            Map<String, String> map = new HashMap<>();
            for (Sound sound : Registry.SOUNDS) {
                NamespacedKey key = Registry.SOUNDS.getKey(sound);
                if (key != null && NamespacedKey.MINECRAFT.equals(key.getNamespace())) {
                    map.put(key.getKey().replace('.', '_').toUpperCase(Locale.ROOT), key.getKey());
                }
            }
            byEnumName = map;
        }
        return byEnumName;
    }
}
