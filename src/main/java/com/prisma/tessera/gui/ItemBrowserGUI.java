package com.prisma.tessera.gui;

import com.prisma.tessera.TesseraPlugin;
import com.prisma.tessera.api.Tessera;
import com.prisma.tessera.blocks.BlockRegistry;
import com.prisma.tessera.blocks.BlockTemplate;
import com.prisma.tessera.furniture.FurnitureManager;
import com.prisma.tessera.furniture.FurnitureType;
import com.prisma.tessera.items.ItemBuilder;
import com.prisma.tessera.items.ItemTemplate;
import com.prisma.tessera.items.ItemUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class ItemBrowserGUI {

    public enum Category {
        ALL("All Custom Objects", Material.COMPASS),
        ITEMS("Items", Material.DIAMOND_SWORD),
        BLOCKS("Custom Blocks", Material.BRICKS),
        FURNITURE("Furniture", Material.ARMOR_STAND);

        private final String displayName;
        private final Material icon;

        Category(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getIcon() {
            return icon;
        }
    }

    private static final int PAGE_SIZE = 36;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void open(Player player) {
        open(player, Category.ALL, 0, null);
    }

    public static void open(Player player, Category category, int page) {
        open(player, category, page, null);
    }

    public static void open(Player player, Category category, int page, String searchQuery) {
        List<ItemStack> items = getCategoryItems(category, searchQuery);
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / PAGE_SIZE));
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));

        String title = "<white>:tessera_badge:</white> <gradient:#00c6ff:#0072ff><bold>Tessera Catalog</bold></gradient> <dark_gray>»</dark_gray> <gray>" +
                category.getDisplayName() + (searchQuery != null ? " <yellow>[\"" + searchQuery + "\"]</yellow>" : "") + "</gray>";

        TesseraGUI gui = new TesseraGUI(title, 6);

        ItemStack borderCyan = new ItemBuilder(Material.CYAN_STAINED_GLASS_PANE).setName("<gray> </gray>").build();
        ItemStack borderBlue = new ItemBuilder(Material.BLUE_STAINED_GLASS_PANE).setName("<gray> </gray>").build();
        for (int i = 36; i < 45; i++) {
            gui.setButton(i, GUIButton.of(i % 2 == 0 ? borderCyan : borderBlue));
        }

        ItemStack bannerItem = new ItemBuilder(Material.CYAN_BANNER)
                .setName("<white>:tessera_banner:</white>")
                .setLore(List.of(
                        "<gradient:#00c6ff:#0072ff><bold>✦ TESSERA CATALOG BANNER ✦</bold></gradient>",
                        "<gray>Active Category: <yellow>" + category.getDisplayName() + "</yellow></gray>",
                        "<gray>Page: <yellow>" + (currentPage + 1) + "</yellow> of <yellow>" + totalPages + "</yellow></gray>",
                        "<dark_gray>────────────────────────────────────</dark_gray>",
                        "<aqua>Left-Click Item:</aqua> <gray>Give 1x</gray>",
                        "<aqua>Right-Click Item:</aqua> <gray>Give Full Stack</gray>",
                        "<aqua>Shift-Click Item:</aqua> <gray>Give 16x</gray>",
                        "<aqua>Middle-Click Item:</aqua> <gray>Inspect Crafting Recipe</gray>",
                        "<dark_gray>────────────────────────────────────</dark_gray>",
                        "<gold>▶ Click here to open Main Hub ✦</gold>"
                ))
                .build();
        gui.setButton(40, GUIButton.of(bannerItem, p -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            MainMenuGUI.open(p);
        }));

        int startIndex = currentPage * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, items.size());

        for (int i = startIndex; i < endIndex; i++) {
            ItemStack stack = items.get(i);
            int slot = i - startIndex;

            gui.setButton(slot, GUIButton.of(stack, (p, click) -> {
                if (!p.hasPermission("tessera.admin")) {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    p.sendMessage(MM.deserialize("<red>You don't have permission to take custom items.</red>"));
                    return;
                }

                if (click.getClick() == org.bukkit.event.inventory.ClickType.MIDDLE) {
                    String id = ItemUtils.getTesseraId(stack);
                    if (id != null) {
                        RecipeGUI.open(p, id);
                        return;
                    }
                }

                ItemStack giveStack = stack.clone();
                int amount;
                if (click.isRightClick()) {
                    amount = giveStack.getMaxStackSize();
                } else if (click.isShiftClick()) {
                    amount = 16;
                } else {
                    amount = 1;
                }

                giveStack.setAmount(amount);
                p.getInventory().addItem(giveStack);
                p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.5f);
                p.sendMessage(MM.deserialize("<green>Received <yellow>" + amount + "x</yellow> <white>" +
                        (giveStack.getItemMeta() != null && giveStack.getItemMeta().hasDisplayName() ?
                                MM.serialize(giveStack.getItemMeta().displayName()) : giveStack.getType().name()) + "</white>!</green>"));
            }));
        }

        if (currentPage > 0) {
            ItemStack prev = new ItemBuilder(Material.ARROW)
                    .setName("<yellow>« Previous Page <gray>(" + currentPage + "/" + totalPages + ")</gray>")
                    .build();
            gui.setButton(45, GUIButton.of(prev, p -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                open(p, category, currentPage - 1, searchQuery);
            }));
        } else {
            gui.setButton(45, GUIButton.of(new ItemBuilder(Material.BARRIER).setName("<dark_gray>« No Previous Page").build()));
        }

        ItemStack searchBtn = new ItemBuilder(Material.SPYGLASS)
                .setName("<aqua><bold>Search Filter</bold>")
                .setLore(List.of(
                        searchQuery != null ? "<gray>Current filter: <yellow>\"" + searchQuery + "\"</yellow>" : "<gray>No active search filter",
                        "<dark_gray>Tip: Use command <click:suggest_command:'/tessera give '>/tessera give</click> for direct lookups"
                ))
                .build();
        gui.setButton(46, GUIButton.of(searchBtn));

        ItemStack hubBtn = new ItemBuilder(Material.NETHER_STAR)
                .setName("<gradient:#00c6ff:#0072ff><bold>✦ Return to Main Hub</bold></gradient>")
                .setLore(List.of(
                        "<gray>Go back to main navigation,</gray>",
                        "<gray>category tiles, and server overview.</gray>"
                ))
                .build();
        gui.setButton(47, GUIButton.of(hubBtn, p -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            MainMenuGUI.open(p);
        }));

        Category nextCategory = Category.values()[(category.ordinal() + 1) % Category.values().length];
        ItemStack catItem = new ItemBuilder(category.getIcon())
                .setName("<gold><bold>Category:</bold> <yellow>" + category.getDisplayName())
                .setLore(List.of(
                        "<gray>Click to cycle category to:",
                        "<aqua>» " + nextCategory.getDisplayName(),
                        "",
                        "<dark_gray>Left-Click: Next Category"
                ))
                .build();
        gui.setButton(48, GUIButton.of(catItem, p -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.5f);
            open(p, nextCategory, 0, searchQuery);
        }));

        TesseraPlugin plugin = TesseraPlugin.get();
        ItemStack indicator = new ItemBuilder(Material.COMPASS)
                .setName("<gold><bold>Page " + (currentPage + 1) + " of " + totalPages + "</bold>")
                .setLore(List.of(
                        "<gray>Total entries: <white>" + items.size() + "</white>",
                        "<gray>Items registered: <aqua>" + plugin.getItemRegistry().getAll().size() + "</aqua>",
                        "<gray>Blocks registered: <aqua>" + plugin.getBlockRegistry().getAll().size() + "</aqua>",
                        "<gray>Furniture registered: <aqua>" + plugin.getFurnitureManager().getAll().size() + "</aqua>",
                        "",
                        "<dark_gray>Left-Click item: Take 1",
                        "<dark_gray>Right-Click item: Take Stack",
                        "<dark_gray>Shift-Click item: Take 16"
                ))
                .build();
        gui.setButton(49, GUIButton.of(indicator));

        ItemStack settingsBtn = new ItemBuilder(Material.COMPARATOR)
                .setName("<aqua><bold>⚙ Player Preferences & HUD</bold></aqua>")
                .setLore(List.of(
                        "<gray>Toggle your status HUD bar,</gray>",
                        "<gray>top BossBar, and audio settings.</gray>"
                ))
                .build();
        gui.setButton(50, GUIButton.of(settingsBtn, p -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            SettingsGUI.open(p);
        }));

        boolean serverRunning = plugin.getPackServer() != null && plugin.getPackServer().isRunning();
        ItemStack packInfo = new ItemBuilder(serverRunning ? Material.EMERALD : Material.REDSTONE)
                .setName(serverRunning ? "<green><bold>Pack Server: ACTIVE</bold>" : "<red><bold>Pack Server: OFFLINE</bold>")
                .setLore(List.of(
                        "<gray>URL: <white>" + (plugin.getPackServer() != null ? plugin.getPackServer().getPackUrl() : "None") + "</white>",
                        "<gray>SHA-1: <white>" + (plugin.getPackServer() != null && plugin.getPackServer().getSha1Hex() != null ?
                                plugin.getPackServer().getSha1Hex().substring(0, Math.min(10, plugin.getPackServer().getSha1Hex().length())) + "..." : "None") + "</white>",
                        "",
                        "<yellow>Click to re-generate pack</yellow>"
                ))
                .build();
        gui.setButton(51, GUIButton.of(packInfo, p -> {
            if (p.hasPermission("tessera.admin")) {
                plugin.getPackGenerator().generate();
                p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 0.7f, 1.5f);
                p.sendMessage(MM.deserialize("<green>Resource pack regenerated and re-hashed!</green>"));
                open(p, category, currentPage, searchQuery);
            }
        }));

        if (player.hasPermission("tessera.admin")) {
            ItemStack adminBtn = new ItemBuilder(Material.BEACON)
                    .setName("<gradient:#ff416c:#ff4b2b><bold>👑 Admin Dashboard</bold></gradient>")
                    .setLore(List.of(
                            "<gray>Manage server pack, reload engines,</gray>",
                            "<gray>purge entities, run benchmarks.</gray>"
                    ))
                    .build();
            gui.setButton(52, GUIButton.of(adminBtn, p -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.5f);
                AdminDashboardGUI.open(p);
            }));
        }

        if (currentPage < totalPages - 1) {
            ItemStack next = new ItemBuilder(Material.ARROW)
                    .setName("<yellow>Next Page » <gray>(" + (currentPage + 2) + "/" + totalPages + ")</gray>")
                    .build();
            gui.setButton(53, GUIButton.of(next, p -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                open(p, category, currentPage + 1, searchQuery);
            }));
        } else {
            gui.setButton(53, GUIButton.of(new ItemBuilder(Material.BARRIER).setName("<dark_gray>No Next Page »").build()));
        }

        gui.open(player);
    }

    private static List<ItemStack> getCategoryItems(Category category, String query) {
        List<ItemStack> list = new ArrayList<>();
        TesseraPlugin plugin = TesseraPlugin.get();
        String filter = query != null ? query.toLowerCase(Locale.ROOT) : null;

        if (category == Category.ALL || category == Category.ITEMS) {
            for (ItemTemplate t : plugin.getItemRegistry().getAll()) {
                if (filter != null && !t.getId().toLowerCase(Locale.ROOT).contains(filter)) {
                    continue;
                }
                ItemStack item = t.build();
                if (item != null) {
                    list.add(item);
                }
            }
        }

        if (category == Category.ALL || category == Category.BLOCKS) {
            BlockRegistry blockRegistry = BlockRegistry.get();
            if (blockRegistry != null) {
                for (BlockTemplate t : blockRegistry.getAll()) {
                    if (filter != null && !t.getId().toLowerCase(Locale.ROOT).contains(filter)) {
                        continue;
                    }
                    ItemStack item = Tessera.getItem(t.getId());
                    if (item == null) {
                        item = new ItemBuilder(t.getMaterial())
                                .setTesseraId(t.getId())
                                .setName(t.getDisplayName())
                                .build();
                    }
                    list.add(item);
                }
            }
        }

        if (category == Category.ALL || category == Category.FURNITURE) {
            FurnitureManager furnitureManager = FurnitureManager.get();
            if (furnitureManager != null) {
                for (FurnitureType f : furnitureManager.getAll()) {
                    if (filter != null && !f.getId().toLowerCase(Locale.ROOT).contains(filter)) {
                        continue;
                    }
                    ItemStack item = Tessera.getItem(f.getItemId());
                    if (item == null) {
                        item = new ItemBuilder(Material.ARMOR_STAND)
                                .setTesseraId(f.getId())
                                .setName(f.getDisplayName())
                                .build();
                    }
                    list.add(item);
                }
            }
        }

        return list;
    }
}
