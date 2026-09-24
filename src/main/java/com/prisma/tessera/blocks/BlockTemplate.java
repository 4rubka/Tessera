package com.prisma.tessera.blocks;

import com.prisma.tessera.items.ItemBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BlockTemplate {

    private static final Random RANDOM = new Random();

    private final String id;
    private final Material material;
    private final String displayName;
    private final float hardness;
    private final String requiredTool;
    private final String minimumTier;
    private final String placeSound;
    private final String breakSound;
    private final String stepSound;
    private final String hitSound;
    private final int lightLevel;
    private final boolean cancelDrop;
    private final int customModelData;
    private final List<BlockDrop> drops;
    private final ConfigurationSection section;

    public BlockTemplate(
            @NotNull String id,
            @NotNull Material material,
            @Nullable String displayName,
            float hardness,
            @Nullable String requiredTool,
            @Nullable String minimumTier,
            @Nullable String placeSound,
            @Nullable String breakSound,
            @Nullable String stepSound,
            @Nullable String hitSound,
            int lightLevel,
            boolean cancelDrop,
            int customModelData,
            @NotNull List<BlockDrop> drops,
            @Nullable ConfigurationSection section
    ) {
        this.id = id;
        this.material = material;
        this.displayName = displayName != null ? displayName : id;
        this.hardness = hardness;
        this.requiredTool = requiredTool != null ? requiredTool.toUpperCase() : "ANY";
        this.minimumTier = minimumTier != null ? minimumTier.toUpperCase() : "NONE";
        this.placeSound = placeSound;
        this.breakSound = breakSound;
        this.stepSound = stepSound;
        this.hitSound = hitSound;
        this.lightLevel = Math.max(0, Math.min(15, lightLevel));
        this.cancelDrop = cancelDrop;
        this.customModelData = customModelData;
        this.drops = drops;
        this.section = section;
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public float getHardness() {
        return hardness;
    }

    public String getRequiredTool() {
        return requiredTool;
    }

    public String getMinimumTier() {
        return minimumTier;
    }

    @Nullable
    public String getPlaceSound() {
        return placeSound;
    }

    @Nullable
    public String getBreakSound() {
        return breakSound;
    }

    @Nullable
    public String getStepSound() {
        return stepSound;
    }

    @Nullable
    public String getHitSound() {
        return hitSound;
    }

    public int getLightLevel() {
        return lightLevel;
    }

    public boolean isCancelDrop() {
        return cancelDrop;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public List<BlockDrop> getDrops() {
        return Collections.unmodifiableList(drops);
    }

    @Nullable
    public ConfigurationSection getSection() {
        return section;
    }

    public boolean isToolAppropriate(@Nullable ItemStack tool) {
        if ("ANY".equals(requiredTool) || "NONE".equals(requiredTool)) {
            return true;
        }
        if (tool == null || tool.getType() == Material.AIR) {
            return false;
        }

        String toolName = tool.getType().name();
        boolean matchesType = switch (requiredTool) {
            case "PICKAXE" -> toolName.endsWith("_PICKAXE");
            case "AXE" -> toolName.endsWith("_AXE") && !toolName.endsWith("_PICKAXE");
            case "SHOVEL" -> toolName.endsWith("_SHOVEL");
            case "HOE" -> toolName.endsWith("_HOE");
            case "SWORD" -> toolName.endsWith("_SWORD");
            case "SHEARS" -> toolName.equals("SHEARS");
            default -> toolName.contains(requiredTool);
        };

        if (!matchesType) {
            return false;
        }

        if ("NONE".equals(minimumTier)) {
            return true;
        }

        int requiredLevel = getTierLevel(minimumTier);
        int toolLevel = getToolTierLevel(toolName);
        return toolLevel >= requiredLevel;
    }

    private int getTierLevel(String tier) {
        return switch (tier) {
            case "WOOD", "WOODEN", "GOLD", "GOLDEN" -> 1;
            case "STONE" -> 2;
            case "IRON" -> 3;
            case "DIAMOND" -> 4;
            case "NETHERITE" -> 5;
            default -> 0;
        };
    }

    private int getToolTierLevel(String toolName) {
        if (toolName.startsWith("NETHERITE_")) return 5;
        if (toolName.startsWith("DIAMOND_")) return 4;
        if (toolName.startsWith("IRON_")) return 3;
        if (toolName.startsWith("STONE_")) return 2;
        if (toolName.startsWith("GOLDEN_") || toolName.startsWith("WOODEN_")) return 1;
        return 0;
    }

    public List<ItemStack> computeDrops(@Nullable ItemStack tool) {
        if (cancelDrop) {
            return Collections.emptyList();
        }

        if (!isToolAppropriate(tool) && !"ANY".equals(requiredTool) && !"NONE".equals(requiredTool)) {
            return Collections.emptyList();
        }

        List<ItemStack> result = new ArrayList<>();
        for (BlockDrop drop : drops) {
            if (RANDOM.nextDouble() <= drop.chance()) {
                int amount = drop.minAmount();
                if (drop.maxAmount() > drop.minAmount()) {
                    amount += RANDOM.nextInt(drop.maxAmount() - drop.minAmount() + 1);
                }
                if (amount > 0) {
                    ItemStack stack = drop.createItemStack(amount);
                    if (stack != null) {
                        result.add(stack);
                    }
                }
            }
        }
        return result;
    }

    public record BlockDrop(
            @NotNull String itemId,
            int minAmount,
            int maxAmount,
            double chance
    ) {
        @Nullable
        public ItemStack createItemStack(int amount) {
            Material vanilla = Material.matchMaterial(itemId);
            if (vanilla != null) {
                return new ItemStack(vanilla, amount);
            }
            ItemStack customItem = com.prisma.tessera.api.Tessera.getItem(itemId);
            if (customItem != null) {
                customItem.setAmount(amount);
                return customItem;
            }
            return null;
        }
    }
}
