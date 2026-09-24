package com.prisma.tessera.fonts;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Glyph {

    private final String id;
    private final char character;
    private final String texture;
    private final int ascent;
    private final int height;
    private final int width;
    private final ConfigurationSection section;

    public Glyph(
            @NotNull String id,
            char character,
            @NotNull String texture,
            int ascent,
            int height,
            int width,
            @Nullable ConfigurationSection section
    ) {
        this.id = id;
        this.character = character;
        this.texture = texture;
        this.ascent = ascent;
        this.height = height;
        this.width = width;
        this.section = section;
    }

    public String getId() {
        return id;
    }

    public char getCharacter() {
        return character;
    }

    public String getTexture() {
        return texture;
    }

    public int getAscent() {
        return ascent;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    @Nullable
    public ConfigurationSection getSection() {
        return section;
    }

    @Override
    public String toString() {
        return String.valueOf(character);
    }
}
