package com.prisma.tessera.gui;

import com.prisma.tessera.fonts.FontManager;
import com.prisma.tessera.items.ItemBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TesseraGUI {

    private Component title = Component.text("Tessera Menu");
    private int rows = 3;
    private final Map<Integer, GUIButton> buttons = new HashMap<>();
    private Consumer<Player> openHandler;
    private Consumer<Player> closeHandler;
    private boolean cancelClicks = true;

    public TesseraGUI() {
    }

    public TesseraGUI(@NotNull Component title, int rows) {
        this.title = title;
        this.rows = Math.max(1, Math.min(6, rows));
    }

    public TesseraGUI(@NotNull String miniMessageTitle, int rows) {
        this.title = parseTitle(miniMessageTitle);
        this.rows = Math.max(1, Math.min(6, rows));
    }

    public static Component parseTitle(String miniMessageTitle) {
        String titleStr = miniMessageTitle;
        if (!titleStr.contains("<!italic>") && !titleStr.contains("<italic:false>") && !titleStr.contains("<italic>") && !titleStr.contains("<i>")) {
            titleStr = "<!italic>" + titleStr;
        }
        Component comp;
        if (FontManager.get() != null) {
            comp = FontManager.get().format(titleStr);
        } else {
            comp = MiniMessage.miniMessage().deserialize(titleStr);
        }
        return comp.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
    }

    public TesseraGUI title(@NotNull Component title) {
        this.title = title;
        return this;
    }

    public TesseraGUI title(@NotNull String miniMessageTitle) {
        this.title = parseTitle(miniMessageTitle);
        return this;
    }

    public TesseraGUI rows(int rows) {
        this.rows = Math.max(1, Math.min(6, rows));
        return this;
    }

    public int getRows() {
        return rows;
    }

    public int getSize() {
        return rows * 9;
    }

    public Component getTitle() {
        return title;
    }

    public boolean isCancelClicks() {
        return cancelClicks;
    }

    public TesseraGUI setCancelClicks(boolean cancelClicks) {
        this.cancelClicks = cancelClicks;
        return this;
    }

    public TesseraGUI setButton(int slot, @Nullable GUIButton button) {
        if (slot >= 0 && slot < getSize()) {
            if (button != null) {
                buttons.put(slot, button);
            } else {
                buttons.remove(slot);
            }
        }
        return this;
    }

    public TesseraGUI setButton(int row, int col, @Nullable GUIButton button) {
        int slot = (row * 9) + col;
        return setButton(slot, button);
    }

    @Nullable
    public GUIButton getButton(int slot) {
        return buttons.get(slot);
    }

    public TesseraGUI fill(@NotNull GUIButton button) {
        for (int i = 0; i < getSize(); i++) {
            if (!buttons.containsKey(i)) {
                buttons.put(i, button);
            }
        }
        return this;
    }

    public TesseraGUI fillBorder(@NotNull GUIButton button) {
        int totalSlots = getSize();
        for (int i = 0; i < 9; i++) {
            buttons.put(i, button);
        }
        for (int i = 9; i < totalSlots - 9; i += 9) {
            buttons.put(i, button);
            buttons.put(i + 8, button);
        }
        for (int i = totalSlots - 9; i < totalSlots; i++) {
            buttons.put(i, button);
        }
        return this;
    }

    public TesseraGUI fillRow(int row, @NotNull GUIButton button) {
        int start = row * 9;
        int end = Math.min(start + 9, getSize());
        for (int i = start; i < end; i++) {
            buttons.put(i, button);
        }
        return this;
    }

    public TesseraGUI applyBannerFrame() {
        ItemStack cyanPane = new ItemBuilder(Material.CYAN_STAINED_GLASS_PANE).setName("<gray> </gray>").build();
        ItemStack bluePane = new ItemBuilder(Material.BLUE_STAINED_GLASS_PANE).setName("<gray> </gray>").build();
        int total = getSize();
        for (int i = 0; i < 9; i++) {
            if (!buttons.containsKey(i)) {
                buttons.put(i, GUIButton.of(i % 2 == 0 ? cyanPane : bluePane));
            }
        }
        for (int i = total - 9; i < total; i++) {
            if (!buttons.containsKey(i)) {
                buttons.put(i, GUIButton.of(i % 2 == 0 ? bluePane : cyanPane));
            }
        }
        return this;
    }

    public TesseraGUI onOpen(@Nullable Consumer<Player> handler) {
        this.openHandler = handler;
        return this;
    }

    public TesseraGUI onClose(@Nullable Consumer<Player> handler) {
        this.closeHandler = handler;
        return this;
    }

    public void handleOpen(@NotNull Player player) {
        if (openHandler != null) {
            openHandler.accept(player);
        }
    }

    public void handleClose(@NotNull Player player) {
        if (closeHandler != null) {
            closeHandler.accept(player);
        }
    }

    @NotNull
    public Inventory buildInventory() {
        TesseraInventory holder = new TesseraInventory(this);
        Inventory inventory = Bukkit.createInventory(holder, getSize(), title);
        holder.setInventory(inventory);

        for (Map.Entry<Integer, GUIButton> entry : buttons.entrySet()) {
            if (entry.getKey() < getSize()) {
                inventory.setItem(entry.getKey(), entry.getValue().getIcon());
            }
        }
        return inventory;
    }

    public void open(@NotNull Player player) {
        Inventory inventory = buildInventory();
        player.openInventory(inventory);
        handleOpen(player);
    }

    public void refresh(@NotNull Player player) {
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof TesseraInventory tesseraInv) {
            if (tesseraInv.getGui() == this) {
                Inventory inv = tesseraInv.getInventory();
                inv.clear();
                for (Map.Entry<Integer, GUIButton> entry : buttons.entrySet()) {
                    if (entry.getKey() < getSize()) {
                        inv.setItem(entry.getKey(), entry.getValue().getIcon());
                    }
                }
            }
        }
    }
}
