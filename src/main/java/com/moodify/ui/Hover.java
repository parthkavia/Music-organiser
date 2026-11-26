package com.moodify.ui;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Tracks the mouse hovering over a component and eases a value 0..1 for smooth hover transitions. */
final class Hover {
    private final JComponent c;
    private final Timer timer;
    private float t;
    private boolean over;

    Hover(JComponent c) {
        this.c = c;
        this.timer = new Timer(15, e -> {
            float target = over ? 1f : 0f;
            t += (target - t) * 0.35f;
            if (Math.abs(target - t) < 0.02f) {
                t = target;
                ((Timer) e.getSource()).stop();
            }
            c.repaint();
        });
        c.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { set(true); }
            @Override public void mouseExited(MouseEvent e) { set(false); }
        });
    }

    private void set(boolean value) {
        over = value;
        if (!timer.isRunning()) timer.start();
    }

    /** 0 = not hovered, 1 = fully hovered. */
    float value() { return t; }

    boolean isOver() { return over; }
}
