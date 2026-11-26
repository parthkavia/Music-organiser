package com.moodify.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/** Vector icons drawn with Java2D on a 24x24 grid, so they stay crisp at any size. */
public enum Glyph {
    PLAY, PAUSE, NEXT, PREV, REPEAT, REPEAT_ONE, VOLUME, MUTE, HOME, LIBRARY, PLAYLIST, INSIGHTS,
    PLUS, SEARCH, MORE, UP, DOWN, TRASH, EDIT, LINK, WARNING, NOTE;

    /** Paints this glyph into a size x size square whose top-left corner is (x, y). */
    public void paint(Graphics2D g0, double x, double y, double size, Color color) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(x, y);
        double s = size / 24.0;
        g.scale(s, s);
        g.setColor(color);
        BasicStroke line = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        switch (this) {
            case PLAY -> g.fill(poly(8, 5, 19.5, 12, 8, 19));
            case PAUSE -> {
                g.fill(new RoundRectangle2D.Double(6, 5, 4.2, 14, 1.6, 1.6));
                g.fill(new RoundRectangle2D.Double(13.8, 5, 4.2, 14, 1.6, 1.6));
            }
            case NEXT -> {
                g.fill(poly(6, 5.5, 16, 12, 6, 18.5));
                g.fill(new RoundRectangle2D.Double(16.5, 5.5, 2.6, 13, 1.2, 1.2));
            }
            case PREV -> {
                g.fill(poly(18, 5.5, 8, 12, 18, 18.5));
                g.fill(new RoundRectangle2D.Double(4.9, 5.5, 2.6, 13, 1.2, 1.2));
            }
            case REPEAT, REPEAT_ONE -> {
                g.setStroke(line);
                Path2D top = new Path2D.Double();
                top.moveTo(5, 12);
                top.lineTo(5, 10);
                top.quadTo(5, 7.5, 7.5, 7.5);
                top.lineTo(15, 7.5);
                g.draw(top);
                g.fill(poly(14.5, 4, 19.5, 7.5, 14.5, 11));
                Path2D bot = new Path2D.Double();
                bot.moveTo(19, 12);
                bot.lineTo(19, 14);
                bot.quadTo(19, 16.5, 16.5, 16.5);
                bot.lineTo(9, 16.5);
                g.draw(bot);
                g.fill(poly(9.5, 13, 4.5, 16.5, 9.5, 20));
                if (this == REPEAT_ONE) {
                    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString("1", (float) (12 - fm.stringWidth("1") / 2.0), 15.2f);
                }
            }
            case VOLUME, MUTE -> {
                g.fill(poly(4, 9.5, 8, 9.5, 13, 5.5, 13, 18.5, 8, 14.5, 4, 14.5));
                g.setStroke(line);
                if (this == VOLUME) {
                    g.draw(new Arc2D.Double(8.5, 8, 8, 8, -50, 100, Arc2D.OPEN));
                    g.draw(new Arc2D.Double(5.5, 4.5, 15, 15, -50, 100, Arc2D.OPEN));
                } else {
                    g.draw(new java.awt.geom.Line2D.Double(16, 9.5, 21, 14.5));
                    g.draw(new java.awt.geom.Line2D.Double(21, 9.5, 16, 14.5));
                }
            }
            case HOME -> g.fill(poly(12, 3.2, 21.8, 11.8, 19.2, 11.8, 19.2, 20.5, 14.6, 20.5, 14.6, 14.5,
                    9.4, 14.5, 9.4, 20.5, 4.8, 20.5, 4.8, 11.8, 2.2, 11.8));
            case LIBRARY -> {
                g.fill(new RoundRectangle2D.Double(3.5, 4, 3.4, 16, 1.2, 1.2));
                g.fill(new RoundRectangle2D.Double(9.3, 4, 3.4, 16, 1.2, 1.2));
                g.fill(poly(15, 6.2, 18.1, 5.4, 21.6, 18.4, 18.5, 19.2));
            }
            case PLAYLIST -> {
                g.fill(new RoundRectangle2D.Double(3.5, 5.5, 11.5, 2.2, 1.1, 1.1));
                g.fill(new RoundRectangle2D.Double(3.5, 10.9, 11.5, 2.2, 1.1, 1.1));
                g.fill(new RoundRectangle2D.Double(3.5, 16.3, 6.5, 2.2, 1.1, 1.1));
                g.fill(new Ellipse2D.Double(14.5, 14.5, 5, 4.2));
                g.fill(new Rectangle2D.Double(18.2, 6, 1.7, 11));
                g.fill(poly(18.2, 6, 22, 8.3, 18.2, 9.5));
            }
            case INSIGHTS -> {
                g.fill(new RoundRectangle2D.Double(3.5, 12.5, 4.4, 8, 1.4, 1.4));
                g.fill(new RoundRectangle2D.Double(9.8, 4, 4.4, 16.5, 1.4, 1.4));
                g.fill(new RoundRectangle2D.Double(16.1, 8.5, 4.4, 12, 1.4, 1.4));
            }
            case PLUS -> {
                g.fill(new RoundRectangle2D.Double(10.8, 4.5, 2.4, 15, 1.2, 1.2));
                g.fill(new RoundRectangle2D.Double(4.5, 10.8, 15, 2.4, 1.2, 1.2));
            }
            case SEARCH -> {
                g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Ellipse2D.Double(4, 4, 11.5, 11.5));
                g.draw(new java.awt.geom.Line2D.Double(14, 14, 20, 20));
            }
            case MORE -> {
                g.fill(new Ellipse2D.Double(4.2, 10.2, 3.6, 3.6));
                g.fill(new Ellipse2D.Double(10.2, 10.2, 3.6, 3.6));
                g.fill(new Ellipse2D.Double(16.2, 10.2, 3.6, 3.6));
            }
            case UP -> {
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D p = new Path2D.Double();
                p.moveTo(6, 15);
                p.lineTo(12, 9);
                p.lineTo(18, 15);
                g.draw(p);
            }
            case DOWN -> {
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D p = new Path2D.Double();
                p.moveTo(6, 9);
                p.lineTo(12, 15);
                p.lineTo(18, 9);
                g.draw(p);
            }
            case TRASH -> {
                g.fill(new RoundRectangle2D.Double(4, 5, 16, 2.2, 1.1, 1.1));
                g.fill(new RoundRectangle2D.Double(9, 2.8, 6, 2.6, 1.2, 1.2));
                Path2D body = new Path2D.Double();
                body.moveTo(6, 8.5);
                body.lineTo(18, 8.5);
                body.lineTo(17, 20);
                body.quadTo(16.9, 21, 15.9, 21);
                body.lineTo(8.1, 21);
                body.quadTo(7.1, 21, 7, 20);
                body.closePath();
                g.fill(body);
            }
            case EDIT -> {
                g.fill(poly(3.5, 20.5, 4.4, 15.6, 15.8, 4.2, 19.8, 8.2, 8.4, 19.6));
                g.fill(poly(17, 3, 18.4, 1.6, 22.4, 5.6, 21, 7));
            }
            case LINK -> {
                g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new RoundRectangle2D.Double(2.8, 8.5, 11, 7, 7, 7));
                g.draw(new RoundRectangle2D.Double(10.2, 8.5, 11, 7, 7, 7));
            }
            case WARNING -> {
                g.fill(poly(12, 3, 22, 20.5, 2, 20.5));
                g.setColor(new Color(0x161618));
                g.fill(new RoundRectangle2D.Double(11, 9, 2, 6, 1, 1));
                g.fill(new Ellipse2D.Double(10.9, 16.3, 2.2, 2.2));
            }
            case NOTE -> {
                g.fill(new Ellipse2D.Double(5, 15, 7, 5.6));
                g.fill(new Rectangle2D.Double(10.4, 4, 2, 13));
                g.fill(poly(10.4, 4, 18.5, 6.5, 18.5, 10, 10.4, 7.6));
            }
        }
        g.dispose();
    }

    private static Path2D poly(double... xy) {
        Path2D p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) p.lineTo(xy[i], xy[i + 1]);
        p.closePath();
        return p;
    }

    /** The Moodify logo: a gradient rounded square with equaliser bars. */
    public static BufferedImage logo(int px) {
        BufferedImage img = new BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(0x7C3AED), px, px, new Color(0xEC4899)));
        g.fill(new RoundRectangle2D.Double(0, 0, px, px, px * 0.3, px * 0.3));
        g.setColor(Color.WHITE);
        double bw = px * 0.12, gap = px * 0.075, total = 4 * bw + 3 * gap, x0 = (px - total) / 2;
        double[] h = {0.30, 0.52, 0.40, 0.24};
        double base = px * 0.72;
        for (int i = 0; i < 4; i++) {
            double hh = px * h[i] * 1.15;
            g.fill(new RoundRectangle2D.Double(x0 + i * (bw + gap), base - hh, bw, hh, bw, bw));
        }
        g.dispose();
        return img;
    }
}
