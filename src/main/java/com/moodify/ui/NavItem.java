package com.moodify.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/** A row in the sidebar (navigation entry or playlist shortcut). */
final class NavItem extends JComponent {
    private final Glyph glyph;
    private final String text;
    private final int iconSize;
    private boolean selected;
    private final Hover hover;

    NavItem(Glyph glyph, String text, int height, int iconSize, Runnable onClick) {
        this.glyph = glyph;
        this.text = text;
        this.iconSize = iconSize;
        setPreferredSize(new Dimension(200, height));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        setMinimumSize(new Dimension(100, height));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(text);
        hover = new Hover(this);
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (e.getButton() == MouseEvent.BUTTON1) onClick.run(); }
        });
    }

    void setSelected(boolean s) {
        if (selected != s) {
            selected = s;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        UiKit.antialias(g);
        int w = getWidth(), h = getHeight();
        float t = hover.value();
        if (selected) {
            g.setColor(Theme.ELEVATED);
            g.fill(new RoundRectangle2D.Double(0, 0, w, h, 14, 14));
            g.setColor(Theme.ACCENT);
            g.fill(new RoundRectangle2D.Double(0, h / 2.0 - 11, 4, 22, 4, 4));
        } else if (t > 0.01f) {
            g.setColor(Theme.alpha(Theme.ELEVATED, (int) (200 * t)));
            g.fill(new RoundRectangle2D.Double(0, 0, w, h, 14, 14));
        }
        Color c = selected ? Theme.TEXT : Theme.mix(Theme.TEXT_DIM, Theme.TEXT, t);
        glyph.paint(g, 14, (h - iconSize) / 2.0, iconSize, selected ? Theme.ACCENT_HOVER : c);
        g.setFont(selected ? Theme.bold(14) : Theme.regular(14));
        g.setColor(c);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(UiKit.ellipsize(fm, text, w - 14 - iconSize - 24), 14 + iconSize + 12, (h - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
    }
}
