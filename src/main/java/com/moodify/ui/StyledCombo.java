package com.moodify.ui;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/** A dark, rounded combo box. */
public class StyledCombo<T> extends JComboBox<T> {
    public StyledCombo(T[] items) {
        super(items);
        setUI(new Ui());
        setOpaque(false);
        setFont(Theme.regular(13));
        setForeground(Theme.TEXT);
        setBackground(Theme.ELEVATED);
        setFocusable(true);
        setMaximumRowCount(12);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(160, 38));
        setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
                setFont(Theme.regular(13));
                if (index < 0) { // the collapsed "current value" cell
                    setOpaque(false);
                    setForeground(Theme.TEXT);
                } else {
                    setOpaque(true);
                    setBackground(selected ? Theme.ACCENT : Theme.ELEVATED);
                    setForeground(selected ? Color.WHITE : Theme.TEXT);
                }
                return this;
            }
        });
    }

    private static final class Ui extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            JButton b = new JButton() {
                @Override
                protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(Theme.TEXT_DIM);
                    g.setStroke(new java.awt.BasicStroke(1.8f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                    Path2D p = new Path2D.Double();
                    double cx = getWidth() / 2.0, cy = getHeight() / 2.0;
                    p.moveTo(cx - 4, cy - 2);
                    p.lineTo(cx, cy + 2);
                    p.lineTo(cx + 4, cy - 2);
                    g.draw(p);
                    g.dispose();
                }
            };
            b.setBorder(BorderFactory.createEmptyBorder());
            b.setContentAreaFilled(false);
            b.setFocusPainted(false);
            b.setOpaque(false);
            return b;
        }

        @Override
        public void paint(Graphics g0, JComponent c) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Theme.ELEVATED);
            g.fill(new RoundRectangle2D.Double(0, 0, c.getWidth(), c.getHeight(), 14, 14));
            g.setColor(c.hasFocus() ? Theme.ACCENT : Theme.BORDER);
            g.draw(new RoundRectangle2D.Double(0.5, 0.5, c.getWidth() - 1, c.getHeight() - 1, 14, 14));
            g.dispose();
            super.paint(g0, c);
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) { /* transparent */ }

        @Override
        protected javax.swing.plaf.basic.ComboPopup createPopup() {
            javax.swing.plaf.basic.BasicComboPopup popup = new javax.swing.plaf.basic.BasicComboPopup(comboBox);
            popup.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
            popup.getList().setBackground(Theme.ELEVATED);
            return popup;
        }
    }
}
