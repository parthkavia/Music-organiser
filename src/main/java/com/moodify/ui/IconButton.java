package com.moodify.ui;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

/** A flat button that paints a vector {@link Glyph}, with a smooth hover transition. */
public class IconButton extends JButton {
    private Glyph glyph;
    private final int iconSize;
    private Color normal = Theme.TEXT_DIM;
    private Color hoverColor = Theme.TEXT;
    private Color circleColor;          // when set, the glyph sits on a filled circle (e.g. the play button)
    private Color circleHover;
    private Color glyphOnCircle = Color.BLACK;
    private boolean active;             // toggled-on state (accent colour)
    private final Hover hover;

    public IconButton(Glyph glyph, int iconSize, int boxSize, String tooltip) {
        this.glyph = glyph;
        this.iconSize = iconSize;
        setPreferredSize(new Dimension(boxSize, boxSize));
        setMinimumSize(new Dimension(boxSize, boxSize));
        setMaximumSize(new Dimension(boxSize, boxSize));
        setToolTipText(tooltip);
        getAccessibleContext().setAccessibleName(tooltip);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        hover = new Hover(this);
    }

    public IconButton circle(Color fill, Color fillHover, Color glyphColor) {
        this.circleColor = fill;
        this.circleHover = fillHover;
        this.glyphOnCircle = glyphColor;
        return this;
    }

    public IconButton colors(Color normal, Color hover) {
        this.normal = normal;
        this.hoverColor = hover;
        return this;
    }

    public void setGlyph(Glyph g) {
        this.glyph = g;
        repaint();
    }

    public void setActive(boolean active) {
        this.active = active;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        float t = hover.value();
        Color color;
        if (circleColor != null) {
            double scale = 1 + 0.06 * t;
            double d = Math.min(w, h) * scale;
            g.setColor(Theme.mix(circleColor, circleHover == null ? circleColor : circleHover, t));
            g.fill(new Ellipse2D.Double((w - d) / 2, (h - d) / 2, d, d));
            color = glyphOnCircle;
        } else {
            Color base = active ? Theme.ACCENT_HOVER : normal;
            color = Theme.mix(base, active ? Color.WHITE : hoverColor, t);
        }
        if (!isEnabled()) color = Theme.alpha(color, 90);
        glyph.paint(g, (w - iconSize) / 2.0, (h - iconSize) / 2.0, iconSize, color);
        if (active && circleColor == null) {
            g.setColor(Theme.ACCENT_HOVER);
            g.fill(new Ellipse2D.Double(w / 2.0 - 2, h - 6, 4, 4));
        }
        g.dispose();
    }
}
