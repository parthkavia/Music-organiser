package com.moodify.ui;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

/** A rounded text field with a placeholder and an optional leading icon. */
public class StyledField extends JTextField {
    private final String placeholder;
    private final Glyph leading;
    private boolean focused;

    public StyledField(String placeholder, Glyph leading) {
        this.placeholder = placeholder;
        this.leading = leading;
        setOpaque(false);
        setForeground(Theme.TEXT);
        setCaretColor(Theme.TEXT);
        setSelectionColor(Theme.ACCENT);
        setSelectedTextColor(Color.WHITE);
        setFont(Theme.regular(13));
        setBorder(BorderFactory.createEmptyBorder(0, leading != null ? 38 : 14, 0, 14));
        setPreferredSize(new Dimension(260, 38));
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
            @Override public void focusLost(FocusEvent e) { focused = false; repaint(); }
        });
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g.setColor(Theme.ELEVATED);
        g.fill(new RoundRectangle2D.Double(0, 0, w, h, h, h));
        g.setColor(focused ? Theme.ACCENT : Theme.BORDER);
        g.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, h, h));
        if (leading != null) leading.paint(g, 13, (h - 18) / 2.0, 18, Theme.TEXT_DIM);
        if (getText().isEmpty() && placeholder != null) {
            g.setFont(getFont());
            g.setColor(Theme.TEXT_FAINT);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(placeholder, getInsets().left, (h - fm.getHeight()) / 2 + fm.getAscent());
        }
        g.dispose();
        super.paintComponent(g0);
    }
}
