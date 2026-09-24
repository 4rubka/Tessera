package com.prisma.tessera.pack.importer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.prisma.tessera.TesseraPlugin;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SmartPackImporter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final AtomicInteger CMD_COUNTER = new AtomicInteger(20000);
    private static final long MAX_DOWNLOAD_BYTES = 200L * 1024 * 1024;

    public record ImportResult(
            String sourceName,
            int texturesCount,
            int modelsCount,
            int soundsMerged,
            int fontsMerged,
            int itemsGenerated,
            int blocksGenerated,
            long elapsedMillis,
            String packUrl,
            String sha1Hex,
            List<String> warnings,
            boolean success
    ) {}

    private final TesseraPlugin plugin;
    private final File importsDir;
    private final File processedDir;
    private final File packDir;
    private BukkitTask watcherTask;
    private final Map<String, Long> failedImports = new ConcurrentHashMap<>();

    public SmartPackImporter(@NotNull TesseraPlugin plugin) {
        this.plugin = plugin;
        this.importsDir = new File(plugin.getDataFolder(), "imports");
        this.processedDir = new File(importsDir, "processed");
        this.packDir = new File(plugin.getDataFolder(), "pack");

        this.importsDir.mkdirs();
        this.processedDir.mkdirs();
        this.packDir.mkdirs();
    }

    public void startWatcher() {
        stopWatcher();
        boolean autoWatch = plugin.getConfig().getBoolean("pack.importer.auto_watch", true);
        if (!autoWatch) return;

        int intervalSeconds = Math.max(5, plugin.getConfig().getInt("pack.importer.watch_interval_seconds", 15));
        watcherTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            // Skip files that are still being copied in, and broken zips until they change on disk.
            long now = System.currentTimeMillis();
            File[] zips = importsDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".zip"));
            if (zips == null) return;
            for (File zip : zips) {
                long modified = zip.lastModified();
                if (now - modified < 5000 || Long.valueOf(modified).equals(failedImports.get(zip.getName()))) continue;
                plugin.getLogger().info("[SmartPackImporter] Detected new resource pack " + zip.getName() + " in imports/. Importing...");
                if (importZip(zip, true, true).success()) {
                    failedImports.remove(zip.getName());
                } else {
                    failedImports.put(zip.getName(), modified);
                    plugin.getLogger().warning("[SmartPackImporter] " + zip.getName() + " was not imported. It is retried after the file changes.");
                }
            }
        }, 100L, intervalSeconds * 20L);
    }

    public void stopWatcher() {
        if (watcherTask != null) {
            watcherTask.cancel();
            watcherTask = null;
        }
    }

    // synchronized: the folder watcher, the command and URL imports run on different async threads.
    public synchronized ImportResult importZip(@NotNull File zipFile, boolean autoGenerateItems, boolean broadcastPack) {
        long start = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();
        int texturesCount = 0;
        int modelsCount = 0;
        int soundsMerged = 0;
        int fontsMerged = 0;
        int itemsGenerated = 0;
        int blocksGenerated = 0;

        if (!zipFile.exists() || !zipFile.isFile()) {
            warnings.add("File does not exist: " + zipFile.getName());
            return new ImportResult(zipFile.getName(), 0, 0, 0, 0, 0, 0, 0, null, null, warnings, false);
        }

        File tempExtractDir = new File(plugin.getDataFolder(), ".temp_import_" + System.currentTimeMillis());
        tempExtractDir.mkdirs();

        try {
            
            try (ZipFile zip = new ZipFile(zipFile)) {
                String extractRoot = tempExtractDir.getCanonicalPath() + File.separator;
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();

                    if (name.startsWith("__MACOSX") || name.contains(".DS_Store") || name.endsWith("Thumbs.db")) {
                        continue;
                    }

                    File destFile = new File(tempExtractDir, name);
                    // Zip Slip: an entry like ../../plugins/x.jar would otherwise be written outside the
                    // temp folder, for example into plugins/, and run on the next start.
                    if (!destFile.getCanonicalPath().startsWith(extractRoot)) {
                        warnings.add("Skipped unsafe zip entry: " + name);
                        plugin.getLogger().warning("[SmartPackImporter] Skipped zip entry outside the pack: " + name);
                        continue;
                    }
                    if (entry.isDirectory()) {
                        destFile.mkdirs();
                    } else {
                        destFile.getParentFile().mkdirs();
                        try (InputStream in = zip.getInputStream(entry);
                             FileOutputStream out = new FileOutputStream(destFile)) {
                            in.transferTo(out);
                        }
                    }
                }
            }

            File assetsRoot = new File(tempExtractDir, "assets");
            if (!assetsRoot.exists()) {
                
                File[] subs = tempExtractDir.listFiles(File::isDirectory);
                if (subs != null && subs.length == 1) {
                    File nested = new File(subs[0], "assets");
                    if (nested.exists()) {
                        assetsRoot = nested;
                    }
                }
            }

            if (!assetsRoot.exists()) {
                warnings.add("No 'assets/' folder found in zip. Please ensure the zip follows Minecraft resource pack structure.");
                deleteDir(tempExtractDir);
                return new ImportResult(zipFile.getName(), 0, 0, 0, 0, 0, 0, System.currentTimeMillis() - start, null, null, warnings, false);
            }

            File packPng = new File(tempExtractDir, "pack.png");
            if (packPng.exists()) {
                Files.copy(packPng.toPath(), new File(packDir, "pack.png").toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            File[] namespaces = assetsRoot.listFiles(File::isDirectory);
            if (namespaces != null) {
                for (File nsDir : namespaces) {
                    String namespace = nsDir.getName();
                    File targetNsDir = new File(packDir, "assets/" + namespace);
                    targetNsDir.mkdirs();

                    File soundsJson = new File(nsDir, "sounds.json");
                    if (soundsJson.exists()) {
                        soundsMerged += mergeSoundsJson(soundsJson, new File(targetNsDir, "sounds.json"), warnings);
                    }

                    File fontDir = new File(nsDir, "font");
                    if (fontDir.exists()) {
                        File[] fontFiles = fontDir.listFiles((dir, name) -> name.endsWith(".json"));
                        if (fontFiles != null) {
                            File targetFontDir = new File(targetNsDir, "font");
                            targetFontDir.mkdirs();
                            for (File ff : fontFiles) {
                                fontsMerged += mergeFontJson(ff, new File(targetFontDir, ff.getName()), warnings);
                            }
                        }
                    }

                    File[] subCategories = nsDir.listFiles(File::isDirectory);
                    if (subCategories != null) {
                        for (File catDir : subCategories) {
                            String catName = catDir.getName();
                            File targetCatDir = new File(targetNsDir, catName);
                            targetCatDir.mkdirs();

                            if (catName.equals("textures")) {
                                texturesCount += copyDirectoryContents(catDir, targetCatDir);
                            } else if (catName.equals("models")) {
                                modelsCount += copyDirectoryContents(catDir, targetCatDir);
                            } else if (!catName.equals("font")) {
                                copyDirectoryContents(catDir, targetCatDir);
                            }
                        }
                    }
                }
            }

            if (autoGenerateItems) {
                itemsGenerated = autoGenerateItemTemplates(assetsRoot, warnings);
                blocksGenerated = autoGenerateBlockTemplates(assetsRoot, warnings);
            }

            deleteDir(tempExtractDir);

            File destProcessed = new File(processedDir, System.currentTimeMillis() + "_" + zipFile.getName());
            Files.move(zipFile.toPath(), destProcessed.toPath(), StandardCopyOption.REPLACE_EXISTING);

            long elapsed = System.currentTimeMillis() - start;
            plugin.getLogger().info("[SmartPackImporter] Merged pack '" + zipFile.getName() + "': " +
                    texturesCount + " textures, " + modelsCount + " models, " + itemsGenerated + " items generated in " + elapsed + "ms.");

            final int finalTex = texturesCount;
            final int finalMod = modelsCount;
            final int finalItems = itemsGenerated;
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.reloadAllData();
                if (broadcastPack && plugin.getPackListener() != null) {
                    plugin.getPackListener().sendToAllOnline();
                }
                notifyAdmins(zipFile.getName(), finalTex, finalMod, finalItems, elapsed);
            });

            String url = plugin.getPackServer() != null ? plugin.getPackServer().getPackUrl() : "None";
            String sha1 = plugin.getPackServer() != null ? plugin.getPackServer().getSha1Hex() : "None";

            return new ImportResult(
                    zipFile.getName(), texturesCount, modelsCount, soundsMerged, fontsMerged,
                    itemsGenerated, blocksGenerated, elapsed, url, sha1, warnings, true
            );

        } catch (Exception e) {
            deleteDir(tempExtractDir);
            warnings.add("Ingestion failed: " + e.getMessage());
            plugin.getLogger().severe("[SmartPackImporter] Error importing " + zipFile.getName() + ": " + e.getMessage());
            return new ImportResult(zipFile.getName(), texturesCount, modelsCount, soundsMerged, fontsMerged,
                    itemsGenerated, blocksGenerated, System.currentTimeMillis() - start, null, null, warnings, false);
        }
    }

    public void importFromUrl(@NotNull String urlString, boolean autoGenerate, boolean broadcast, @Nullable Player requester) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (requester != null) {
                    requester.sendMessage(MM.deserialize("<gradient:#00c6ff:#0072ff>Downloading resource pack from URL...</gradient>"));
                }
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(15))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();

                // Redirects are followed by hand so every hop is checked against internal addresses.
                URI uri = URI.create(urlString);
                HttpResponse<InputStream> response = null;
                for (int hop = 0; hop <= 5; hop++) {
                    checkPublicUri(uri);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(uri)
                            .timeout(Duration.ofMinutes(2))
                            .header("User-Agent", "Tessera-ResourcePack-Pipeline/1.0")
                            .GET()
                            .build();
                    response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                    int code = response.statusCode();
                    if (code < 300 || code >= 400) break;
                    String location = response.headers().firstValue("Location").orElse(null);
                    response.body().close();
                    if (location == null || hop == 5) throw new IOException("Too many or broken redirects");
                    uri = uri.resolve(location);
                }

                // Written under a .part name so the folder watcher does not import a half-downloaded file.
                String downloadName = "downloaded_" + System.currentTimeMillis() + ".zip";
                File partFile = new File(importsDir, downloadName + ".part");
                File downloadTarget = new File(importsDir, downloadName);

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    try (InputStream in = response.body();
                         FileOutputStream out = new FileOutputStream(partFile)) {
                        byte[] buffer = new byte[8192];
                        long total = 0;
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            total += read;
                            if (total > MAX_DOWNLOAD_BYTES) break;
                            out.write(buffer, 0, read);
                        }
                        if (total > MAX_DOWNLOAD_BYTES) {
                            out.close();
                            Files.deleteIfExists(partFile.toPath());
                            throw new IOException("Pack is larger than " + MAX_DOWNLOAD_BYTES / 1024 / 1024 + " MB");
                        }
                    }
                    Files.move(partFile.toPath(), downloadTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    if (requester != null) {
                        requester.sendMessage(MM.deserialize("<green>Download complete! Ingesting into server resource pack...</green>"));
                    }

                    ImportResult res = importZip(downloadTarget, autoGenerate, broadcast);
                    if (requester != null) {
                        if (res.success()) {
                            requester.sendMessage(MM.deserialize("<gradient:#00c6ff:#0072ff><bold>Pack Imported & Deployed!</bold></gradient>"));
                            requester.sendMessage(MM.deserialize("<gray>Textures: <green>" + res.texturesCount() + "</green> | Models: <aqua>" + res.modelsCount() + "</aqua> | Auto Items: <yellow>" + res.itemsGenerated() + "</yellow></gray>"));
                            requester.sendMessage(MM.deserialize("<gray>Server Pack URL: <aqua>" + res.packUrl() + "</aqua></gray>"));
                        } else {
                            requester.sendMessage(MM.deserialize("<red>Failed to import pack: " + String.join(", ", res.warnings()) + "</red>"));
                        }
                    }
                } else {
                    if (requester != null) {
                        requester.sendMessage(MM.deserialize("<red>HTTP download failed: HTTP status " + response.statusCode() + "</red>"));
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[SmartPackImporter] Download from " + urlString + " failed: " + e.getMessage());
                if (requester != null) {
                    requester.sendMessage(MM.deserialize("<red>Download error: " + e.getMessage() + "</red>"));
                }
            }
        });
    }

    private static void checkPublicUri(URI uri) throws IOException {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https") || uri.getHost() == null) {
            throw new IOException("Only http(s) URLs are allowed");
        }
        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
            byte[] raw = address.getAddress();
            boolean uniqueLocalV6 = raw.length == 16 && (raw[0] & 0xFE) == 0xFC;
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress() || uniqueLocalV6) {
                throw new IOException("Refusing to download from internal address " + uri.getHost());
            }
        }
    }

    private int mergeSoundsJson(File source, File target, List<String> warnings) {
        int count = 0;
        try {
            JsonObject sourceJson = JsonParser.parseReader(new InputStreamReader(new FileInputStream(source), StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject targetJson = new JsonObject();
            if (target.exists()) {
                targetJson = JsonParser.parseReader(new InputStreamReader(new FileInputStream(target), StandardCharsets.UTF_8)).getAsJsonObject();
            }

            for (Map.Entry<String, JsonElement> entry : sourceJson.entrySet()) {
                targetJson.add(entry.getKey(), entry.getValue());
                count++;
            }

            Files.writeString(target.toPath(), GSON.toJson(targetJson), StandardCharsets.UTF_8);
        } catch (Exception e) {
            warnings.add("Failed to merge sounds.json: " + e.getMessage());
        }
        return count;
    }

    private int mergeFontJson(File source, File target, List<String> warnings) {
        int count = 0;
        try {
            JsonObject sourceJson = JsonParser.parseReader(new InputStreamReader(new FileInputStream(source), StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray sourceProviders = sourceJson.has("providers") ? sourceJson.getAsJsonArray("providers") : new JsonArray();

            JsonObject targetJson;
            JsonArray targetProviders;
            if (target.exists()) {
                targetJson = JsonParser.parseReader(new InputStreamReader(new FileInputStream(target), StandardCharsets.UTF_8)).getAsJsonObject();
                targetProviders = targetJson.has("providers") ? targetJson.getAsJsonArray("providers") : new JsonArray();
            } else {
                targetJson = new JsonObject();
                targetProviders = new JsonArray();
                targetJson.add("providers", targetProviders);
            }

            for (JsonElement provider : sourceProviders) {
                targetProviders.add(provider);
                count++;
            }

            Files.writeString(target.toPath(), GSON.toJson(targetJson), StandardCharsets.UTF_8);
        } catch (Exception e) {
            warnings.add("Failed to merge font json " + source.getName() + ": " + e.getMessage());
        }
        return count;
    }

    private int autoGenerateItemTemplates(File assetsRoot, List<String> warnings) {
        int generated = 0;
        File itemsDir = new File(plugin.getDataFolder(), "items");
        itemsDir.mkdirs();

        File[] namespaces = assetsRoot.listFiles(File::isDirectory);
        if (namespaces == null) return 0;

        for (File ns : namespaces) {
            File modelsItem = new File(ns, "models/item");
            if (!modelsItem.exists()) continue;

            List<File> jsonModels = findFilesRecursive(modelsItem, ".json");
            for (File modelFile : jsonModels) {
                try {
                    String rawJson = Files.readString(modelFile.toPath(), StandardCharsets.UTF_8);
                    JsonObject obj = JsonParser.parseString(rawJson).getAsJsonObject();

                    if (obj.has("overrides")) {
                        JsonArray overrides = obj.getAsJsonArray("overrides");
                        for (JsonElement el : overrides) {
                            if (el.isJsonObject()) {
                                JsonObject ov = el.getAsJsonObject();
                                if (ov.has("predicate") && ov.getAsJsonObject("predicate").has("custom_model_data")) {
                                    int cmd = ov.getAsJsonObject("predicate").get("custom_model_data").getAsInt();
                                    String modelTarget = ov.has("model") ? ov.get("model").getAsString() : "";
                                    String id = extractIdFromPath(modelTarget);
                                    if (id.isEmpty()) id = modelFile.getName().replace(".json", "") + "_" + cmd;

                                    if (createItemYaml(itemsDir, id, guessMaterialFromName(modelFile.getName()), cmd, id)) {
                                        generated++;
                                    }
                                }
                            }
                        }
                    } else if (modelFile.getParentFile().getName().equals("custom") || !isVanillaItemName(modelFile.getName())) {
                        
                        String id = modelFile.getName().replace(".json", "").toLowerCase(Locale.ROOT);
                        String guessedMat = guessMaterialFromName(id);
                        int cmd = CMD_COUNTER.incrementAndGet();
                        if (createItemYaml(itemsDir, "imported_" + id, guessedMat, cmd, formatDisplayName(id))) {
                            generated++;
                        }
                    }
                } catch (Exception e) {
                    warnings.add("Failed to analyze model " + modelFile.getName() + ": " + e.getMessage());
                }
            }
        }
        return generated;
    }

    private int autoGenerateBlockTemplates(File assetsRoot, List<String> warnings) {
        int generated = 0;
        File blocksDir = new File(plugin.getDataFolder(), "blocks");
        blocksDir.mkdirs();

        File[] namespaces = assetsRoot.listFiles(File::isDirectory);
        if (namespaces == null) return 0;

        for (File ns : namespaces) {
            File modelsBlock = new File(ns, "models/block");
            if (!modelsBlock.exists()) continue;

            List<File> blockModels = findFilesRecursive(modelsBlock, ".json");
            for (File bModel : blockModels) {
                String id = bModel.getName().replace(".json", "").toLowerCase(Locale.ROOT);
                if (isVanillaBlockName(id)) continue;

                File ymlFile = new File(blocksDir, "imported_" + id + ".yml");
                if (!ymlFile.exists()) {
                    try {
                        YamlConfiguration yaml = new YamlConfiguration();
                        yaml.set(id + ".material", "NOTE_BLOCK");
                        yaml.set(id + ".display_name", "<gradient:#00c6ff:#0072ff><bold>" + formatDisplayName(id) + "</bold></gradient>");
                        yaml.set(id + ".block_material", "NOTE_BLOCK");
                        yaml.set(id + ".hardness", 2.0);
                        yaml.set(id + ".requires_correct_tool", true);
                        yaml.save(ymlFile);
                        generated++;
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        return generated;
    }

    private boolean createItemYaml(File itemsDir, String id, String material, int cmd, String displayName) {
        File itemFile = new File(itemsDir, id + ".yml");
        if (itemFile.exists()) return false;

        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set(id + ".material", material);
            yaml.set(id + ".display_name", "<gradient:#00c6ff:#0072ff><bold>" + displayName + "</bold></gradient>");
            yaml.set(id + ".custom_model_data", cmd);
            yaml.set(id + ".lore", List.of("<gray>Auto-imported server resource</gray>"));
            yaml.save(itemFile);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private String guessMaterialFromName(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("sword") || n.contains("blade") || n.contains("katana") || n.contains("dagger") || n.contains("saber")) {
            return "DIAMOND_SWORD";
        }
        if (n.contains("shield")) return "SHIELD";
        if (n.contains("bow")) return "BOW";
        if (n.contains("crossbow")) return "CROSSBOW";
        if (n.contains("pickaxe") || n.contains("drill")) return "DIAMOND_PICKAXE";
        if (n.contains("axe") || n.contains("halberd") || n.contains("battleaxe")) return "DIAMOND_AXE";
        if (n.contains("shovel") || n.contains("spade")) return "DIAMOND_SHOVEL";
        if (n.contains("hoe") || n.contains("scythe")) return "DIAMOND_HOE";
        if (n.contains("helmet") || n.contains("hat") || n.contains("crown") || n.contains("mask")) return "DIAMOND_HELMET";
        if (n.contains("chestplate") || n.contains("armor") || n.contains("robe") || n.contains("wings")) return "DIAMOND_CHESTPLATE";
        if (n.contains("leggings") || n.contains("pants")) return "DIAMOND_LEGGINGS";
        if (n.contains("boots") || n.contains("shoes")) return "DIAMOND_BOOTS";
        if (n.contains("staff") || n.contains("wand") || n.contains("rod")) return "BLAZE_ROD";
        if (n.contains("potion") || n.contains("flask") || n.contains("drink")) return "POTION";
        if (n.contains("coin") || n.contains("ruby") || n.contains("gem")) return "AMETHYST_SHARD";
        return "PAPER";
    }

    private String formatDisplayName(String raw) {
        String clean = raw.replace('_', ' ').replace('-', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : clean.split("\\s+")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String extractIdFromPath(String path) {
        if (path == null || path.isEmpty()) return "";
        int lastSlash = path.lastIndexOf('/');
        return lastSlash != -1 ? path.substring(lastSlash + 1) : path;
    }

    private boolean isVanillaItemName(String name) {
        String clean = name.replace(".json", "").toUpperCase(Locale.ROOT);
        return org.bukkit.Material.matchMaterial(clean) != null;
    }

    private boolean isVanillaBlockName(String name) {
        String clean = name.replace(".json", "").toUpperCase(Locale.ROOT);
        org.bukkit.Material mat = org.bukkit.Material.matchMaterial(clean);
        return mat != null && mat.isBlock();
    }

    private int copyDirectoryContents(File source, File target) throws IOException {
        int count = 0;
        File[] files = source.listFiles();
        if (files == null) return 0;

        for (File f : files) {
            File dest = new File(target, f.getName());
            if (f.isDirectory()) {
                dest.mkdirs();
                count += copyDirectoryContents(f, dest);
            } else {
                Files.copy(f.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                count++;
            }
        }
        return count;
    }

    private List<File> findFilesRecursive(File dir, String extension) {
        List<File> list = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return list;

        for (File f : files) {
            if (f.isDirectory()) {
                list.addAll(findFilesRecursive(f, extension));
            } else if (f.getName().endsWith(extension)) {
                list.add(f);
            }
        }
        return list;
    }

    private void deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDir(f);
                else f.delete();
            }
        }
        dir.delete();
    }

    private void notifyAdmins(String zipName, int textures, int models, int items, long elapsed) {
        String msg = "<gradient:#00c6ff:#0072ff><bold>✦ Auto-Pack Pipeline:</bold></gradient> <gray>Imported <aqua>" +
                zipName + "</aqua> (" + textures + " textures, " + models + " models, " + items + " items generated) in <yellow>" + elapsed + "ms</yellow>!</gray>";
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("tessera.admin")) {
                p.sendMessage(MM.deserialize(msg));
                p.playSound(p.getLocation(), "entity.player.levelup", 0.5f, 1.8f);
            }
        }
    }

    public File getImportsDir() {
        return importsDir;
    }
}
