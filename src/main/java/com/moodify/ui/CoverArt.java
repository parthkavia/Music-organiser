package com.moodify.ui;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** The one bundled cover image, scaled (and rounded) on demand and cached. */
public final class CoverArt {
    private CoverArt() {}

    private static BufferedImage source;
    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    private static synchronized BufferedImage source() {
        if (source != null) return source;
        try (InputStream in = CoverArt.class.getResourceAsStream("/images/cover.png")) {
            if (in != null) source = ImageIO.read(in);
        } catch (IOException ignored) {
            // fall through to the generated fallback
        }
        if (source == null) {
            // fallback: should never be needed, keeps the app usable if the resource is missing
            source = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = source.createGraphics();
            g.setPaint(new GradientPaint(0, 0, new Color(0x7C3AED), 512, 512, new Color(0xEC4899)));
            g.fillRect(0, 0, 512, 512);
            g.dispose();
        }
        return source;
    }

    /** A square cover, {@code size} px wide, with rounded corners of {@code radius} px. */
    public static synchronized BufferedImage get(int size, int radius) {
        String key = size + "/" + radius;
        BufferedImage cached = CACHE.get(key);
        if (cached != null) return cached;
        BufferedImage src = source();
        // step-down scaling keeps the result smooth
        BufferedImage cur = src;
        while (cur.getWidth() / 2 >= size * 2) cur = scale(cur, cur.getWidth() / 2);
        cur = scale(cur, size);
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setClip(new RoundRectangle2D.Double(0, 0, size, size, radius * 2.0, radius * 2.0));
        g.drawImage(cur, 0, 0, null);
        g.dispose();
        CACHE.put(key, out);
        return out;
    }

    public static ImageIcon icon(int size, int radius) { return new ImageIcon(get(size, radius)); }

    private static BufferedImage scale(BufferedImage in, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(in, 0, 0, size, size, null);
        g.dispose();
        return out;
    }
}
