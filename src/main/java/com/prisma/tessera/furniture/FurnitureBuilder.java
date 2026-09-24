package com.prisma.tessera.furniture;

import org.bukkit.configuration.ConfigurationSection;

public final class FurnitureBuilder {

    private final String id;
    private String itemId;
    private String displayName;
    private FurnitureDisplayType displayType = FurnitureDisplayType.ITEM_DISPLAY;
    private float hitboxWidth = 1.0f;
    private float hitboxHeight = 1.0f;
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float scaleZ = 1.0f;
    private float translationX = 0.0f;
    private float translationY = 0.0f;
    private float translationZ = 0.0f;
    private boolean seat = false;
    private double seatHeight = 0.5;
    private String placeSound = "BLOCK_WOOD_PLACE";
    private String breakSound = "BLOCK_WOOD_BREAK";
    private String dropItemId;
    private boolean cancelDrop = false;
    private boolean cardinalRotation = true;
    private ConfigurationSection section;

    public FurnitureBuilder(String id) {
        this.id = id;
        this.itemId = id;
    }

    public FurnitureBuilder setItemId(String itemId) {
        this.itemId = itemId;
        return this;
    }

    public FurnitureBuilder setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public FurnitureBuilder setDisplayType(FurnitureDisplayType displayType) {
        this.displayType = displayType;
        return this;
    }

    public FurnitureBuilder setHitbox(float width, float height) {
        this.hitboxWidth = width;
        this.hitboxHeight = height;
        return this;
    }

    public FurnitureBuilder setScale(float x, float y, float z) {
        this.scaleX = x;
        this.scaleY = y;
        this.scaleZ = z;
        return this;
    }

    public FurnitureBuilder setTranslation(float x, float y, float z) {
        this.translationX = x;
        this.translationY = y;
        this.translationZ = z;
        return this;
    }

    public FurnitureBuilder setSeat(boolean seat, double seatHeight) {
        this.seat = seat;
        this.seatHeight = seatHeight;
        return this;
    }

    public FurnitureBuilder setSounds(String placeSound, String breakSound) {
        this.placeSound = placeSound;
        this.breakSound = breakSound;
        return this;
    }

    public FurnitureBuilder setDropItemId(String dropItemId) {
        this.dropItemId = dropItemId;
        return this;
    }

    public FurnitureBuilder setCancelDrop(boolean cancelDrop) {
        this.cancelDrop = cancelDrop;
        return this;
    }

    public FurnitureBuilder setCardinalRotation(boolean cardinalRotation) {
        this.cardinalRotation = cardinalRotation;
        return this;
    }

    public FurnitureBuilder setSection(ConfigurationSection section) {
        this.section = section;
        return this;
    }

    public FurnitureType build() {
        return new FurnitureType(
                id,
                itemId != null ? itemId : id,
                displayName != null ? displayName : id,
                displayType,
                hitboxWidth,
                hitboxHeight,
                scaleX,
                scaleY,
                scaleZ,
                translationX,
                translationY,
                translationZ,
                seat,
                seatHeight,
                placeSound,
                breakSound,
                dropItemId,
                cancelDrop,
                cardinalRotation,
                section
        );
    }
}
