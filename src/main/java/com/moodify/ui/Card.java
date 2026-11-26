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
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/** A clickable music card (song or playlist) with cover artwork and a hover play button. */
final class Card extends JComponent {
    static final int WIDTH = 168, HEIGHT = 216;

    private final String title;
    private final String subtitle;
    private final boolean missing;
    private final boolean newTile;
    private final Hover hover;

    Card(String title, String subtitle, boolean missing, boolean newTile, Runnable onClick) {
        this.title = title;
        this.subtitle = subtitle;
        this.missing = missing;
        this.newTile = newTile;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setMinimumSize(new Dimension(WIDTH, HEIGHT));
        setMaximumSize(new Dimension(WIDTH, HEIGHT));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(title);
        hover = new Hover(this);
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) onClick.run();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        UiKit.antialias(g);
        int w = getWidth(), h = getHeight();
        float t = hover.value();
        g.setColor(Theme.mix(Theme.PANEL, Theme.ELEVATED, t));
        g.fill(new RoundRectangle2D.Double(0, 0, w, h, 20, 20));
        int art = w - 24;
        if (newTile) {
            g.setColor(Theme.mix(Theme.ELEVATED, Theme.HOVER, t));
            g.fill(new RoundRectangle2D.Double(12, 12, art, art, 14, 14));
            g.setColor(Theme.mix(Theme.TEXT_FAINT, Theme.ACCENT_HOVER, t));
            Glyph.PLUS.paint(g, 12 + art / 2.0 - 22, 12 + art / 2.0 - 22, 44, g.getColor());
        } else {
            g.drawImage(CoverArt.get(art, 14), 12, 12, null);
            if (missing) {
                g.setColor(Theme.alpha(Color.BLACK, 150));
                g.fill(new RoundRectangle2D.Double(12, 12, art, art, 28, 28));
                Glyph.WARNING.paint(g, 12 + art / 2.0 - 14, 12 + art / 2.0 - 14, 28, Theme.DANGER);
            } else if (t > 0.01f) {
                double d = 44;
                double y = 12 + art - d - 8 + (1 - t) * 8;
                g.setColor(Theme.alpha(Theme.ACCENT, (int) (255 * t)));
                g.fill(new Ellipse2D.Double(12 + art - d - 8, y, d, d));
                Glyph.PLAY.paint(g, 12 + art - d - 8 + 11, y + 11, 22, Theme.alpha(Color.WHITE, (int) (255 * t)));
            }
        }
        g.setFont(Theme.bold(13));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Theme.TEXT);
        g.drawString(UiKit.ellipsize(fm, title, w - 24), 12, 12 + art + 24);
        g.setFont(Theme.regular(12));
        fm = g.getFontMetrics();
        g.setColor(Theme.TEXT_DIM);
        g.drawString(UiKit.ellipsize(fm, subtitle, w - 24), 12, 12 + art + 43);
        g.dispose();
    }
}
