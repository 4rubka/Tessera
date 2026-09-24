package com.prisma.tessera.fonts;

import com.prisma.tessera.TesseraPlugin;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GlyphRegistry {

    private static GlyphRegistry instance;

    private final TesseraPlugin plugin;
    private final Map<String, Glyph> glyphs = new LinkedHashMap<>();
    private final Map<Character, Glyph> byChar = new LinkedHashMap<>();
    private char nextChar = '\uE000';

    public GlyphRegistry(@NotNull TesseraPlugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static GlyphRegistry get() {
        return instance;
    }

    public void loadAll() {
        glyphs.clear();
        byChar.clear();
        nextChar = '\uE010';

        registerDefaults();

        loadFromDirectory("fonts");
        loadFromDirectory("glyphs");
    }

    public void registerDefaults() {
        register(new Glyph("tessera_badge", '\uE001', "tessera:font/tessera_badge.png", 8, 8, 8, null));
        // Glyph top edge = text y + 7 - ascent. Metrics below are fitted to where each banner is drawn.
        // Item tooltips: fills the first tooltip line without leaving the tooltip frame.
        register(new Glyph("tessera_banner", '\uE002', "tessera:font/tessera_banner.png", 11, 16, 128, null));
        // Pause menu title (y = 40): sits between the title slot and the first button.
        register(new Glyph("pause_banner", '\uE003', "tessera:font/pause_banner.png", 16, 28, 224, null));
        register(new Glyph("tessera_status_bar", '\uE004', "tessera:font/tessera_status_bar.png", 10, 12, 90, null));
        register(new Glyph("ruby", '\uE005', "tessera:font/ruby.png", 8, 8, 8, null));
        register(new Glyph("heart", '\uE006', "tessera:font/heart.png", 8, 8, 8, null));
        register(new Glyph("mana", '\uE007', "tessera:font/mana.png", 8, 8, 8, null));
        // Bossbar title (y = 3): starts at the top edge of the screen, over the hidden bar.
        register(new Glyph("top_banner", '\uE008', "tessera:font/pause_banner.png", 10, 22, 176, null));
    }

    private void loadFromDirectory(String dirName) {
        for (YamlConfiguration config : plugin.getConfigManager().getConfigsInDirectory(dirName).values()) {
            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section != null) {
                    register(parseGlyph(key, section));
                }
            }
        }
    }

    private Glyph parseGlyph(String id, ConfigurationSection section) {
        char c;
        if (section.contains("character")) {
            String charStr = section.getString("character", "");
            c = parseChar(charStr);
        } else {
            Glyph existing = glyphs.get(id);
            c = existing != null ? existing.getCharacter() : allocateNextChar();
        }

        String texture = section.getString("texture", "tessera:font/" + id + ".png");
        int ascent = section.getInt("ascent", 8);
        int height = section.getInt("height", 8);
        int width = section.getInt("width", 8);

        return new Glyph(id, c, texture, ascent, height, width, section);
    }

    private char parseChar(String charStr) {
        if (charStr == null || charStr.isEmpty()) return allocateNextChar();
        if (charStr.startsWith("\\u") && charStr.length() >= 6) {
            try {
                return (char) Integer.parseInt(charStr.substring(2, 6), 16);
            } catch (NumberFormatException ignored) {}
        }
        return charStr.charAt(0);
    }

    private synchronized char allocateNextChar() {
        char allocated = nextChar;
        nextChar = (char) (nextChar + 1);
        return allocated;
    }

    public void register(@NotNull Glyph glyph) {
        glyphs.put(glyph.getId(), glyph);
        byChar.put(glyph.getCharacter(), glyph);
    }

    public void unregister(@NotNull String id) {
        Glyph g = glyphs.remove(id);
        if (g != null) {
            byChar.remove(g.getCharacter());
        }
    }

    @Nullable
    public Glyph get(@NotNull String id) {
        return glyphs.get(id);
    }

    @Nullable
    public Glyph getByChar(char character) {
        return byChar.get(character);
    }

    public boolean exists(@NotNull String id) {
        return glyphs.containsKey(id);
    }

    @NotNull
    public Collection<Glyph> getAll() {
        return Collections.unmodifiableCollection(glyphs.values());
    }

    @NotNull
    public Set<String> getIds() {
        return Collections.unmodifiableSet(glyphs.keySet());
    }

    public String generateFontJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"providers\": [\n");
        int index = 0;
        int size = glyphs.size();
        for (Glyph g : glyphs.values()) {
            String texture = g.getTexture();
            if (!texture.contains(":")) {
                texture = "tessera:" + texture;
            }
            if (!texture.endsWith(".png")) {
                texture = texture + ".png";
            }
            sb.append("    {\n");
            sb.append("      \"type\": \"bitmap\",\n");
            sb.append("      \"file\": \"").append(texture).append("\",\n");
            sb.append("      \"ascent\": ").append(g.getAscent()).append(",\n");
            sb.append("      \"height\": ").append(g.getHeight()).append(",\n");
            sb.append("      \"chars\": [\"\\u").append(String.format("%04X", (int) g.getCharacter())).append("\"]\n");
            sb.append("    }");
            if (++index < size) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ]\n}");
        return sb.toString();
    }
}
