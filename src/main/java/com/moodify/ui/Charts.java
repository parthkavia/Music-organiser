package com.moodify.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Simple Java2D charts for the Insights page. */
final class Charts {
    private Charts() {}

    record Entry(String label, int value, Color color) {}

    /** Horizontal bars with labels and counts. */
    static final class BarChart extends JComponent {
        private static final int ROW = 34;
        private final List<Entry> entries;
        private final String emptyText;

        BarChart(List<Entry> entries, String emptyText) {
            this.entries = new ArrayList<>(entries);
            this.emptyText = emptyText;
            setOpaque(false);
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(280, Math.max(entries.size(), 1) * ROW + 4);
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            UiKit.antialias(g);
            if (entries.isEmpty()) {
                g.setFont(Theme.regular(13));
                g.setColor(Theme.TEXT_FAINT);
                g.drawString(emptyText, 0, 20);
                g.dispose();
                return;
            }
            int max = 1;
            for (Entry e : entries) max = Math.max(max, e.value());
            int labelW = Math.min(130, getWidth() / 3);
            int valueW = 34;
            int barX = labelW + 10;
            int barW = Math.max(20, getWidth() - barX - valueW);
            for (int i = 0; i < entries.size(); i++) {
                Entry e = entries.get(i);
                int y = i * ROW;
                g.setFont(Theme.regular(13));
                FontMetrics fm = g.getFontMetrics();
                g.setColor(Theme.TEXT);
                g.drawString(UiKit.ellipsize(fm, e.label(), labelW), 0, y + (ROW - fm.getHeight()) / 2 + fm.getAscent());
                g.setColor(new Color(0x26262B));
                g.fill(new RoundRectangle2D.Double(barX, y + 10, barW, 14, 14, 14));
                double w = Math.max(14, barW * (e.value() / (double) max));
                g.setColor(e.color());
                g.fill(new RoundRectangle2D.Double(barX, y + 10, w, 14, 14, 14));
                g.setColor(Theme.TEXT_DIM);
                g.drawString(Integer.toString(e.value()), barX + barW + 8, y + (ROW - fm.getHeight()) / 2 + fm.getAscent());
            }
            g.dispose();
        }
    }

    /** A donut with the total in the middle and a legend (with percentages) on the right. */
    static final class DonutChart extends JComponent {
        private final List<Entry> entries;
        private final String centerLabel;

        DonutChart(List<Entry> entries, String centerLabel) {
            this.entries = new ArrayList<>(entries);
            this.centerLabel = centerLabel;
            setOpaque(false);
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(300, Math.max(190, entries.size() * 26 + 10));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            UiKit.antialias(g);
            int total = 0;
            for (Entry e : entries) total += e.value();
            if (total == 0) {
                g.setFont(Theme.regular(13));
                g.setColor(Theme.TEXT_FAINT);
                g.drawString("Import songs to see their distribution.", 0, 20);
                g.dispose();
                return;
            }
            int size = Math.min(getHeight() - 4, Math.min(190, getWidth() / 2));
            double cx = size / 2.0 + 2, cy = getHeight() / 2.0;
            double start = 90;
            for (Entry e : entries) {
                double extent = -360.0 * e.value() / total;
                if (entries.size() == 1) extent = -359.99;
                g.setColor(e.color());
                g.fill(new Arc2D.Double(cx - size / 2.0, cy - size / 2.0, size, size, start, extent, Arc2D.PIE));
                start += extent;
            }
            double hole = size * 0.62;
            g.setColor(Theme.PANEL);
            g.fill(new Ellipse2D.Double(cx - hole / 2, cy - hole / 2, hole, hole));

            g.setFont(Theme.bold(28));
            g.setColor(Theme.TEXT);
            FontMetrics fm = g.getFontMetrics();
            String n = Integer.toString(total);
            g.drawString(n, (float) (cx - fm.stringWidth(n) / 2.0), (float) (cy + fm.getAscent() / 2.0 - 10));
            g.setFont(Theme.regular(11));
            g.setColor(Theme.TEXT_DIM);
            fm = g.getFontMetrics();
            g.drawString(centerLabel, (float) (cx - fm.stringWidth(centerLabel) / 2.0), (float) (cy + 18));

            int lx = size + 28;
            int rowH = 26;
            int y = (int) (cy - entries.size() * rowH / 2.0);
            g.setFont(Theme.regular(13));
            fm = g.getFontMetrics();
            for (Entry e : entries) {
                g.setColor(e.color());
                g.fill(new Ellipse2D.Double(lx, y + 8, 10, 10));
                g.setColor(Theme.TEXT);
                g.drawString(e.label(), lx + 18, y + (rowH - fm.getHeight()) / 2 + fm.getAscent());
                g.setColor(Theme.TEXT_DIM);
                String pct = e.value() + "  (" + Math.round(100.0 * e.value() / total) + "%)";
                g.drawString(pct, Math.max(lx + 120, getWidth() - fm.stringWidth(pct) - 4), y + (rowH - fm.getHeight()) / 2 + fm.getAscent());
                y += rowH;
            }
            g.dispose();
        }
    }
}
