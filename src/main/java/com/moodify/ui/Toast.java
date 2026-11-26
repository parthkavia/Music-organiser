package com.moodify.ui;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/** A small non-blocking message that floats above the content and fades away by itself. */
public final class Toast extends JComponent {
    private final JLayeredPane layer;
    private final Timer hideTimer;
    private String text = "";
    private boolean error;
    private final Font font = Theme.regular(13);

    public Toast(JLayeredPane layer) {
        this.layer = layer;
        setVisible(false);
        setOpaque(false);
        hideTimer = new Timer(3800, e -> setVisible(false));
        hideTimer.setRepeats(false);
        layer.add(this, JLayeredPane.POPUP_LAYER);
    }

    /** Safe to call from any thread. */
    public void show(String message, boolean isError) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            this.text = message;
            this.error = isError;
            FontMetrics fm = getFontMetrics(font);
            int maxW = Math.max(240, layer.getWidth() - 80);
            int w = Math.min(maxW, fm.stringWidth(message) + 56);
            int h = 44;
            setSize(new Dimension(w, h));
            setLocation((layer.getWidth() - w) / 2, layer.getHeight() - 132 - h);
            setVisible(true);
            layer.moveToFront(this);
            repaint();
            hideTimer.restart();
        });
    }

    /** Keeps the toast placed correctly when the window is resized. */
    public void reposition() {
        if (isVisible()) setLocation((layer.getWidth() - getWidth()) / 2, layer.getHeight() - 132 - getHeight());
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        UiKit.antialias(g);
        int w = getWidth(), h = getHeight();
        g.setColor(Theme.alpha(java.awt.Color.BLACK, 70));
        g.fill(new RoundRectangle2D.Double(2, 4, w - 4, h - 4, h, h));
        g.setColor(Theme.ELEVATED);
        g.fill(new RoundRectangle2D.Double(0, 0, w - 2, h - 2, h, h));
        g.setColor(error ? Theme.DANGER : Theme.ACCENT);
        g.fill(new java.awt.geom.Ellipse2D.Double(16, (h - 2) / 2.0 - 4, 8, 8));
        g.setFont(font);
        g.setColor(Theme.TEXT);
        FontMetrics fm = g.getFontMetrics();
        String shown = UiKit.ellipsize(fm, text, w - 56);
        g.drawString(shown, 34, (h - 2 - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
    }

    @Override
    public boolean contains(int x, int y) { return false; } // never steals mouse clicks
}
