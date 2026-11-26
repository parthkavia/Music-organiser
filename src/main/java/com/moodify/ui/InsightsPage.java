package com.moodify.ui;

import com.moodify.data.LibraryStats;
import com.moodify.model.Song;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Charts and totals calculated from the real local library. */
public final class InsightsPage extends JPanel implements Refreshable {
    private final AppContext ctx;
    private final UiKit.ScrollablePanel content = new UiKit.ScrollablePanel();
    private static final Color[] PALETTE = {
            new Color(0x8B5CF6), new Color(0xEC4899), new Color(0x22D3EE), new Color(0xFBBF24),
            new Color(0x34D399), new Color(0xF97316), new Color(0x60A5FA), new Color(0xF472B6)};

    public InsightsPage(AppContext ctx) {
        super(new BorderLayout());
        this.ctx = ctx;
        setOpaque(false);
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(26, 28, 24, 24));
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBackground(Theme.BG);
        UiKit.styleScroll(scroll);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);
        refresh();
    }

    @Override
    public void refresh() {
        content.removeAll();
        List<Song> all = ctx.library.getAll();
        LibraryStats stats = new LibraryStats(all);

        PageHeader header = new PageHeader("Insights", all.isEmpty()
                ? "Import some music to see how your library looks." : "How your local music library breaks down.");
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(header);
        content.add(Box.createRigidArea(new Dimension(0, 20)));

        JPanel numbers = new JPanel(new GridLayout(1, 4, 14, 0));
        numbers.setOpaque(false);
        numbers.setAlignmentX(Component.LEFT_ALIGNMENT);
        numbers.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        numbers.add(stat("Songs", Integer.toString(stats.totalSongs()), Theme.ACCENT_HOVER));
        numbers.add(stat("Playlists", Integer.toString(ctx.playlists.count()), Theme.ACCENT2));
        numbers.add(stat("Total playtime", stats.totalDurationMs() > 0 ? UiKit.formatTotal(stats.totalDurationMs()) : "\u2013", new Color(0x22D3EE)));
        numbers.add(stat("Missing files", Integer.toString(stats.missingFiles()),
                stats.missingFiles() > 0 ? Theme.DANGER : Theme.SUCCESS));
        content.add(numbers);
        content.add(Box.createRigidArea(new Dimension(0, 16)));

        List<Charts.Entry> genreEntries = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Integer> e : stats.topGenres(8).entrySet()) {
            genreEntries.add(new Charts.Entry(e.getKey(), e.getValue(), PALETTE[i++ % PALETTE.length]));
        }
        List<Charts.Entry> artistEntries = new ArrayList<>();
        i = 3;
        for (Map.Entry<String, Integer> e : stats.topArtists(8).entrySet()) {
            artistEntries.add(new Charts.Entry(e.getKey(), e.getValue(), PALETTE[i++ % PALETTE.length]));
        }
        String artistNote = stats.songsWithoutArtist() > 0
                ? UiKit.plural(stats.songsWithoutArtist(), "song has", "songs have") + " no artist set (edit a song to add one)."
                : null;
        content.add(row(
                card("Songs by genre", new Charts.BarChart(genreEntries, "No songs yet.")),
                card("Genre distribution", new Charts.DonutChart(genreEntries, "songs"))));
        content.add(Box.createRigidArea(new Dimension(0, 16)));
        content.add(row(
                card("Most common artists", new Charts.BarChart(artistEntries, "No artists set yet."), artistNote),
                card("Library health", new Charts.BarChart(health(stats), "No songs yet."))));
        content.revalidate();
        content.repaint();
    }

    private List<Charts.Entry> health(LibraryStats stats) {
        List<Charts.Entry> e = new ArrayList<>();
        if (stats.totalSongs() == 0) return e;
        e.add(new Charts.Entry("Available", stats.totalSongs() - stats.missingFiles(), Theme.SUCCESS));
        e.add(new Charts.Entry("Missing", stats.missingFiles(), Theme.DANGER));
        e.add(new Charts.Entry("No artist set", stats.songsWithoutArtist(), Theme.TEXT_FAINT));
        return e;
    }

    private JPanel stat(String label, String value, Color accent) {
        RoundedPanel p = new RoundedPanel(new BorderLayout(), Theme.PANEL, 18);
        p.setBorder(BorderFactory.createEmptyBorder(16, 20, 14, 16));
        JLabel v = UiKit.label(value, Theme.bold(26), accent);
        JLabel l = UiKit.label(label, Theme.regular(12), Theme.TEXT_DIM);
        p.add(v, BorderLayout.CENTER);
        p.add(l, BorderLayout.SOUTH);
        return p;
    }

    private JPanel card(String title, JComponent chart) { return card(title, chart, null); }

    private JPanel card(String title, JComponent chart, String note) {
        RoundedPanel p = new RoundedPanel(new BorderLayout(0, 14), Theme.PANEL, 18);
        p.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        p.add(UiKit.label(title, Theme.bold(15), Theme.TEXT), BorderLayout.NORTH);
        p.add(chart, BorderLayout.CENTER);
        if (note != null) p.add(UiKit.label(note, Theme.regular(11), Theme.TEXT_FAINT), BorderLayout.SOUTH);
        return p;
    }

    private JPanel row(JComponent left, JComponent right) {
        JPanel r = new JPanel(new GridLayout(1, 2, 14, 0));
        r.setOpaque(false);
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        r.add(left);
        r.add(right);
        return r;
    }
}
