package com.prisma.tessera.commands;

import com.prisma.tessera.TesseraPlugin;
import com.prisma.tessera.api.Tessera;
import com.prisma.tessera.gui.AdminDashboardGUI;
import com.prisma.tessera.gui.ItemBrowserGUI;
import com.prisma.tessera.gui.MainMenuGUI;
import com.prisma.tessera.gui.RecipeGUI;
import com.prisma.tessera.gui.SettingsGUI;
import com.prisma.tessera.items.ItemBuilder;
import com.prisma.tessera.items.ItemTemplate;
import com.prisma.tessera.utils.BlockTool;
import com.prisma.tessera.utils.DebugBenchmark;
import com.prisma.tessera.utils.FurnitureTool;
import com.prisma.tessera.utils.HatManager;
import com.prisma.tessera.utils.ItemInspector;
import com.prisma.tessera.utils.RepairManager;
import com.prisma.tessera.utils.SeatManager;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TesseraCommand implements CommandExecutor, TabCompleter {

    private final TesseraPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public TesseraCommand(TesseraPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHeader(sender);
            sender.sendMessage(mm.deserialize("<gray>Type <aqua>/" + label + " help</aqua> to view all commands, or <aqua>/" + label + " menu</aqua> to open the hub.</gray>"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> handleHelp(sender, label, args);
            case "menu", "gui" -> handleMenu(sender, args);
            case "browse", "catalog" -> handleBrowse(sender);
            case "admin" -> handleAdmin(sender);
            case "settings" -> handleSettings(sender);
            case "get" -> handleGet(sender, label, args);
            case "give" -> handleGive(sender, label, args);
            case "giveall" -> handleGiveAll(sender, label, args);
            case "drop" -> handleDrop(sender, label, args);
            case "durability" -> handleDurability(sender, label, args);
            case "custommodeldata", "cmd" -> handleCustomModelData(sender, label, args);
            case "nbt", "pdc" -> handleNbt(sender);
            case "killfurniture" -> handleKillFurniture(sender, label, args);
            case "cleartemp" -> handleClearTemp(sender);
            case "sound", "playsound" -> handleSound(sender, label, args);
            case "stats" -> handleStats(sender);
            case "emote" -> handleEmote(sender, label, args);
            case "export" -> handleExport(sender);
            case "recipe" -> handleRecipe(sender, label, args);
            case "inspect" -> handleInspect(sender);
            case "search" -> handleSearch(sender, label, args);
            case "hat", "wear" -> handleHat(sender);
            case "sit" -> handleSit(sender);
            case "repair" -> handleRepair(sender, label, args);
            case "block" -> handleBlock(sender, label, args);
            case "furniture" -> handleFurniture(sender, label, args);
            case "debug" -> handleDebug(sender, label, args);
            case "hud" -> handleHud(sender, label, args);
            case "pack" -> handlePack(sender, label, args);
            case "welcome" -> handleWelcome(sender);
            case "reload" -> handleReload(sender);
            case "info" -> handleInfo(sender);
            default -> {
                sender.sendMessage(mm.deserialize("<red>Unknown subcommand: <yellow>" + args[0] + "</yellow>. Type <aqua>/" + label + " help</aqua> for assistance.</red>"));
            }
        }
        return true;
    }

    private void sendHeader(CommandSender sender) {
        sender.sendMessage(mm.deserialize("<gradient:#00c6ff:#0072ff><bold>Tessera</bold></gradient> <gray>v" + plugin.getDescription().getVersion() + "</gray>"));
    }

    private void handleHelp(CommandSender sender, String label, String[] args) {
        int page = 1;
        if (args.length > 1) {
            try {
                page = Math.max(1, Math.min(3, Integer.parseInt(args[1])));
            } catch (NumberFormatException ignored) {
            }
        }

        int totalPages = 3;
        sender.sendMessage(mm.deserialize("<gradient:#00c6ff:#0072ff><bold>════════ Tessera Commands (" + page + "/" + totalPages + ") ════════</bold></gradient>"));

        if (page == 1) {
            sendHelpLine(sender, label, "menu", "Open main navigation hub with categories & banners");
            sendHelpLine(sender, label, "browse [category]", "Open multi-page custom item and block browser");
            sendHelpLine(sender, label, "search <query>", "Instant search custom objects in catalog");
            sendHelpLine(sender, label, "recipe <id>", "Open interactive 3x3 crafting workbench viewer");
            sendHelpLine(sender, label, "settings", "Player preferences GUI (HUD, Banner, Pack)");
            sendHelpLine(sender, label, "hat", "Wear held custom 3D model or block on your head");
            sendHelpLine(sender, label, "sit", "Sit on target block, chair, or custom furniture");
            sendHelpLine(sender, label, "emote <name>", "Play cosmetic particle & sound animation");
        } else if (page == 2) {
            sendHelpLine(sender, label, "get <id> [amount]", "Instantly obtain custom item or block");
            sendHelpLine(sender, label, "give <player> <id> [amount]", "Give custom object to a specific player");
            sendHelpLine(sender, label, "giveall <id> [amount]", "Broadcast custom object to all online players");
            sendHelpLine(sender, label, "drop <id> [amount] [coords]", "Spawn dropped custom item at location");
            sendHelpLine(sender, label, "inspect", "Interactive inspection of held item PDC & DataComponents");
            sendHelpLine(sender, label, "durability <get|set|add>", "Get or adjust custom item durability");
            sendHelpLine(sender, label, "cmd <get|set [value]>", "Inspect or edit CustomModelData of held item");
            sendHelpLine(sender, label, "nbt", "Dump all PersistentDataContainer keys & values");
            sendHelpLine(sender, label, "repair [hand|all]", "Fully restore custom item durability");
        } else {
            sendHelpLine(sender, label, "admin", "Open dedicated Admin Control Center GUI");
            sendHelpLine(sender, label, "stats", "Detailed runtime performance, GC & object telemetry");
            sendHelpLine(sender, label, "sound <name> [vol] [pitch]", "Test/play custom sound at player location");
            sendHelpLine(sender, label, "killfurniture [radius]", "Purge orphaned or corrupted furniture entities");
            sendHelpLine(sender, label, "cleartemp", "Purge empty temporary seat armor stands server-wide");
            sendHelpLine(sender, label, "block <info|remove>", "Inspect or safely clear custom block in crosshairs");
            sendHelpLine(sender, label, "furniture <list|remove> [r]", "Highlight or remove furniture display entities");
            sendHelpLine(sender, label, "pack <generate|apply|push|import>", "Resource pack deployment & merge engine");
            sendHelpLine(sender, label, "reload", "Hot-reload all YAML configurations & modules");
        }

        sender.sendMessage(mm.deserialize("<gray>Use <aqua>/" + label + " help <1-3></aqua> to navigate command pages.</gray>"));
        sender.sendMessage(mm.deserialize("<gradient:#00c6ff:#0072ff><bold>═══════════════════════════════════════</bold></gradient>"));
    }

    private void sendHelpLine(CommandSender sender, String label, String cmd, String desc) {
        Component comp = mm.deserialize("<dark_gray> » </dark_gray><aqua><bold>/" + label + " " + cmd + "</bold></aqua> <gray>- " + desc + "</gray>")
                .clickEvent(ClickEvent.suggestCommand("/" + label + " " + cmd.split(" ")[0]))
                .hoverEvent(HoverEvent.showText(mm.deserialize("<green>Click to suggest: /" + label + " " + cmd.split(" ")[0] + "</green>")));
        sender.sendMessage(comp);
    }

    private void handleMenu(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can open the menu.</red>"));
            return;
        }
        if (args.length == 1) {
            MainMenuGUI.open(player);
            return;
        }
        if (args[1].equalsIgnoreCase("browse") || args[1].equalsIgnoreCase("catalog")) {
            ItemBrowserGUI.open(player, ItemBrowserGUI.Category.ALL, 0, null);
            return;
        }
        ItemBrowserGUI.Category cat = ItemBrowserGUI.Category.ALL;
        String query = null;
        try {
            cat = ItemBrowserGUI.Category.valueOf(args[1].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            query = args[1];
        }
        if (args.length > 2) {
            query = args[2];
        }
        ItemBrowserGUI.open(player, cat, 0, query);
    }

    private void handleBrowse(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can browse catalog.</red>"));
            return;
        }
        ItemBrowserGUI.open(player, ItemBrowserGUI.Category.ALL, 0, null);
    }

    private void handleAdmin(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can open admin dashboard.</red>"));
            return;
        }
        AdminDashboardGUI.open(player);
    }

    private void handleSettings(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can open settings.</red>"));
            return;
        }
        SettingsGUI.open(player);
    }

    private void handleGet(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can use /" + label + " get.</red>"));
            return;
        }
        if (!sender.hasPermission("tessera.get") && !sender.hasPermission("tessera.admin")) {
            sender.sendMessage(mm.deserialize("<red>No permission.</red>"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(mm.deserialize("<red>Usage: /" + label + " get <id> [amount]</red>"));
            return;
        }

        String id = args[1];
        ItemStack item = resolveItemOrBlock(id);
        if (item == null) {
            sender.sendMessage(mm.deserialize("<red>Custom object not found: <yellow>" + id + "</yellow></red>"));
            return;
        }

        int amount = 1;
        if (args.length > 2) {
            try {
                amount = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException ignored) {
            }
        }
        item.setAmount(amount);
        player.getInventory().addItem(item);
        player.playSound(player.getLocation(), "entity.item.pickup", 1.0f, 1.2f);
        sender.sendMessage(mm.deserialize("<green>Obtained <yellow>" + amount + "x " + id + "</yellow>.</green>"));
    }

    private void handleGive(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("tessera.give") && !sender.hasPermission("tessera.admin")) {
            sender.sendMessage(mm.deserialize("<red>No permission.</red>"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(mm.deserialize("<red>Usage: /" + label + " give <player> <id> [amount]</red>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(mm.deserialize("<red>Player not found: " + args[1] + "</red>"));
            return;
        }
        String id = args[2];
        ItemStack item = resolveItemOrBlock(id);
        if (item == null) {
            sender.sendMessage(mm.deserialize("<red>Custom object not found: " + id + "</red>"));
            return;
        }
        int amount = 1;
        if (args.length > 3) {
            try {
                amount = Math.max(1, Integer.parseInt(args[3]));
            } catch (NumberFormatException ignored) {
            }
        }
        item.setAmount(amount);
        target.getInventory().addItem(item);
        target.playSound(target.getLocation(), "entity.item.pickup", 1.0f, 1.2f);
        sender.sendMessage(mm.deserialize("<green>Gave <yellow>" + amount + "x " + id + "</yellow> to <aqua>" + target.getName() + "</aqua>.</green>"));
    }

    private void handleGiveAll(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("tessera.giveall") && !sender.hasPermission("tessera.admin")) {
            sender.sendMessage(mm.deserialize("<red>No permission.</red>"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(mm.deserialize("<red>Usage: /" + label + " giveall <id> [amount]</red>"));
            return;
        }
        String id = args[1];
        ItemStack item = resolveItemOrBlock(id);
        if (item == null) {
            sender.sendMessage(mm.deserialize("<red>Custom object not found: " + id + "</red>"));
            return;
        }
        int amount = 1;
        if (args.length > 2) {
            try {
                amount = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException ignored) {
            }
        }
        item.setAmount(amount);

        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.getInventory().addItem(item.clone());
            p.playSound(p.getLocation(), "entity.player.levelup", 0.7f, 1.5f);
            p.sendMessage(mm.deserialize("<gradient:#00c6ff:#0072ff>✦ Server Event:</gradient> <gray>You received <yellow>" + amount + "x " + id + "</yellow>!</gray>"));
            count++;
        }
        sender.sendMessage(mm.deserialize("<green>Broadcasted <yellow>" + amount + "x " + id + "</yellow> to <aqua>" + count + "</aqua> online players!</green>"));
    }

    private void handleRecipe(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can view crafting recipes in GUI.</red>"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(mm.deserialize("<red>Usage: /" + label + " recipe <id></red>"));
            return;
        }
        RecipeGUI.open(player, args[1]);
    }

    private void handleInspect(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can inspect held items.</red>"));
            return;
        }
        ItemInspector.inspect(player, player.getInventory().getItemInMainHand());
    }

    private void handleSearch(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(mm.deserialize("<red>Usage: /" + label + " search <query></red>"));
            return;
        }
        String query = args[1].toLowerCase(Locale.ROOT);
        List<String> matched = new ArrayList<>();
        for (String id : plugin.getItemRegistry().getIds()) {
            if (id.toLowerCase(Locale.ROOT).contains(query)) matched.add(id);
        }
        for (String id : plugin.getBlockRegistry().getIds()) {
            if (id.toLowerCase(Locale.ROOT).contains(query) && !matched.contains(id)) matched.add(id);
        }
        for (String id : plugin.getFurnitureManager().getIds()) {
            if (id.toLowerCase(Locale.ROOT).contains(query) && !matched.contains(id)) matched.add(id);
        }

        if (matched.isEmpty()) {
            sender.sendMessage(mm.deserialize("<yellow>No custom objects matched query: \"" + query + "\"</yellow>"));
            return;
        }

        sender.sendMessage(mm.deserialize("<gradient:#00c6ff:#0072ff><bold>Search Results for \"" + query + "\" (" + matched.size() + ")</bold></gradient>"));
        for (String id : matched) {
            Component line = mm.deserialize("<dark_gray> » </dark_gray><aqua><bold>" + id + "</bold></aqua> ")
                    .append(mm.deserialize("<green>[GET]</green> ")
                            .clickEvent(ClickEvent.runCommand("/" + label + " get " + id))
                            .hoverEvent(HoverEvent.showText(mm.deserialize("<green>Click to give " + id + "</green>"))))
                    .append(mm.deserialize("<yellow>[RECIPE]</yellow>")
                            .clickEvent(ClickEvent.runCommand("/" + label + " recipe " + id))
                            .hoverEvent(HoverEvent.showText(mm.deserialize("<yellow>Click to view recipe</yellow>"))));
            sender.sendMessage(line);
        }
    }

    private void handleHat(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can wear cosmetic hats.</red>"));
            return;
        }
        HatManager.toggleHat(player);
    }

    private void handleSit(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can use /tessera sit.</red>"));
            return;
        }
        Block target = player.getTargetBlockExact(5, FluidCollisionMode.NEVER);
        if (target == null || target.getType().isAir()) {
            sender.sendMessage(mm.deserialize("<red>Look at a solid block or chair within 5 blocks to sit down.</red>"));
            return;
        }
        SeatManager.get().sitOnBlock(player, target);
    }

    private void handleRepair(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can repair items.</red>"));
            return;
        }
        if (!sender.hasPermission("tessera.repair") && !sender.hasPermission("tessera.admin")) {
            sender.sendMessage(mm.deserialize("<red>No permission.</red>"));
            return;
        }
        boolean all = args.length > 1 && args[1].equalsIgnoreCase("all");
        if (all) {
            RepairManager.repairAll(player);
        } else {
            RepairManager.repairHand(player);
        }
    }

    private void handleBlock(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can use block tools.</red>"));
            return;
        }
        if (args.length < 2 || args[1].equalsIgnoreCase("info")) {
            BlockTool.inspectTarget(player);
        } else if (args[1].equalsIgnoreCase("remove")) {
            if (!sender.hasPermission("tessera.admin")) {
                sender.sendMessage(mm.deserialize("<red>No permission.</red>"));
                return;
            }
            BlockTool.removeTarget(player);
        } else {
            sender.sendMessage(mm.deserialize("<red>Usage: /" + label + " block <info|remove></red>"));
        }
    }

    private void handleFurniture(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can use furniture tools.</red>"));
            return;
        }
        double radius = 10.0;
        if (args.length > 2) {
            try {
                radius = Double.parseDouble(args[2]);
            } catch (NumberFormatException ignored) {
            }
        }

        if (args.length < 2 || args[1].equalsIgnoreCase("list")) {
            FurnitureTool.listNearby(player, radius);
        } else if (args[1].equalsIgnoreCase("remove")) {
            if (!sender.hasPermission("tessera.admin")) {
                sender.sendMessage(mm.deserialize("<red>No permission.</red>"));
                return;
            }
            FurnitureTool.removeNearby(player, radius);
        } else {
            sender.sendMessage(mm.deserialize("<red>Usage: /" + label + " furniture <list|remove> [radius]</red>"));
        }
    }

    private void handleDebug(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("tessera.admin")) {
            sender.sendMessage(mm.deserialize("<red>No permission.</red>"));
            return;
        }
        if (args.length > 1 && args[1].equalsIgnoreCase("entities")) {
            DebugBenchmark.reportEntities(sender);
        } else {
            DebugBenchmark.runBenchmark(sender);
        }
    }

    private void handleHud(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can toggle HUD.</red>"));
            return;
        }
        boolean enabled = plugin.getHudManager().toggleHud(player);
        if (enabled) {
            player.sendMessage(mm.deserialize("<green>Actionbar HUD enabled.</green>"));
        } else {
            player.sendMessage(mm.deserialize("<red>Actionbar HUD disabled.</red>"));
        }
    }

    private void handlePack(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("tessera.admin")) {
            sender.sendMessage(mm.deserialize("<red>No permission.</red>"));
            return;
        }

        if (args.length > 1) {
            String action = args[1].toLowerCase(Locale.ROOT);
            switch (action) {
                case "apply", "push" -> {
                    if (args.length > 2 && !args[2].equalsIgnoreCase("all")) {
                        Player target = Bukkit.getPlayer(args[2]);
                        if (target == null) {
                            sender.sendMessage(mm.deserialize("<red>Target player not found: " + args[2] + "</red>"));
                            return;
                        }
                        plugin.getPackListener().sendResourcePack(target);
                        sender.sendMessage(mm.deserialize("<green>Sent resource pack prompt to <aqua>" + target.getName() + "</aqua>.</green>"));
                    } else {
                        plugin.getPackListener().sendToAllOnline();
                        sender.sendMessage(mm.deserialize("<green>Pushed resource pack reload prompt to all online players!</green>"));
                    }
                    return;
                }
                case "import" -> {
                    if (args.length > 2) {
                        String target = args[2];
                        if (target.startsWith("http://") || target.startsWith("https://")) {
                            sender.sendMessage(mm.deserialize("<gradient:#00c6ff:#0072ff>Starting remote pack download & ingestion pipeline...</gradient>"));
                            plugin.getSmartPackImporter().importFromUrl(target, true, true, sender instanceof Player p ? p : null);
                            return;
                        }

                        File targetFile = new File(plugin.getSmartPackImporter().getImportsDir(), target);
                        if (!targetFile.exists() && !target.endsWith(".zip")) {
                            targetFile = new File(plugin.getSmartPackImporter().getImportsDir(), target + ".zip");
                        }
                        if (!targetFile.exists()) {
                            sender.sendMessage(mm.deserialize("<red>ZIP file not found in imports folder: <yellow>" + target + "</yellow></red>"));
                            sender.sendMessage(mm.deserialize("<gray>Drop your .zip pack into <aqua>plugins/Tessera/imports/</aqua> first.</gray>"));
                            return;
                        }

                        sender.sendMessage(mm.deserialize("<gradient:#00c6ff:#0072ff>Ingesting resource pack <yellow>" + targetFile.getName() + "</yellow>...</gradient>"));
                        final File finalTarget = targetFile;
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            var res = plugin.getSmartPackImporter().importZip(finalTarget, true, true);
                            sender.sendMessage(mm.deserialize("<gradient:#00c6ff:#0072ff><bold>════ Pack Ingestion Complete ════</bold></gradient>"));
                            sender.sendMessage(mm.deserialize("<gray>Textures: <green>" + res.texturesCount() + "</green> | Models: <aqua>" + res.modelsCount() + "</aqua></gray>"));
                            sender.sendMessage(mm.deserialize("<gray>Auto-Generated Items: <yellow>" + res.itemsGenerated() + "</yellow> | Blocks: <yellow>" + res.blocksGenerated() + "</yellow></gray>"));
                            sender.sendMessage(mm.deserialize("<gray>Elapsed: <gold>" + res.elapsedMillis() + "ms</gold> | SHA-1: <aqua>" + (res.sha1Hex() != null ? res.sha1Hex().substring(0, Math.min(10, res.sha1Hex().length())) + "..." : "N/A") + "</aqua></gray>"));
                            sender.sendMessage(mm.deserialize("<green>Pack merged, protected, and hosted on server! Pushed to players.</green>"));
                        });
                        return;
                    }

                    File[] zips = plugin.getSmartPackImporter().getImportsDir().listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".zip"));
                    if (zips == null || zips.length == 0) {
                        sender.sendMessage(mm.deserialize("<yellow>No .zip resource packs found in <aqua>plugins/Tessera/imports/</aqua>.</yellow>"));
                        sender.sendMessage(mm.deserialize("<gray>Drop your resource packs (.zip) into the <aqua>imports/</aqua> directory and run <yellow>/tessera pack import</yellow> again!</gray>"));
                        return;
                    }

                    sender.sendMessage(mm.deserialize("<gradient:#00c6ff:#0072ff>Found " + zips.length + " pack(s) in imports/. Starting batch ingestion...</gradient>"));
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        int totalTex = 0, totalMod = 0, totalItems = 0;
                        for (File zip : zips) {
                            var res = plugin.getSmartPackImporter().importZip(zip, true, true);
                            totalTex += res.texturesCount();
                            totalMod += res.modelsCount();
                            totalItems += res.itemsGenerated();
                        }
                        sender.sendMessage(mm.deserialize("<green>Successfully ingested " + zips.length + " pack(s)! (" + totalTex + " textures, " + totalMod + " models, " + totalItems + " items generated).</green>"));
                    });
                    return;
                }
            }
        }

        plugin.getPackGenerator().generate();
        String url = plugin.getPackServer() != null ? plugin.getPackServer().getPackUrl() : "None";
        String sha1 = plugin.getPackServer() != null && plugin.getPackServer().getSha1Hex() != null ? plugin.getPackServer().getSha1Hex() : "None";
        sender.sendMessage(mm.deserialize("<green>Resource pack regenerated, protected, and hosted!</green>"));
        sender.sendMessage(mm.deserialize("<gray>URL: <aqua>" + url + "</aqua></gray>"));
        sender.sendMessage(mm.deserialize("<gray>SHA-1: <yellow>" + sha1 + "</yellow></gray>"));
        sender.sendMessage(mm.deserialize("<gray>Use <yellow>/tessera pack import [file|all|url]</yellow> to merge external packs.</gray>"));
    }

    private void handleWelcome(CommandSender sender) {
        if (!sender.hasPermission("tessera.admin")) {
            sender.sendMessage(mm.deserialize("<red>No permission.</red>"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Must be a player to test welcome sequence.</red>"));
            return;
        }
        plugin.getWelcomeManager().handlePlayerJoin(player);
        sender.sendMessage(mm.deserialize("<green>Triggered welcome message sequence!</green>"));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("tessera.admin")) {
            sender.sendMessage(mm.deserialize("<red>No permission.</red>"));
            return;
        }
        plugin.getConfigManager().reload();
        plugin.reloadAllData();
        sender.sendMessage(mm.deserialize("<green>Tessera configurations and modules reloaded successfully!</green>"));
    }

    private void handleInfo(CommandSender sender) {
        sender.sendMessage(mm.deserialize("<gradient:#00c6ff:#0072ff><bold>Tessera Engine</bold></gradient> <gray>v" + plugin.getDescription().getVersion() + "</gray>"));
        sender.sendMessage(mm.deserialize("<gray>Items: <yellow>" + plugin.getItemRegistry().getAll().size() + "</yellow></gray>"));
        sender.sendMessage(mm.deserialize("<gray>Blocks: <yellow>" + plugin.getBlockRegistry().getAll().size() + "</yellow></gray>"));
        sender.sendMessage(mm.deserialize("<gray>Furniture: <yellow>" + plugin.getFurnitureManager().getAll().size() + "</yellow></gray>"));
        sender.sendMessage(mm.deserialize("<gray>Recipes: <yellow>" + plugin.getRecipeManager().getAll().size() + "</yellow></gray>"));
        sender.sendMessage(mm.deserialize("<gray>Glyphs: <yellow>" + plugin.getGlyphRegistry().getAll().size() + "</yellow></gray>"));
        sender.sendMessage(mm.deserialize("<gray>HUDs: <yellow>" + plugin.getHudManager().getAll().size() + "</yellow></gray>"));
        sender.sendMessage(mm.deserialize("<gray>Pack Server: <yellow>" + (plugin.getPackServer() != null && plugin.getPackServer().isRunning() ? "Active (" + plugin.getPackServer().getPackUrl() + ")" : "Disabled") + "</yellow></gray>"));
    }

    @Nullable
    private ItemStack resolveItemOrBlock(String id) {
        ItemStack item = Tessera.getItem(id);
        if (item != null) return item;

        var blockTpl = plugin.getBlockRegistry().getTemplate(id);
        if (blockTpl != null) {
            return new ItemBuilder(blockTpl.getMaterial())
                    .setTesseraId(id)
                    .setName(blockTpl.getDisplayName())
                    .build();
        }

        var furnTpl = plugin.getFurnitureManager().getType(id);
        if (furnTpl != null) {
            ItemStack furnItem = Tessera.getItem(furnTpl.getItemId());
            if (furnItem != null) return furnItem;
        }
        return null;
    }

    private void handleDrop(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("tessera.drop") && !sender.hasPermission("tessera.admin")) {
            sender.sendMessage(mm.deserialize("<red>No permission to drop custom items.</red>"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(mm.deserialize("<red>Usage: /" + label + " drop <id> [amount] [world] [x] [y] [z]</red>"));
            return;
        }

        String id = args[1];
        ItemStack stack = resolveItemOrBlock(id);
        if (stack == null) {
            sender.sendMessage(mm.deserialize("<red>Unknown custom item or block: <yellow>" + id + "</yellow></red>"));
            return;
        }

        int amount = 1;
        if (args.length > 2) {
            try {
                amount = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException ignored) {}
        }
        stack.setAmount(amount);

        Location loc;
        if (args.length >= 7) {
            org.bukkit.World w = Bukkit.getWorld(args[3]);
            if (w == null) {
                sender.sendMessage(mm.deserialize("<red>World not found: " + args[3] + "</red>"));
                return;
            }
            try {
                double x = Double.parseDouble(args[4]);
                double y = Double.parseDouble(args[5]);
                double z = Double.parseDouble(args[6]);
                loc = new Location(w, x, y, z);
            } catch (NumberFormatException e) {
                sender.sendMessage(mm.deserialize("<red>Invalid coordinates.</red>"));
                return;
            }
        } else if (sender instanceof Player p) {
            loc = p.getLocation();
        } else {
            sender.sendMessage(mm.deserialize("<red>Console must specify world and coordinates.</red>"));
            return;
        }

        loc.getWorld().dropItem(loc, stack);
        sender.sendMessage(mm.deserialize("<green>Dropped <yellow>" + amount + "x " + id + "</yellow> at [" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "].</green>"));
    }

    private void handleDurability(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can check durability.</red>"));
            return;
        }
        if (!sender.hasPermission("tessera.durability") && !sender.hasPermission("tessera.admin")) {
            sender.sendMessage(mm.deserialize("<red>No permission.</red>"));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(mm.deserialize("<red>Hold an item in your main hand.</red>"));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable dmg)) {
            player.sendMessage(mm.deserialize("<yellow>Held item does not support durability.</yellow>"));
            return;
        }

        int max = item.getType().getMaxDurability();
        int currentDamage = dmg.getDamage();
        int currentDurability = max > 0 ? (max - currentDamage) : 0;

        if (args.length < 2 || args[1].equalsIgnoreCase("get")) {
            player.sendMessage(mm.deserialize("<gradient:#00c6ff:#0072ff><bold>Durability Info</bold></gradient> <dark_gray>»</dark_gray> <gray>Current: <green>" + currentDurability + "</green> / <aqua>" + max + "</aqua> (Damage: <yellow>" + currentDamage + "</yellow>)</gray>"));
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("repair")) {
            dmg.setDamage(0);
            item.setItemMeta(meta);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
            player.sendMessage(mm.deserialize("<green>Held item repaired to full durability!</green>"));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(mm.deserialize("<red>Usage: /" + label + " durability <get|set|add|repair> [amount]</red>"));
            return;
        }

        int val;
        try {
            val = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(mm.deserialize("<red>Invalid durability value.</red>"));
            return;
        }

        if (action.equals("set")) {
            int newDmg = Math.max(0, Math.min(max, max - val));
            dmg.setDamage(newDmg);
            item.setItemMeta(meta);
            player.sendMessage(mm.deserialize("<green>Durability set to <yellow>" + val + "</yellow> / " + max + ".</green>"));
        } else if (action.equals("add")) {
            int newDmg = Math.max(0, dmg.getDamage() - val);
            dmg.setDamage(newDmg);
            item.setItemMeta(meta);
            player.sendMessage(mm.deserialize("<green>Restored <yellow>" + val + "</yellow> durability points.</green>"));
        }
    }

    private void handleCustomModelData(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can modify CustomModelData.</red>"));
            return;
        }
        if (!sender.hasPermission("tessera.admin")) {
            sender.sendMessage(mm.deserialize("<red>No permission.</red>"));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(mm.deserialize("<red>Hold an item in your main hand.</red>"));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (args.length < 2 || args[1].equalsIgnoreCase("get")) {
            if (meta.hasCustomModelData()) {
                player.sendMessage(mm.deserialize("<gradient:#00c6ff:#0072ff><bold>CustomModelData</bold></gradient> <dark_gray>»</dark_gray> <yellow>" + meta.getCustomModelData() + "</yellow>"));
            } else {
                player.sendMessage(mm.deserialize("<yellow>Held item has no CustomModelData set.</yellow>"));
            }
            return;
        }

        if (args[1].equalsIgnoreCase("set")) {
            if (args.length < 3) {
                player.sendMessage(mm.deserialize("<red>Usage: /" + label + " custommodeldata set <number></red>"));
                return;
            }
            try {
                int cmd = Integer.parseInt(args[2]);
                meta.setCustomModelData(cmd);
                item.setItemMeta(meta);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
                player.sendMessage(mm.deserialize("<green>Set CustomModelData to <yellow>" + cmd + "</yellow>!</green>"));
            } catch (NumberFormatException e) {
                player.sendMessage(mm.deserialize("<red>Invalid CustomModelData integer.</red>"));
            }
        }
    }

    private void handleNbt(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can inspect NBT/PDC.</red>"));
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(mm.deserialize("<red>Hold an item in your main hand.</red>"));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        player.sendMessage(mm.deserialize("<gradient:#00c6ff:#0072ff><bold>════ PersistentDataContainer Tags ════</bold></gradient>"));
        var pdc = meta.getPersistentDataContainer();
        var keys = pdc.getKeys();
        if (keys.isEmpty()) {
            player.sendMessage(mm.deserialize("<dark_gray>  (No persistent data keys on this item)</dark_gray>"));
        } else {
            for (var key : keys) {
                player.sendMessage(mm.deserialize("<dark_gray> » </dark_gray><aqua>" + key.getNamespace() + ":" + key.getKey() + "</aqua>")
                        .clickEvent(ClickEvent.copyToClipboard(key.toString()))
                        .hoverEvent(HoverEvent.showText(mm.deserialize("<green>Click to copy: " + key + "</green>"))));
            }
        }
        player.sendMessage(mm.deserialize("<gradient:#00c6ff:#0072ff><bold>═════════════════════════════════════</bold></gradient>"));
    }

    private void handleKillFurniture(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can remove nearby furniture.</red>"));
            return;
        }
        if (!sender.hasPermission("tessera.admin")) {
            sender.sendMessage(mm.deserialize("<red>No permission.</red>"));
            return;
        }
        double r = 15.0;
        if (args.length > 1) {
            try {
                r = Math.max(1.0, Math.min(50.0, Double.parseDouble(args[1])));
            } catch (NumberFormatException ignored) {}
        }
        FurnitureTool.removeNearby(player, r);
    }

    private void handleClearTemp(CommandSender sender) {
        if (!sender.hasPermission("tessera.admin")) {
            sender.sendMessage(mm.deserialize("<red>No permission.</red>"));
            return;
        }
        int purged = SeatManager.get() != null ? SeatManager.get().cleanupAllOrphans() : 0;
        sender.sendMessage(mm.deserialize("<green>Cleaned <yellow>" + purged + "</yellow> orphaned seat and marker entities across all worlds!</green>"));
    }

    private void handleSound(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can play sound tests.</red>"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(mm.deserialize("<red>Usage: /" + label + " sound <sound_name> [volume] [pitch]</red>"));
            return;
        }

        String sound = args[1];
        float vol = 1.0f;
        float pitch = 1.0f;
        if (args.length > 2) {
            try { vol = Float.parseFloat(args[2]); } catch (NumberFormatException ignored) {}
        }
        if (args.length > 3) {
            try { pitch = Float.parseFloat(args[3]); } catch (NumberFormatException ignored) {}
        }

        player.playSound(player.getLocation(), sound, vol, pitch);
        player.sendMessage(mm.deserialize("<green>Playing sound <yellow>" + sound + "</yellow> (vol: " + vol + ", pitch: " + pitch + ")</green>"));
    }

    private void handleStats(CommandSender sender) {
        if (!sender.hasPermission("tessera.admin")) {
            sender.sendMessage(mm.deserialize("<red>No permission.</red>"));
            return;
        }
        Runtime rt = Runtime.getRuntime();
        long free = rt.freeMemory() / 1024 / 1024;
        long total = rt.totalMemory() / 1024 / 1024;
        long max = rt.maxMemory() / 1024 / 1024;
        long used = total - free;

        sender.sendMessage(mm.deserialize("<gradient:#00c6ff:#0072ff><bold>════════ Tessera Performance & Telemetry ════════</bold></gradient>"));
        sender.sendMessage(mm.deserialize("<gray>JVM Memory: <yellow>" + used + " MB</yellow> / <aqua>" + max + " MB</aqua> (Allocated: " + total + " MB)</gray>"));
        sender.sendMessage(mm.deserialize("<gray>Processors: <yellow>" + rt.availableProcessors() + "</yellow> cores  <dark_gray>│</dark_gray>  Active Threads: <yellow>" + Thread.activeCount() + "</yellow></gray>"));
        sender.sendMessage(mm.deserialize("<gray>Registered Items: <aqua>" + plugin.getItemRegistry().getAll().size() + "</aqua>  <dark_gray>│</dark_gray>  Blocks: <aqua>" + plugin.getBlockRegistry().getAll().size() + "</aqua></gray>"));
        sender.sendMessage(mm.deserialize("<gray>Registered Furniture: <aqua>" + plugin.getFurnitureManager().getAll().size() + "</aqua>  <dark_gray>│</dark_gray>  Recipes: <aqua>" + plugin.getRecipeManager().getAll().size() + "</aqua></gray>"));
        sender.sendMessage(mm.deserialize("<gray>Glyphs / Fonts: <aqua>" + plugin.getGlyphRegistry().getAll().size() + "</aqua>  <dark_gray>│</dark_gray>  Mechanics: <aqua>" + plugin.getMechanicsManager().getAll().size() + "</aqua></gray>"));
        boolean packRunning = plugin.getPackServer() != null && plugin.getPackServer().isRunning();
        sender.sendMessage(mm.deserialize("<gray>Pack Web Server: " + (packRunning ? "<green>ACTIVE (Port " + plugin.getPackServer().getPort() + ")</green>" : "<red>OFFLINE</red>") + "</gray>"));
        sender.sendMessage(mm.deserialize("<gradient:#00c6ff:#0072ff><bold>═════════════════════════════════════════════════</bold></gradient>"));
    }

    private void handleEmote(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>Only players can play emotes.</red>"));
            return;
        }
        String emote = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "sparkle";
        Location loc = player.getLocation().add(0, 1.2, 0);

        switch (emote) {
            case "heart" -> {
                player.getWorld().spawnParticle(Particle.HEART, loc, 15, 0.4, 0.4, 0.4, 0.1);
                player.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.8f);
            }
            case "totem" -> {
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 30, 0.5, 0.8, 0.5, 0.2);
                player.playSound(loc, Sound.ITEM_TOTEM_USE, 0.7f, 1.5f);
            }
            case "flame" -> {
                player.getWorld().spawnParticle(Particle.FLAME, loc, 25, 0.3, 0.5, 0.3, 0.05);
                player.playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 1.0f, 1.2f);
            }
            case "notes" -> {
                player.getWorld().spawnParticle(Particle.NOTE, loc, 12, 0.5, 0.5, 0.5, 0.1);
                player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
            }
            default -> {
                player.getWorld().spawnParticle(Particle.FIREWORK, loc, 20, 0.4, 0.6, 0.4, 0.08);
                player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.8f);
            }
        }
        player.sendActionBar(mm.deserialize("<gradient:#00c6ff:#0072ff>✦ Emote: " + emote.toUpperCase(Locale.ROOT) + " ✦</gradient>"));
    }

    private void handleExport(CommandSender sender) {
        if (!sender.hasPermission("tessera.admin")) {
            sender.sendMessage(mm.deserialize("<red>No permission.</red>"));
            return;
        }
        File exportFile = new File(plugin.getDataFolder(), "catalog_export.json");
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n  \"version\": \"").append(plugin.getDescription().getVersion()).append("\",\n");
            sb.append("  \"items\": ").append(plugin.getItemRegistry().getIds().size()).append(",\n");
            sb.append("  \"blocks\": ").append(plugin.getBlockRegistry().getIds().size()).append(",\n");
            sb.append("  \"furniture\": ").append(plugin.getFurnitureManager().getIds().size()).append("\n}");
            java.nio.file.Files.writeString(exportFile.toPath(), sb.toString());
            sender.sendMessage(mm.deserialize("<green>Exported catalog manifest to <yellow>" + exportFile.getName() + "</yellow>!</green>"));
        } catch (Exception e) {
            sender.sendMessage(mm.deserialize("<red>Failed to export catalog: " + e.getMessage() + "</red>"));
        }
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subs = List.of(
                    "menu", "browse", "admin", "settings", "get", "give", "giveall", "drop",
                    "durability", "cmd", "custommodeldata", "nbt", "killfurniture", "cleartemp",
                    "sound", "stats", "emote", "export", "recipe", "inspect", "search",
                    "hat", "sit", "repair", "block", "furniture",
                    "debug", "hud", "pack", "welcome", "reload", "info", "help"
            );
            return filter(subs, args[0]);
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "menu" -> {
                    return filter(List.of("browse", "ALL", "ITEMS", "BLOCKS", "FURNITURE"), args[1]);
                }
                case "browse" -> {
                    return filter(List.of("ALL", "ITEMS", "BLOCKS", "FURNITURE"), args[1]);
                }
                case "get", "giveall", "drop", "recipe" -> {
                    return filter(getAllCustomIds(), args[1]);
                }
                case "give" -> {
                    return filter(getOnlinePlayerNames(), args[1]);
                }
                case "durability" -> {
                    return filter(List.of("get", "set", "add", "repair"), args[1]);
                }
                case "cmd", "custommodeldata" -> {
                    return filter(List.of("get", "set"), args[1]);
                }
                case "emote" -> {
                    return filter(List.of("sparkle", "heart", "totem", "flame", "notes"), args[1]);
                }
                case "sound", "playsound" -> {
                    return filter(List.of("entity.player.levelup", "block.amethyst_block.chime", "ui.button.click", "entity.item.pickup"), args[1]);
                }
                case "killfurniture" -> {
                    return filter(List.of("5", "10", "15", "25"), args[1]);
                }
                case "repair" -> {
                    return filter(List.of("hand", "all"), args[1]);
                }
                case "block" -> {
                    return filter(List.of("info", "remove"), args[1]);
                }
                case "furniture" -> {
                    return filter(List.of("list", "remove"), args[1]);
                }
                case "debug" -> {
                    return filter(List.of("benchmark", "entities"), args[1]);
                }
                case "hud" -> {
                    return filter(List.of("toggle", "actionbar"), args[1]);
                }
                case "pack" -> {
                    return filter(List.of("generate", "apply", "import", "push"), args[1]);
                }
                case "welcome" -> {
                    return filter(List.of("test"), args[1]);
                }
                case "help" -> {
                    return filter(List.of("1", "2", "3"), args[1]);
                }
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                return filter(getAllCustomIds(), args[2]);
            }
            if (args[0].equalsIgnoreCase("drop")) {
                return filter(List.of("1", "16", "64"), args[2]);
            }
            if (args[0].equalsIgnoreCase("pack")) {
                if (args[1].equalsIgnoreCase("apply") || args[1].equalsIgnoreCase("push")) {
                    List<String> options = new ArrayList<>(getOnlinePlayerNames());
                    options.add("all");
                    return filter(options, args[2]);
                }
                if (args[1].equalsIgnoreCase("import")) {
                    List<String> options = new ArrayList<>();
                    options.add("all");
                    File[] zips = plugin.getSmartPackImporter().getImportsDir().listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".zip"));
                    if (zips != null) {
                        for (File z : zips) options.add(z.getName());
                    }
                    return filter(options, args[2]);
                }
            }
            if (args[0].equalsIgnoreCase("convert")) {
                return filter(List.of("merge", "nomirror"), args[2]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> getAllCustomIds() {
        List<String> ids = new ArrayList<>();
        ids.addAll(plugin.getItemRegistry().getIds());
        ids.addAll(plugin.getBlockRegistry().getIds());
        ids.addAll(plugin.getFurnitureManager().getIds());
        return ids;
    }

    private List<String> getOnlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            names.add(p.getName());
        }
        return names;
    }

    private List<String> filter(List<String> list, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String item : list) {
            if (item.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(item);
            }
        }
        return result;
    }
}
