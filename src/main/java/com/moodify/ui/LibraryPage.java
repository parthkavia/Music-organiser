package com.moodify.ui;

import com.moodify.data.LibraryQuery;
import com.moodify.data.SortOption;
import com.moodify.model.Song;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** All imported songs, with search, mood / playlist filters and sorting. */
public final class LibraryPage extends JPanel implements Refreshable {
    private static final Object ALL_SONGS = new Object() { @Override public String toString() { return "All songs"; } };

    private final AppContext ctx;
    private final PageHeader header = new PageHeader("Your Library", " ");
    private final StyledField search = new StyledField("Search title, artist or genre", Glyph.SEARCH);
    private StyledCombo<Object> playlistBox;
    private final StyledCombo<SortOption> sortBox = new StyledCombo<>(SortOption.values());
    private final SongListPanel list;
    private final JPanel playlistHolder = new JPanel(new BorderLayout());
    private boolean updating;

    public LibraryPage(AppContext ctx) {
        super(new BorderLayout(0, 14));
        this.ctx = ctx;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(26, 28, 8, 20));

        sortBox.setPreferredSize(new Dimension(170, 38));
        sortBox.setToolTipText("Sort songs");
        playlistHolder.setOpaque(false);
        playlistBox = buildPlaylistBox();
        playlistHolder.add(playlistBox, BorderLayout.CENTER);
        playlistHolder.setPreferredSize(new Dimension(170, 38));

        PillButton importBtn = new PillButton("Import music", Glyph.PLUS, PillButton.Style.PRIMARY);
        importBtn.addActionListener(e -> ctx.importMusic(this));

        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.setOpaque(false);
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(header, BorderLayout.WEST);
        JPanel importWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        importWrap.setOpaque(false);
        importWrap.add(importBtn);
        titleRow.add(importWrap, BorderLayout.EAST);
        top.add(titleRow, BorderLayout.NORTH);

        JPanel tools = new JPanel(new GridBagLayout());
        tools.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0;
        c.gridx = 0; c.weightx = 1; c.insets = new Insets(0, 0, 0, 10); tools.add(search, c);
        c.insets = new Insets(0, 0, 0, 10);
        c.gridx = 1; c.weightx = 0; tools.add(playlistHolder, c);
        c.gridx = 2; c.insets = new Insets(0, 0, 0, 0); tools.add(sortBox, c);
        top.add(tools, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        list = new SongListPanel(ctx, SongListPanel.Mode.LIBRARY, () -> null);
        add(list, BorderLayout.CENTER);

        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { apply(); }
            @Override public void removeUpdate(DocumentEvent e) { apply(); }
            @Override public void changedUpdate(DocumentEvent e) { apply(); }
        });
        sortBox.addActionListener(e -> { if (!updating) apply(); });
        refresh();
    }

    private StyledCombo<Object> buildPlaylistBox() {
        List<Object> items = new ArrayList<>();
        items.add(ALL_SONGS);
        items.addAll(ctx.playlists.names());
        StyledCombo<Object> box = new StyledCombo<>(items.toArray());
        box.setPreferredSize(new Dimension(170, 38));
        box.setToolTipText("Filter by playlist");
        box.addActionListener(e -> { if (!updating) apply(); });
        return box;
    }

    /** Shows only the songs of a playlist (null = all songs). */
    public void showPlaylist(String playlist) {
        updating = true;
        search.setText("");
        playlistBox.setSelectedItem(playlist != null && ctx.playlists.exists(playlist) ? playlist : ALL_SONGS);
        updating = false;
        apply();
    }

    @Override
    public void refresh() {
        updating = true;
        Object previous = playlistBox.getSelectedItem();
        playlistHolder.remove(playlistBox);
        playlistBox = buildPlaylistBox();
        boolean stillThere = previous instanceof String s && ctx.playlists.exists(s);
        playlistBox.setSelectedItem(stillThere ? previous : ALL_SONGS);
        playlistHolder.add(playlistBox, BorderLayout.CENTER);
        playlistHolder.revalidate();
        playlistHolder.repaint();
        updating = false;
        apply();
    }

    private void apply() {
        List<Song> all = ctx.library.getAll();
        String playlist = playlistBox.getSelectedItem() instanceof String s ? s : null;
        LinkedHashSet<String> allowed = playlist == null ? null : new LinkedHashSet<>(ctx.playlists.songIds(playlist));
        SortOption sort = (SortOption) sortBox.getSelectedItem();
        List<Song> shown = LibraryQuery.apply(all, search.getText(), allowed, sort);

        int missing = 0;
        for (Song s : all) if (s.isMissing()) missing++;
        String sub = all.isEmpty() ? "No songs yet"
                : (shown.size() == all.size() ? UiKit.plural(all.size(), "song", "songs")
                : shown.size() + " of " + UiKit.plural(all.size(), "song", "songs"));
        if (missing > 0) sub += "  \u2022  " + missing + " missing";
        header.setSubtitle(sub);

        if (all.isEmpty()) list.setSongs(shown, "Your library is empty", "Import MP3 files and they will show up here.", true);
        else list.setSongs(shown, "No songs match", "Try a different search or filter.", false);
    }
}
