package com.moodify.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/** A panel with a rounded, optionally bordered background. */
public class RoundedPanel extends JPanel {
    private Color fill;
    private Color border;
    private final int radius;

    public RoundedPanel(LayoutManager layout, Color fill, int radius) {
        super(layout);
        this.fill = fill;
        this.radius = radius;
        setOpaque(false);
    }

    public RoundedPanel border(Color c) {
        this.border = c;
        return this;
    }

    public void setFill(Color c) {
        this.fill = c;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(fill);
        g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), radius * 2.0, radius * 2.0));
        if (border != null) {
            g.setColor(border);
            g.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, radius * 2.0, radius * 2.0));
        }
        g.dispose();
        super.paintComponent(g0);
    }
}
