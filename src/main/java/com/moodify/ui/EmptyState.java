package com.moodify.ui;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

/** A friendly centred message shown instead of an empty list. */
public final class EmptyState extends JPanel {
    private String title = "";
    private String subtitle = "";
    private final PillButton action;
    private final GridBagConstraints c = new GridBagConstraints();

    public EmptyState(PillButton action) {
        super(new GridBagLayout());
        this.action = action;
        setOpaque(false);
        if (action != null) {
            c.gridx = 0;
            c.gridy = 0;
            c.insets = new java.awt.Insets(150, 0, 0, 0);
            add(action, c);
        }
    }

    public void set(String title, String subtitle, boolean showAction) {
        this.title = title;
        this.subtitle = subtitle;
        if (action != null) action.setVisible(showAction);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        UiKit.antialias(g);
        int cx = getWidth() / 2;
        int top = Math.max(20, getHeight() / 2 - (action != null && action.isVisible() ? 120 : 70));
        Glyph.NOTE.paint(g, cx - 28, top, 56, Theme.alpha(Theme.TEXT_FAINT, 150));
        g.setFont(Theme.bold(18));
        g.setColor(Theme.TEXT);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, cx - fm.stringWidth(title) / 2, top + 100);
        g.setFont(Theme.regular(13));
        g.setColor(Theme.TEXT_DIM);
        fm = g.getFontMetrics();
        g.drawString(subtitle, cx - fm.stringWidth(subtitle) / 2, top + 126);
        g.dispose();
    }

    @Override
    public Dimension getMinimumSize() { return new Dimension(200, 260); }
}
