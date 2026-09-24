package com.prisma.tessera.furniture;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FurnitureType {

    private final String id;
    private final String itemId;
    private final String displayName;
    private final FurnitureDisplayType displayType;
    private final float hitboxWidth;
    private final float hitboxHeight;
    private final float scaleX;
    private final float scaleY;
    private final float scaleZ;
    private final float translationX;
    private final float translationY;
    private final float translationZ;
    private final boolean seat;
    private final double seatHeight;
    private final String placeSound;
    private final String breakSound;
    private final String dropItemId;
    private final boolean cancelDrop;
    private final boolean cardinalRotation;
    private final ConfigurationSection section;

    public FurnitureType(
            @NotNull String id,
            @NotNull String itemId,
            @Nullable String displayName,
            @NotNull FurnitureDisplayType displayType,
            float hitboxWidth,
            float hitboxHeight,
            float scaleX,
            float scaleY,
            float scaleZ,
            float translationX,
            float translationY,
            float translationZ,
            boolean seat,
            double seatHeight,
            @Nullable String placeSound,
            @Nullable String breakSound,
            @Nullable String dropItemId,
            boolean cancelDrop,
            boolean cardinalRotation,
            @Nullable ConfigurationSection section
    ) {
        this.id = id;
        this.itemId = itemId;
        this.displayName = displayName != null ? displayName : id;
        this.displayType = displayType;
        this.hitboxWidth = Math.max(0.1f, hitboxWidth);
        this.hitboxHeight = Math.max(0.1f, hitboxHeight);
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
        this.translationX = translationX;
        this.translationY = translationY;
        this.translationZ = translationZ;
        this.seat = seat;
        this.seatHeight = seatHeight;
        this.placeSound = placeSound;
        this.breakSound = breakSound;
        this.dropItemId = dropItemId != null ? dropItemId : itemId;
        this.cancelDrop = cancelDrop;
        this.cardinalRotation = cardinalRotation;
        this.section = section;
    }

    public String getId() {
        return id;
    }

    public String getItemId() {
        return itemId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public FurnitureDisplayType getDisplayType() {
        return displayType;
    }

    public float getHitboxWidth() {
        return hitboxWidth;
    }

    public float getHitboxHeight() {
        return hitboxHeight;
    }

    public float getScaleX() {
        return scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    public float getScaleZ() {
        return scaleZ;
    }

    public float getTranslationX() {
        return translationX;
    }

    public float getTranslationY() {
        return translationY;
    }

    public float getTranslationZ() {
        return translationZ;
    }

    public boolean isSeat() {
        return seat;
    }

    public double getSeatHeight() {
        return seatHeight;
    }

    @Nullable
    public String getPlaceSound() {
        return placeSound;
    }

    @Nullable
    public String getBreakSound() {
        return breakSound;
    }

    public String getDropItemId() {
        return dropItemId;
    }

    public boolean isCancelDrop() {
        return cancelDrop;
    }

    public boolean isCardinalRotation() {
        return cardinalRotation;
    }

    @Nullable
    public ConfigurationSection getSection() {
        return section;
    }
}
