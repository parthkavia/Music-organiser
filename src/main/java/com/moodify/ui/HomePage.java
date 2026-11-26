package com.moodify.ui;

import com.moodify.model.Song;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The dashboard: welcome banner, recent songs, playlists and mood shortcuts. */
public final class HomePage extends JPanel implements Refreshable {
    private static final int MAX_CARDS = 12;

    private final AppContext ctx;
    private final UiKit.ScrollablePanel content = new UiKit.ScrollablePanel();

    public HomePage(AppContext ctx) {
        super(new BorderLayout());
        this.ctx = ctx;
        setOpaque(false);
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 20));
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBackground(Theme.BG);
        UiKit.styleScroll(scroll);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);
        refresh();
    }

    private static String greeting() {
        int h = LocalTime.now().getHour();
        return h < 12 ? "Good morning" : h < 18 ? "Good afternoon" : "Good evening";
    }

    @Override
    public void refresh() {
        content.removeAll();
        List<Song> all = ctx.library.getAll();

        content.add(banner(all.size()));
        content.add(Box.createRigidArea(new Dimension(0, 8)));

        if (all.isEmpty()) {
            JPanel hint = new JPanel(new BorderLayout());
            hint.setOpaque(false);
            hint.setAlignmentX(Component.LEFT_ALIGNMENT);
            hint.setBorder(BorderFactory.createEmptyBorder(30, 0, 10, 0));
            hint.add(UiKit.label("Your library is empty", Theme.bold(18), Theme.TEXT), BorderLayout.NORTH);
            hint.add(UiKit.label("Import some MP3 files and Moodify will organise them by mood for you.",
                    Theme.regular(13), Theme.TEXT_DIM), BorderLayout.CENTER);
            content.add(hint);
        } else {
            List<Song> played = new ArrayList<>();
            for (Song s : all) if (s.getLastPlayedAt() > 0) played.add(s);
            played.sort(Comparator.comparingLong(Song::getLastPlayedAt).reversed());
            section("Recently played", songCards(limit(played), limit(played)),
                    "Songs you play will show up here.");

            List<Song> added = new ArrayList<>(all);
            added.sort(Comparator.comparingLong(Song::getAddedAt).reversed());
            section("Recently added", songCards(limit(added), limit(added)), null);
        }

        // playlists
        List<Component> plCards = new ArrayList<>();
        for (String name : ctx.playlists.names()) {
            int n = ctx.playlists.songIds(name).size();
            plCards.add(new Card(name, UiKit.plural(n, "song", "songs"), false, false, () -> ctx.nav().showPlaylists(name)));
            if (plCards.size() >= MAX_CARDS) break;
        }
        plCards.add(new Card("New playlist", "Create one", false, true, () -> {
            String n = ctx.createPlaylistPrompt(this);
            if (n != null) ctx.nav().showPlaylists(n);
        }));
        section("Your playlists", plCards, null);

        content.revalidate();
        content.repaint();
    }

    private static List<Song> limit(List<Song> songs) {
        return songs.size() > MAX_CARDS ? new ArrayList<>(songs.subList(0, MAX_CARDS)) : songs;
    }

    private List<Component> songCards(List<Song> shown, List<Song> queue) {
        List<Component> cards = new ArrayList<>();
        for (int i = 0; i < shown.size(); i++) {
            Song s = shown.get(i);
            int index = i;
            cards.add(new Card(s.getTitle(), s.getArtist(), s.isMissing(), false, () -> ctx.playSongs(queue, index)));
        }
        return cards;
    }

    private void section(String title, List<Component> cards, String emptyText) {
        JLabel l = UiKit.label(title, Theme.bold(20), Theme.TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(22, 2, 12, 0));
        content.add(l);
        if (cards.isEmpty()) {
            JLabel e = UiKit.label(emptyText == null ? "" : emptyText, Theme.regular(13), Theme.TEXT_FAINT);
            e.setAlignmentX(Component.LEFT_ALIGNMENT);
            e.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
            content.add(e);
            return;
        }
        JPanel row = new JPanel(new UiKit.WrapLayout(FlowLayout.LEFT, 14, 14));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Component c : cards) row.add(c);
        content.add(row);
    }

    private JPanel banner(int songCount) {
        JPanel b = new JPanel(new BorderLayout(20, 0)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                UiKit.antialias(g);
                g.setPaint(new GradientPaint(0, 0, new Color(0x4C1D95), getWidth(), getHeight(), new Color(0x9D174D)));
                g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 28, 28));
                g.setColor(Theme.alpha(Color.WHITE, 20));
                g.fillOval(getWidth() - 170, -90, 280, 280);
                g.fillOval(getWidth() - 300, 40, 140, 140);
                g.dispose();
            }
        };
        b.setOpaque(false);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setBorder(BorderFactory.createEmptyBorder(26, 30, 26, 30));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        b.setPreferredSize(new Dimension(100, 170));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel hi = UiKit.label(greeting() + ".", Theme.bold(32), Color.WHITE);
        JLabel sub = UiKit.label("Welcome to Moodify \u2014 your music, right on your computer.", Theme.regular(14), Theme.alpha(Color.WHITE, 210));
        hi.setAlignmentX(0f);
        sub.setAlignmentX(0f);
        text.add(hi);
        text.add(Box.createRigidArea(new Dimension(0, 4)));
        text.add(sub);
        text.add(Box.createRigidArea(new Dimension(0, 18)));
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        chips.setOpaque(false);
        chips.setAlignmentX(0f);
        chips.add(chip(UiKit.plural(songCount, "song", "songs")));
        chips.add(chip(UiKit.plural(ctx.playlists.count(), "playlist", "playlists")));
        text.add(chips);
        b.add(text, BorderLayout.CENTER);

        PillButton imp = new PillButton("Import music", Glyph.PLUS, PillButton.Style.PRIMARY);
        imp.setPreferredSize(new Dimension(170, 46));
        imp.addActionListener(e -> ctx.importMusic(this));
        PillButton play = new PillButton("Play library", Glyph.PLAY, PillButton.Style.SECONDARY);
        play.setPreferredSize(new Dimension(170, 46));
        play.addActionListener(e -> ctx.playAllLibrary());
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        imp.setAlignmentX(1f);
        play.setAlignmentX(1f);
        buttons.add(Box.createVerticalGlue());
        buttons.add(imp);
        buttons.add(Box.createRigidArea(new Dimension(0, 10)));
        buttons.add(play);
        buttons.add(Box.createVerticalGlue());
        b.add(buttons, BorderLayout.EAST);
        return b;
    }

    private static JComponentChip chip(String text) { return new JComponentChip(text); }

    /** A small translucent pill with a number/label. */
    private static final class JComponentChip extends JPanel {
        private final String text;
        JComponentChip(String text) {
            this.text = text;
            setOpaque(false);
            setPreferredSize(new Dimension(getFontMetrics(Theme.bold(13)).stringWidth(text) + 28, 30));
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            UiKit.antialias(g);
            g.setColor(Theme.alpha(Color.BLACK, 70));
            g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
            g.setFont(Theme.bold(13));
            g.setColor(Color.WHITE);
            java.awt.FontMetrics fm = g.getFontMetrics();
            g.drawString(text, 14, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }
}
