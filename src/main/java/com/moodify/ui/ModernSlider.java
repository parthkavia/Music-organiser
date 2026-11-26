package com.moodify.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/** A thin horizontal slider (value 0..1) used for playback progress and volume. Click or drag to change. */
public class ModernSlider extends JComponent {
    public interface Listener {
        /** @param adjusting true while the user is dragging, false on release */
        void changed(double value, boolean adjusting);
    }

    private double value;
    private boolean dragging;
    private Listener listener;
    private final Hover hover;

    public ModernSlider() {
        setPreferredSize(new Dimension(120, 18));
        setMinimumSize(new Dimension(40, 18));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        hover = new Hover(this);
        MouseAdapter m = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (!isEnabled()) return;
                dragging = true;
                update(e, true);
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (dragging) update(e, true);
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (!dragging) return;
                update(e, false);
                dragging = false;
                repaint();
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    public void setListener(Listener l) { this.listener = l; }

    public double getValue() { return value; }

    public boolean isDragging() { return dragging; }

    /** Programmatic update (ignored while the user is dragging so the thumb doesn't jump). */
    public void setValue(double v) {
        if (dragging) return;
        double nv = Math.max(0, Math.min(1, v));
        if (nv != value) {
            value = nv;
            repaint();
        }
    }

    private void update(MouseEvent e, boolean adjusting) {
        int pad = 7;
        double span = Math.max(1, getWidth() - pad * 2);
        value = Math.max(0, Math.min(1, (e.getX() - pad) / span));
        repaint();
        if (listener != null) listener.changed(value, adjusting);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int pad = 7;
        int w = getWidth(), cy = getHeight() / 2;
        double trackH = 4 + 2 * hover.value();
        double span = w - pad * 2;
        g.setColor(isEnabled() ? new Color(0x3A3A40) : new Color(0x2A2A2E));
        g.fill(new RoundRectangle2D.Double(pad, cy - trackH / 2, span, trackH, trackH, trackH));
        double fillW = span * value;
        Color fill = Theme.mix(Color.WHITE, Theme.ACCENT_HOVER, hover.value() + (dragging ? 1 : 0));
        g.setColor(isEnabled() ? fill : new Color(0x55555B));
        g.fill(new RoundRectangle2D.Double(pad, cy - trackH / 2, Math.max(fillW, 0), trackH, trackH, trackH));
        float t = dragging ? 1f : hover.value();
        if (t > 0.02f && isEnabled()) {
            double d = 12 * t;
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(pad + fillW - d / 2, cy - d / 2, d, d));
        }
        g.dispose();
    }
}
