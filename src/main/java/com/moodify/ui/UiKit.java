package com.moodify.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.Locale;

/** Small shared helpers for building the interface. */
public final class UiKit {
    private UiKit() {}

    /** mm:ss, or h:mm:ss for an hour or more. A non-positive value shows "--:--". */
    public static String formatTime(long ms) {
        if (ms <= 0) return "--:--";
        return formatClock(ms);
    }

    /** Like {@link #formatTime(long)} but shows 0:00 for zero (used for the playback position). */
    public static String formatClock(long ms) {
        long total = Math.max(0, ms) / 1000;
        long h = total / 3600, m = (total % 3600) / 60, s = total % 60;
        return h > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) : String.format(Locale.ROOT, "%d:%02d", m, s);
    }

    /** "3 h 12 min" / "42 min" style text for totals. */
    public static String formatTotal(long ms) {
        long mins = Math.round(ms / 60000.0);
        if (mins >= 60) return (mins / 60) + " h " + (mins % 60) + " min";
        return mins + " min";
    }

    public static String plural(int n, String one, String many) { return n + " " + (n == 1 ? one : many); }

    /** Cuts text with an ellipsis so it fits in {@code width} pixels. */
    public static String ellipsize(FontMetrics fm, String text, int width) {
        if (text == null) return "";
        if (width <= 0) return "";
        if (fm.stringWidth(text) <= width) return text;
        String dots = "\u2026";
        int end = text.length();
        while (end > 0 && fm.stringWidth(text.substring(0, end)) + fm.stringWidth(dots) > width) end--;
        return text.substring(0, Math.max(end, 0)) + dots;
    }

    public static JLabel label(String text, java.awt.Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    public static void antialias(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    /** Dark, thin scroll bars and no border. */
    public static JScrollPane styleScroll(JScrollPane sp) {
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setViewportBorder(null);
        sp.getViewport().setBackground(sp.getBackground());
        sp.getVerticalScrollBar().setUI(new SlimScrollBarUI());
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
        sp.getHorizontalScrollBar().setUI(new SlimScrollBarUI());
        sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 12));
        sp.getVerticalScrollBar().setUnitIncrement(24);
        sp.setCorner(JScrollPane.UPPER_RIGHT_CORNER, cornerFiller());
        return sp;
    }

    private static JComponent cornerFiller() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        return p;
    }

    private static final class SlimScrollBarUI extends BasicScrollBarUI {
        @Override protected void configureScrollBarColors() {
            thumbColor = new Color(0x3F3F46);
            trackColor = Theme.BG;
        }
        @Override protected JButton createDecreaseButton(int orientation) { return zero(); }
        @Override protected JButton createIncreaseButton(int orientation) { return zero(); }
        private JButton zero() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { /* invisible */ }
        @Override protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(isThumbRollover() || isDragging ? new Color(0x6B6B75) : thumbColor);
            boolean vertical = ((JScrollBar) c).getOrientation() == SwingConstants.VERTICAL;
            if (vertical) g.fill(new RoundRectangle2D.Double(r.x + 3, r.y + 2, r.width - 6, r.height - 4, 6, 6));
            else g.fill(new RoundRectangle2D.Double(r.x + 2, r.y + 3, r.width - 4, r.height - 6, 6, 6));
            g.dispose();
        }
    }

    /** A panel that always matches the width of its scroll pane (so content wraps instead of scrolling sideways). */
    public static class ScrollablePanel extends JPanel implements Scrollable {
        public ScrollablePanel() { setOpaque(true); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 28; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(60, r.height - 60); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport vp && getPreferredSize().height < vp.getHeight();
        }
    }

    /** FlowLayout that wraps components onto new rows when the container is too narrow. */
    public static class WrapLayout extends FlowLayout {
        public WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

        @Override public Dimension preferredLayoutSize(Container target) { return layoutSize(target, true); }

        @Override public Dimension minimumLayoutSize(Container target) {
            Dimension d = layoutSize(target, false);
            d.width -= getHgap() + 1;
            return d;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                Container container = target;
                while (container.getSize().width == 0 && container.getParent() != null) container = container.getParent();
                int targetWidth = container.getSize().width;
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
                int hgap = getHgap(), vgap = getVgap();
                Insets insets = target.getInsets();
                int horizontalInsetsAndGap = insets.left + insets.right + hgap * 2;
                int maxWidth = targetWidth - horizontalInsetsAndGap;
                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0, rowHeight = 0;
                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component m = target.getComponent(i);
                    if (!m.isVisible()) continue;
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                        addRow(dim, rowWidth, rowHeight);
                        rowWidth = 0;
                        rowHeight = 0;
                    }
                    if (rowWidth != 0) rowWidth += hgap;
                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
                addRow(dim, rowWidth, rowHeight);
                dim.width += horizontalInsetsAndGap;
                dim.height += insets.top + insets.bottom + vgap * 2;
                Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
                if (scrollPane != null && target.isValid()) dim.width -= hgap + 1;
                return dim;
            }
        }

        private void addRow(Dimension dim, int rowWidth, int rowHeight) {
            dim.width = Math.max(dim.width, rowWidth);
            if (dim.height > 0) dim.height += getVgap();
            dim.height += rowHeight;
        }
    }
}
