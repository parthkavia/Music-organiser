package com.moodify.ui;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/** A rounded text button with an optional icon and smooth hover colours. */
public class PillButton extends JButton {
    public enum Style { PRIMARY, SECONDARY, GHOST, DANGER }

    private final Style style;
    private Glyph glyph;
    private final Hover hover;
    private int radius = -1; // -1 = fully rounded

    public PillButton(String text, Style style) { this(text, null, style); }

    public PillButton(String text, Glyph glyph, Style style) {
        super(text);
        this.style = style;
        this.glyph = glyph;
        setFont(Theme.bold(13));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        hover = new Hover(this);
    }

    public PillButton radius(int r) {
        this.radius = r;
        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int w = fm.stringWidth(getText()) + 36 + (glyph != null ? 24 : 0);
        return new Dimension(w, 38);
    }

    @Override
    public Dimension getMinimumSize() { return getPreferredSize(); }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        float t = hover.value();
        double arc = radius < 0 ? h : radius * 2.0;
        Color fill;
        Color fg;
        Color border = null;
        switch (style) {
            case PRIMARY -> {
                fill = Theme.mix(Theme.ACCENT, Theme.ACCENT_HOVER, t);
                fg = Color.WHITE;
            }
            case DANGER -> {
                fill = Theme.mix(new Color(0xB91C1C), new Color(0xDC2626), t);
                fg = Color.WHITE;
            }
            case SECONDARY -> {
                fill = Theme.mix(Theme.ELEVATED, Theme.HOVER, t);
                fg = Theme.TEXT;
                border = Theme.mix(Theme.BORDER, Theme.TEXT_FAINT, t);
            }
            default -> {
                fill = Theme.alpha(Theme.HOVER, (int) (255 * t));
                fg = Theme.mix(Theme.TEXT_DIM, Theme.TEXT, t);
            }
        }
        if (!isEnabled()) {
            fill = Theme.alpha(fill, 110);
            fg = Theme.alpha(fg, 120);
        }
        g.setColor(fill);
        g.fill(new RoundRectangle2D.Double(0, 0, w, h, arc, arc));
        if (border != null) {
            g.setColor(border);
            g.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, arc, arc));
        }
        g.setFont(getFont());
        FontMetrics fm = g.getFontMetrics();
        int textW = fm.stringWidth(getText());
        int iconW = glyph != null ? 18 + 6 : 0;
        int x = (w - textW - iconW) / 2;
        if (glyph != null) {
            glyph.paint(g, x, (h - 18) / 2.0, 18, fg);
            x += iconW;
        }
        g.setColor(fg);
        g.drawString(getText(), x, (h - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
    }
}
