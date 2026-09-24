package com.prisma.tessera;

import com.prisma.tessera.blocks.BlockListener;
import com.prisma.tessera.blocks.BlockRegistry;
import com.prisma.tessera.commands.TesseraCommand;
import com.prisma.tessera.configs.ConfigManager;
import com.prisma.tessera.configs.LanguageManager;
import com.prisma.tessera.fonts.FontManager;
import com.prisma.tessera.fonts.GlyphRegistry;
import com.prisma.tessera.furniture.FurnitureManager;
import com.prisma.tessera.furniture.FurniturePlaceListener;
import com.prisma.tessera.gui.GUIListener;
import com.prisma.tessera.hud.HudManager;
import com.prisma.tessera.items.ItemRegistry;
import com.prisma.tessera.mechanics.MechanicsManager;
import com.prisma.tessera.pack.PackGenerator;
import com.prisma.tessera.pack.importer.SmartPackImporter;
import com.prisma.tessera.pack.protection.PackProtector;
import com.prisma.tessera.pack.server.PackListener;
import com.prisma.tessera.pack.server.PackServer;
import com.prisma.tessera.recipes.RecipeListener;
import com.prisma.tessera.recipes.RecipeManager;
import com.prisma.tessera.utils.SeatManager;
import com.prisma.tessera.welcome.WelcomeListener;
import com.prisma.tessera.welcome.WelcomeManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class TesseraPlugin extends JavaPlugin {

    private static TesseraPlugin instance;

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private ItemRegistry itemRegistry;
    private BlockRegistry blockRegistry;
    private FurnitureManager furnitureManager;
    private RecipeManager recipeManager;
    private GlyphRegistry glyphRegistry;
    private FontManager fontManager;
    private HudManager hudManager;
    private MechanicsManager mechanicsManager;
    private PackProtector packProtector;
    private PackServer packServer;
    private PackGenerator packGenerator;
    private PackListener packListener;
    private SmartPackImporter smartPackImporter;
    private WelcomeManager welcomeManager;
    private SeatManager seatManager;

    public static TesseraPlugin get() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
    }

@Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.load();
        languageManager = new LanguageManager(this);
        languageManager.load();

        itemRegistry = new ItemRegistry(this);
        blockRegistry = new BlockRegistry(this);
        furnitureManager = new FurnitureManager(this);
        recipeManager = new RecipeManager(this);
        glyphRegistry = new GlyphRegistry(this);
        fontManager = new FontManager(this, glyphRegistry);
        hudManager = new HudManager(this);
        mechanicsManager = new MechanicsManager(this);
        packProtector = new PackProtector(this);
        packServer = new PackServer(this);
        packGenerator = new PackGenerator(this);
        packListener = new PackListener(this, packServer);
        smartPackImporter = new SmartPackImporter(this);
        welcomeManager = new WelcomeManager(this);
        seatManager = new SeatManager(this);

        reloadAllData();

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new BlockListener(this, blockRegistry), this);
        pm.registerEvents(new FurniturePlaceListener(this, furnitureManager), this);
        pm.registerEvents(new GUIListener(), this);
        pm.registerEvents(new RecipeListener(recipeManager), this);
        pm.registerEvents(packListener, this);
        pm.registerEvents(new WelcomeListener(welcomeManager), this);
        pm.registerEvents(seatManager, this);
        pm.registerEvents(hudManager, this);

        hudManager.start();
        smartPackImporter.startWatcher();

        TesseraCommand cmd = new TesseraCommand(this);
        PluginCommand command = getCommand("tessera");
        if (command != null) {
            command.setExecutor(cmd);
            command.setTabCompleter(cmd);
        }

        getLogger().info("Tessera v" + getDescription().getVersion() + " initialized successfully!");
    }

    public void reloadAllData() {
        if (languageManager != null) {
            languageManager.reload();
        }
        itemRegistry.loadAll();
        blockRegistry.loadAll();
        furnitureManager.loadAll();
        recipeManager.loadAll();
        glyphRegistry.loadAll();
        hudManager.loadAll();
        mechanicsManager.registerDefaults();
        packGenerator.generate();
        packServer.start();
    }

    @Override
    public void onDisable() {
        if (smartPackImporter != null) {
            smartPackImporter.stopWatcher();
        }
        if (seatManager != null) {
            seatManager.cleanUp();
        }
        if (packServer != null) {
            packServer.stop();
        }
        if (hudManager != null) {
            hudManager.stop();
        }
        if (recipeManager != null) {
            recipeManager.unregisterAll();
        }
        if (mechanicsManager != null) {
            mechanicsManager.unregisterAll();
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    public BlockRegistry getBlockRegistry() {
        return blockRegistry;
    }

    public FurnitureManager getFurnitureManager() {
        return furnitureManager;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public GlyphRegistry getGlyphRegistry() {
        return glyphRegistry;
    }

    public FontManager getFontManager() {
        return fontManager;
    }

    public HudManager getHudManager() {
        return hudManager;
    }

    public MechanicsManager getMechanicsManager() {
        return mechanicsManager;
    }

    public PackProtector getPackProtector() {
        return packProtector;
    }

    public PackServer getPackServer() {
        return packServer;
    }

    public PackGenerator getPackGenerator() {
        return packGenerator;
    }

    public WelcomeManager getWelcomeManager() {
        return welcomeManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public SeatManager getSeatManager() {
        return seatManager;
    }

    public PackListener getPackListener() {
        return packListener;
    }

    public SmartPackImporter getSmartPackImporter() {
        return smartPackImporter;
    }
}
