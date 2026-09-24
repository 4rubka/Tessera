package com.prisma.tessera.fonts;

import com.prisma.tessera.TesseraPlugin;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

public final class FontManager {

    private static final Pattern GLYPH_TAG_PATTERN = Pattern.compile("<glyph:([a-zA-Z0-9_]+)>");
    private static final Pattern COLON_GLYPH_PATTERN = Pattern.compile(":([a-zA-Z0-9_]+):");

    private static FontManager instance;

    private final TesseraPlugin plugin;
    private final GlyphRegistry glyphRegistry;

    public FontManager(@NotNull TesseraPlugin plugin, @NotNull GlyphRegistry glyphRegistry) {
        this.plugin = plugin;
        this.glyphRegistry = glyphRegistry;
        instance = this;
    }

    public static FontManager get() {
        return instance;
    }

    public GlyphRegistry getGlyphRegistry() {
        return glyphRegistry;
    }

    private Character getDefaultChar(String id) {
        return switch (id.toLowerCase(java.util.Locale.ROOT)) {
            case "tessera_badge" -> '\uE001';
            case "tessera_banner" -> '\uE002';
            case "pause_banner" -> '\uE003';
            case "tessera_status_bar" -> '\uE004';
            case "ruby" -> '\uE005';
            case "heart" -> '\uE006';
            case "mana" -> '\uE007';
            case "top_banner" -> '\uE008';
            default -> null;
        };
    }

    @NotNull
    public String replaceGlyphs(@NotNull String input) {
        return replace(input, false);
    }

    /**
     * Replaces :id: and <glyph:id> with the glyph character. With {@code noShadow} each glyph is
     * wrapped in a transparent shadow tag, because the client otherwise draws a dark copy of the
     * image one pixel down and right, which looks like a doubled edge on banners.
     */
    private String replace(String input, boolean noShadow) {
        if (input.isEmpty()) {
            return input;
        }
        String result = replacePattern(input, GLYPH_TAG_PATTERN, noShadow);
        return replacePattern(result, COLON_GLYPH_PATTERN, noShadow);
    }

    private String replacePattern(String input, Pattern pattern, boolean noShadow) {
        Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String id = matcher.group(1);
            Glyph glyph = glyphRegistry != null ? glyphRegistry.get(id) : null;
            Character character = glyph != null ? Character.valueOf(glyph.getCharacter()) : getDefaultChar(id);
            String replacement;
            if (character == null) {
                replacement = matcher.group(0);
            } else if (noShadow) {
                replacement = "<shadow:#00000000>" + character + "</shadow>";
            } else {
                replacement = String.valueOf(character);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @NotNull
    public Component format(@NotNull String miniMessage) {
        return MiniMessage.miniMessage().deserialize(replace(miniMessage, true));
    }
}
