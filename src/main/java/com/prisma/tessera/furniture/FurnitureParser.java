package com.prisma.tessera.furniture;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public final class FurnitureParser {

    private final ConfigurationSection section;

    public FurnitureParser(@NotNull ConfigurationSection section) {
        this.section = section;
    }

    public FurnitureType parse() {
        String id = section.getName();
        FurnitureBuilder builder = new FurnitureBuilder(id);

        builder.setItemId(section.getString("item_id", id));
        builder.setDisplayName(section.getString("display_name", id));

        String displayTypeStr = section.getString("display_type", "ITEM_DISPLAY").toUpperCase();
        try {
            builder.setDisplayType(FurnitureDisplayType.valueOf(displayTypeStr));
        } catch (IllegalArgumentException e) {
            builder.setDisplayType(FurnitureDisplayType.ITEM_DISPLAY);
        }

        ConfigurationSection hitboxSec = section.getConfigurationSection("hitbox");
        if (hitboxSec != null) {
            float width = (float) hitboxSec.getDouble("width", 1.0);
            float height = (float) hitboxSec.getDouble("height", 1.0);
            builder.setHitbox(width, height);
        } else {
            builder.setHitbox(
                    (float) section.getDouble("hitbox_width", 1.0),
                    (float) section.getDouble("hitbox_height", 1.0)
            );
        }

        ConfigurationSection scaleSec = section.getConfigurationSection("scale");
        if (scaleSec != null) {
            float sx = (float) scaleSec.getDouble("x", 1.0);
            float sy = (float) scaleSec.getDouble("y", 1.0);
            float sz = (float) scaleSec.getDouble("z", 1.0);
            builder.setScale(sx, sy, sz);
        } else if (section.contains("scale")) {
            float s = (float) section.getDouble("scale", 1.0);
            builder.setScale(s, s, s);
        }

        ConfigurationSection transSec = section.getConfigurationSection("translation");
        if (transSec != null) {
            float tx = (float) transSec.getDouble("x", 0.0);
            float ty = (float) transSec.getDouble("y", 0.0);
            float tz = (float) transSec.getDouble("z", 0.0);
            builder.setTranslation(tx, ty, tz);
        }

        boolean isSeat = section.getBoolean("seat", false);
        double seatHeight = section.getDouble("seat_height", 0.5);
        builder.setSeat(isSeat, seatHeight);

        ConfigurationSection soundSec = section.getConfigurationSection("sounds");
        if (soundSec != null) {
            builder.setSounds(
                    soundSec.getString("place", "BLOCK_WOOD_PLACE"),
                    soundSec.getString("break", "BLOCK_WOOD_BREAK")
            );
        } else {
            builder.setSounds(
                    section.getString("place_sound", "BLOCK_WOOD_PLACE"),
                    section.getString("break_sound", "BLOCK_WOOD_BREAK")
            );
        }

        builder.setDropItemId(section.getString("drop", id));
        builder.setCancelDrop(section.getBoolean("cancel_drop", false));
        builder.setCardinalRotation(section.getBoolean("cardinal_rotation", true));
        builder.setSection(section);

        return builder.build();
    }
}
