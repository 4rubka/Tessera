package com.prisma.tessera.pack;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.prisma.tessera.TesseraPlugin;
import com.prisma.tessera.fonts.Glyph;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

public final class PackGenerator {

    /** Every language code the client ships. The pause title must be overridden in each one. */
    private static final String[] LOCALES = (
            "af_za ar_sa ast_es az_az ba_ru bar be_by be_latn bg_bg br_fr brb bs_ba ca_es cs_cz cv_cu cy_gb "
            + "da_dk de_at de_ch de_de el_gr en_au en_ca en_gb en_nz en_pt en_ud en_us enp enws eo_uy es_ar es_cl "
            + "es_ec es_es es_mx es_uy es_ve esan et_ee eu_es fa_ir fi_fi fil_ph fo_fo fr_ca fr_ch fr_fr fra_de "
            + "fur_it fy_nl ga_ie gd_gb gl_es go_fr got_de hal_ua haw_us he_il hi_in hn_no hr_hr hu_hu hy_am id_id "
            + "ig_ng io_en is_is isv it_it ja_jp jbo_en ka_ge kk_kz kn_in ko_kr ksh kw_gb ky_kg la_la lb_lu li_li "
            + "lmo lo_la lol_us lt_lt lv_lv lzh mk_mk mn_mn ms_my mt_mt nah nds_de nl_be nl_nl nn_no no_no oc_fr "
            + "ovd pl_pl pls pt_br pt_pt qcb_es qid qya_aa ro_ro rpr ru_ru ry_ua sah_sah se_no sk_sk sl_si so_so "
            + "sq_al sr_cs sr_sp sv_se sxu szl ta_in th_th tl_ph tlh_aa tok tr_tr tt_ru tzo_mx uk_ua uz_uz val_es "
            + "vec_it vi_vn vp_vl vro yi_de yo_ng zh_cn zh_hk zh_tw zlm_arab").split(" ");

    private static final String PAUSE_TITLE_KEY = "menu.game";
    private static final String[] BOSS_BAR_COLORS = {"pink", "blue", "red", "green", "yellow", "purple", "white"};

    private final TesseraPlugin plugin;

    public PackGenerator(TesseraPlugin plugin) {
        this.plugin = plugin;
    }

    public void generate() {
        File packDir = new File(plugin.getDataFolder(), "pack");
        if (!packDir.exists()) {
            packDir.mkdirs();
        }
        File assetsDir = new File(packDir, "assets/minecraft");
        if (!assetsDir.exists()) {
            assetsDir.mkdirs();
        }

        generatePackPng(new File(packDir, "pack.png"));
        generateDefaultTextures(packDir);
        writePackMeta(packDir);
        writeFontJson(packDir);
        writePauseTitle(packDir);
        writeBossBarSprites(packDir);
        zipPack(packDir);

        File zipFile = new File(plugin.getDataFolder(), "tessera-pack.zip");
        if (zipFile.exists()) {
            if (plugin.getPackProtector() != null) {
                plugin.getPackProtector().protect(zipFile);
            }
            if (plugin.getPackServer() != null) {
                plugin.getPackServer().updatePack(zipFile);
            }
        }
    }

    private void generateDefaultTextures(File packDir) {
        File mcFontDir = new File(packDir, "assets/minecraft/textures/font");
        File tesseraFontDir = new File(packDir, "assets/tessera/textures/font");
        mcFontDir.mkdirs();
        tesseraFontDir.mkdirs();

        generateBannerPng(new File(mcFontDir, "tessera_banner.png"));
        generateBannerPng(new File(tesseraFontDir, "tessera_banner.png"));

        generatePauseBannerPng(new File(mcFontDir, "pause_banner.png"));
        generatePauseBannerPng(new File(tesseraFontDir, "pause_banner.png"));

        generateBadgePng(new File(mcFontDir, "tessera_badge.png"));
        generateBadgePng(new File(tesseraFontDir, "tessera_badge.png"));

        generateStatusBarPng(new File(mcFontDir, "tessera_status_bar.png"));
        generateStatusBarPng(new File(tesseraFontDir, "tessera_status_bar.png"));

        generateRubyPng(new File(mcFontDir, "ruby.png"));
        generateRubyPng(new File(tesseraFontDir, "ruby.png"));

        generateHeartPng(new File(mcFontDir, "heart.png"));
        generateHeartPng(new File(tesseraFontDir, "heart.png"));

        generateManaPng(new File(mcFontDir, "mana.png"));
        generateManaPng(new File(tesseraFontDir, "mana.png"));
    }

    private BufferedImage getBadgeTexture() {
        try (var in = plugin.getResource("assets/minecraft/textures/font/tessera_badge.png")) {
            if (in != null) {
                return ImageIO.read(in);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void generateBannerPng(File file) {
        if (file.exists()) return;
        try {
            int width = 256;
            int height = 32;
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.setColor(new Color(14, 18, 26, 235));
            g.fillRoundRect(2, 2, width - 4, height - 4, 10, 10);

            GradientPaint gp = new GradientPaint(0, 0, new Color(0, 198, 255, 30), width, height, new Color(0, 114, 255, 10));
            g.setPaint(gp);
            g.fillRoundRect(3, 3, width - 6, height - 6, 8, 8);

            g.setColor(new Color(0, 198, 255, 180));
            g.setStroke(new BasicStroke(1.2f));
            g.drawRoundRect(2, 2, width - 4, height - 4, 10, 10);

            BufferedImage badge = getBadgeTexture();
            if (badge != null) {
                g.drawImage(badge, 6, 4, 24, 24, null);
                g.drawImage(badge, width - 30, 4, 24, 24, null);
            } else {
                drawDiamond(g, 16, 16, 7, new Color(0, 198, 255, 220));
                drawDiamond(g, 16, 16, 3, new Color(255, 255, 255, 240));
                drawDiamond(g, width - 16, 16, 7, new Color(0, 198, 255, 220));
                drawDiamond(g, width - 16, 16, 3, new Color(255, 255, 255, 240));
            }

            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(new Color(255, 255, 255, 240));
            String title = "✦  T E S S E R A  ✦";
            int titleWidth = g.getFontMetrics().stringWidth(title);
            g.drawString(title, (width - titleWidth) / 2, 21);

            g.dispose();
            ImageIO.write(img, "PNG", file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write banner texture: " + e.getMessage());
        }
    }

    private void generatePauseBannerPng(File file) {
        if (file.exists()) return;
        try {
            int width = 256;
            int height = 32;
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(new Color(10, 14, 22, 245));
            g.fillRoundRect(4, 2, width - 8, height - 4, 12, 12);

            g.setColor(new Color(0, 210, 255, 200));
            g.drawLine(20, 3, width - 20, 3);

            BufferedImage badge = getBadgeTexture();
            if (badge != null) {
                g.drawImage(badge, 8, 4, 24, 24, null);
                g.drawImage(badge, width - 32, 4, 24, 24, null);
            }

            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(new Color(220, 235, 255, 230));
            String label = "T E S S E R A   E N G I N E";
            int sw = g.getFontMetrics().stringWidth(label);
            g.drawString(label, (width - sw) / 2, 20);

            g.dispose();
            ImageIO.write(img, "PNG", file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write pause banner texture: " + e.getMessage());
        }
    }

    private void generateBadgePng(File file) {
        if (file.exists()) return;
        try (var in = plugin.getResource("assets/minecraft/textures/font/tessera_badge.png")) {
            if (in != null) {
                java.nio.file.Files.copy(in, file.toPath());
                return;
            }
        } catch (Exception ignored) {
        }
        try {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            drawDiamond(g, 8, 8, 7, new Color(0, 114, 255));
            drawDiamond(g, 8, 8, 5, new Color(0, 198, 255));
            drawDiamond(g, 8, 8, 2, new Color(255, 255, 255, 220));

            g.dispose();
            ImageIO.write(img, "PNG", file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write badge texture: " + e.getMessage());
        }
    }

    private void generateStatusBarPng(File file) {
        if (file.exists()) return;
        try {
            int width = 120;
            int height = 16;
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(new Color(15, 20, 30, 200));
            g.fillRoundRect(1, 1, width - 2, height - 2, 8, 8);
            g.setColor(new Color(0, 198, 255, 160));
            g.drawRoundRect(1, 1, width - 2, height - 2, 8, 8);

            BufferedImage badge = getBadgeTexture();
            if (badge != null) {
                g.drawImage(badge, 2, 2, 12, 12, null);
            }

            g.dispose();
            ImageIO.write(img, "PNG", file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write status bar texture: " + e.getMessage());
        }
    }

    private void generateRubyPng(File file) {
        if (file.exists()) return;
        try {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Polygon oct = new Polygon(
                    new int[]{5, 10, 14, 14, 10, 5, 1, 1},
                    new int[]{1, 1, 5, 10, 14, 14, 10, 5},
                    8
            );
            g.setColor(new Color(190, 10, 50));
            g.fillPolygon(oct);

            g.setColor(new Color(245, 45, 90));
            g.fillRect(5, 5, 6, 6);

            g.setColor(new Color(255, 220, 230));
            g.fillRect(4, 4, 2, 2);

            g.dispose();
            ImageIO.write(img, "PNG", file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write ruby texture: " + e.getMessage());
        }
    }

    private void generateHeartPng(File file) {
        if (file.exists()) return;
        try {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(new Color(230, 30, 60));
            g.fillOval(2, 2, 6, 6);
            g.fillOval(8, 2, 6, 6);
            Polygon tri = new Polygon(new int[]{2, 14, 8}, new int[]{6, 6, 14}, 3);
            g.fillPolygon(tri);

            g.setColor(new Color(255, 170, 185));
            g.fillRect(4, 4, 2, 2);

            g.dispose();
            ImageIO.write(img, "PNG", file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write heart texture: " + e.getMessage());
        }
    }

    private void generateManaPng(File file) {
        if (file.exists()) return;
        try {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            drawDiamond(g, 8, 8, 6, new Color(0, 140, 255));
            drawDiamond(g, 8, 8, 4, new Color(0, 230, 255));
            g.setColor(Color.WHITE);
            g.fillRect(7, 5, 2, 2);

            g.dispose();
            ImageIO.write(img, "PNG", file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write mana texture: " + e.getMessage());
        }
    }

    private void drawDiamond(Graphics2D g, int cx, int cy, int radius, Color color) {
        Polygon p = new Polygon(
                new int[]{cx, cx + radius, cx, cx - radius},
                new int[]{cy - radius, cy, cy + radius, cy},
                4
        );
        g.setColor(color);
        g.fillPolygon(p);
    }

    /**
     * Puts the pause_banner glyph in place of the pause menu title ("Game Menu") in every language.
     * Existing lang files in the pack folder are merged, not replaced.
     */
    private void writePauseTitle(File packDir) {
        boolean enabled = plugin.getConfig().getBoolean("hud.pause_banner", true);
        Glyph glyph = plugin.getGlyphRegistry() != null ? plugin.getGlyphRegistry().get("pause_banner") : null;
        String banner = glyph != null ? String.valueOf(glyph.getCharacter()) : null;

        File langDir = new File(packDir, "assets/minecraft/lang");
        for (String locale : LOCALES) {
            File file = new File(langDir, locale + ".json");
            try {
                JsonObject json = file.exists()
                        ? JsonParser.parseString(Files.readString(file.toPath(), StandardCharsets.UTF_8)).getAsJsonObject()
                        : new JsonObject();
                if (enabled && banner != null) {
                    json.addProperty(PAUSE_TITLE_KEY, banner);
                } else if (json.has(PAUSE_TITLE_KEY)) {
                    json.remove(PAUSE_TITLE_KEY);
                } else {
                    continue;
                }
                if (json.size() == 0) {
                    Files.deleteIfExists(file.toPath());
                    continue;
                }
                langDir.mkdirs();
                Files.writeString(file.toPath(), new GsonBuilder().disableHtmlEscaping().create().toJson(json), StandardCharsets.UTF_8);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to write pause title for " + locale + ": " + e.getMessage());
            }
        }
    }

    /**
     * Hides the bar under the top banner by replacing that color's boss bar sprites with transparent
     * ones. Transparent sprites left over from a previous color are removed first; custom sprites are kept.
     */
    private void writeBossBarSprites(File packDir) {
        File spriteDir = new File(packDir, "assets/minecraft/textures/gui/sprites/boss_bar");
        for (String color : BOSS_BAR_COLORS) {
            for (String part : new String[]{"_background.png", "_progress.png"}) {
                File file = new File(spriteDir, color + part);
                if (file.exists() && isFullyTransparent(file)) {
                    file.delete();
                }
            }
        }

        boolean bannerOn = plugin.getConfig().getBoolean("hud.top_banner.enabled", true);
        boolean hideBar = plugin.getConfig().getBoolean("hud.top_banner.hide_bar", true);
        if (!bannerOn || !hideBar) {
            return;
        }

        String color = plugin.getConfig().getString("hud.top_banner.color", "BLUE").toLowerCase(Locale.ROOT);
        if (!java.util.Arrays.asList(BOSS_BAR_COLORS).contains(color)) {
            return;
        }
        spriteDir.mkdirs();
        BufferedImage empty = new BufferedImage(182, 5, BufferedImage.TYPE_INT_ARGB);
        for (String part : new String[]{"_background.png", "_progress.png"}) {
            File file = new File(spriteDir, color + part);
            if (file.exists()) {
                continue;
            }
            try {
                ImageIO.write(empty, "PNG", file);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to write boss bar sprite " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    private static boolean isFullyTransparent(File file) {
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null || !img.getColorModel().hasAlpha()) {
                return false;
            }
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    if ((img.getRGB(x, y) >>> 24) != 0) {
                        return false;
                    }
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void writeFontJson(File packDir) {
        if (plugin.getGlyphRegistry() == null) {
            return;
        }
        String fontJson = plugin.getGlyphRegistry().generateFontJson();
        if (fontJson == null || fontJson.isEmpty()) {
            return;
        }

        File mcFontDir = new File(packDir, "assets/minecraft/font");
        mcFontDir.mkdirs();
        try {
            java.nio.file.Files.writeString(new File(mcFontDir, "default.json").toPath(), fontJson);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write minecraft font default.json: " + e.getMessage());
        }

        File tesseraFontDir = new File(packDir, "assets/tessera/font");
        tesseraFontDir.mkdirs();
        try {
            java.nio.file.Files.writeString(new File(tesseraFontDir, "default.json").toPath(), fontJson);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write tessera font default.json: " + e.getMessage());
        }
    }

    public void generatePackPng(File file) {
        if (file.exists()) return;
        try {
            int size = 128;
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.setColor(new Color(11, 15, 23));
            g.fillRect(0, 0, size, size);

            GradientPaint bgGrad = new GradientPaint(0, 0, new Color(0, 198, 255, 45), size, size, new Color(0, 114, 255, 20));
            g.setPaint(bgGrad);
            g.fillRect(0, 0, size, size);

            g.setColor(new Color(0, 198, 255, 220));
            g.setStroke(new BasicStroke(2.0f));
            g.drawRoundRect(4, 4, size - 8, size - 8, 16, 16);

            g.setColor(new Color(0, 114, 255, 120));
            g.setStroke(new BasicStroke(1.0f));
            g.drawRoundRect(7, 7, size - 14, size - 14, 12, 12);

            int cx = 64, cy = 48;
            g.setColor(new Color(0, 198, 255, 50));
            drawDiamond(g, cx, cy, 26, new Color(0, 198, 255, 50));
            drawDiamond(g, cx, cy, 20, new Color(0, 114, 255));
            drawDiamond(g, cx, cy, 14, new Color(0, 198, 255));

            Polygon topFacet = new Polygon(
                new int[]{cx, cx + 10, cx, cx - 10},
                new int[]{cy - 14, cy, cy - 4, cy},
                4
            );
            g.setColor(new Color(180, 240, 255, 230));
            g.fillPolygon(topFacet);

            g.setColor(Color.WHITE);
            drawDiamond(g, cx, cy - 2, 4, Color.WHITE);

            g.setFont(new Font("SansSerif", Font.BOLD, 15));
            g.setColor(new Color(255, 255, 255, 245));
            String title = "TESSERA";
            int tw = g.getFontMetrics().stringWidth(title);
            g.drawString(title, (size - tw) / 2, 92);

            g.setFont(new Font("SansSerif", Font.BOLD, 9));
            g.setColor(new Color(0, 198, 255, 230));
            String sub = "✦ SERVER PACK ✦";
            int sw = g.getFontMetrics().stringWidth(sub);
            g.drawString(sub, (size - sw) / 2, 107);

            g.dispose();
            ImageIO.write(img, "PNG", file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to generate pack.png: " + e.getMessage());
        }
    }

    private void writePackMeta(File packDir) {
        File metaFile = new File(packDir, "pack.mcmeta");
        try {
            int packFormat = plugin.getConfig().getInt("pack.meta.pack_format",
                    plugin.getConfig().getInt("pack.pack_format", 34));
            String json = """
            {
              "pack": {
                "pack_format": %d,
                "supported_formats": {"min_inclusive": 34, "max_inclusive": 48},
                "description": [
                  {"text": "✦ ", "color": "#00c6ff", "bold": true},
                  {"text": "TESSERA ", "color": "#0072ff", "bold": true},
                  {"text": "OFFICIAL SERVER PACK", "color": "#00c6ff", "bold": true},
                  {"text": " ✦\\n", "color": "#00c6ff", "bold": true},
                  {"text": "❖ Custom 3D Items, Blocks, Audio & UI Engine", "color": "#8c9fb5", "italic": false}
                ]
              }
            }
            """.formatted(packFormat);
            java.nio.file.Files.writeString(metaFile.toPath(), json);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to write pack.mcmeta: " + e.getMessage());
        }
    }

    private void zipPack(File packDir) {
        File zipFile = new File(plugin.getDataFolder(), "tessera-pack.zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            addDirectoryToZip(zos, packDir, packDir.getName());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to generate pack: " + e.getMessage());
        }
    }

    private void addDirectoryToZip(ZipOutputStream zos, File dir, String basePath) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            String entryName = basePath.equals(dir.getName())
                ? file.getName()
                : basePath.substring(basePath.indexOf('/') + 1) + "/" + file.getName();
            if (file.isDirectory()) {
                addDirectoryToZip(zos, file, basePath + "/" + file.getName());
            } else {
                zos.putNextEntry(new ZipEntry(entryName));
                java.nio.file.Files.copy(file.toPath(), zos);
                zos.closeEntry();
            }
        }
    }
}
