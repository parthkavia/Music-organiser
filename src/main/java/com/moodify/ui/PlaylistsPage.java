package com.moodify.ui;

import com.moodify.model.Song;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/** Playlist browser: list of playlists on the left, the selected playlist's songs on the right. */
public final class PlaylistsPage extends JPanel implements Refreshable {
    private final AppContext ctx;
    private final DefaultListModel<String> names = new DefaultListModel<>();
    private final JList<String> nameList = new JList<>(names);
    private final CardLayout cards = new CardLayout();
    private final JPanel deck = new JPanel(cards);
    private final JLabel title = UiKit.label("", Theme.bold(30), Theme.TEXT);
    private final JLabel meta = UiKit.label("", Theme.regular(13), Theme.TEXT_DIM);
    private final SongListPanel songs;
    private final PillButton playBtn = new PillButton("Play", Glyph.PLAY, PillButton.Style.PRIMARY);
    private boolean updating;

    public PlaylistsPage(AppContext ctx) {
        super(new BorderLayout(18, 0));
        this.ctx = ctx;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(26, 28, 8, 20));

        // ---- left column
        JPanel left = new JPanel(new BorderLayout(0, 14));
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(230, 100));
        JPanel leftTop = new JPanel(new BorderLayout());
        leftTop.setOpaque(false);
        leftTop.add(UiKit.label("Playlists", Theme.bold(28), Theme.TEXT), BorderLayout.WEST);
        IconButton add = new IconButton(Glyph.PLUS, 20, 36, "New playlist").colors(Theme.TEXT_DIM, Color.WHITE);
        add.addActionListener(e -> {
            String n = ctx.createPlaylistPrompt(this);
            if (n != null) select(n);
        });
        leftTop.add(add, BorderLayout.EAST);
        left.add(leftTop, BorderLayout.NORTH);

        nameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        nameList.setOpaque(false);
        nameList.setBackground(Theme.BG);
        nameList.setFixedCellHeight(52);
        nameList.setCellRenderer((l, value, index, selected, focus) -> new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(200, 52); }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                UiKit.antialias(g);
                if (selected) {
                    g.setColor(Theme.ELEVATED);
                    g.fill(new RoundRectangle2D.Double(0, 2, getWidth(), 48, 16, 16));
                }
                g.drawImage(CoverArt.get(38, 7), 7, 7, null);
                g.setFont(Theme.bold(13));
                FontMetrics fm = g.getFontMetrics();
                g.setColor(Theme.TEXT);
                g.drawString(UiKit.ellipsize(fm, value, getWidth() - 62), 54, 23);
                g.setFont(Theme.regular(11));
                g.setColor(Theme.TEXT_DIM);
                g.drawString(UiKit.plural(ctx.playlists.songIds(value).size(), "song", "songs"), 54, 39);
                g.dispose();
            }
        });
        nameList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !updating) showSelected();
        });
        JScrollPane nameScroll = new JScrollPane(nameList);
        nameScroll.setBackground(Theme.BG);
        UiKit.styleScroll(nameScroll);
        nameScroll.setOpaque(false);
        nameScroll.getViewport().setOpaque(false);
        left.add(nameScroll, BorderLayout.CENTER);
        add(left, BorderLayout.WEST);

        // ---- right column
        JPanel detail = new JPanel(new BorderLayout(0, 14));
        detail.setOpaque(false);
        JPanel head = new JPanel(new BorderLayout(18, 0));
        head.setOpaque(false);
        JLabel art = new JLabel(CoverArt.icon(96, 14));
        head.add(art, BorderLayout.WEST);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
        JLabel kicker = UiKit.label("PLAYLIST", Theme.bold(11), Theme.TEXT_FAINT);
        kicker.setAlignmentX(0f);
        title.setAlignmentX(0f);
        meta.setAlignmentX(0f);
        text.add(javax.swing.Box.createVerticalGlue());
        text.add(kicker);
        text.add(title);
        text.add(javax.swing.Box.createRigidArea(new Dimension(0, 2)));
        text.add(meta);
        text.add(javax.swing.Box.createVerticalGlue());
        head.add(text, BorderLayout.CENTER);
        detail.add(head, BorderLayout.NORTH);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        PillButton addSongs = new PillButton("Add songs", Glyph.PLUS, PillButton.Style.SECONDARY);
        IconButton up = new IconButton(Glyph.UP, 20, 38, "Move selected song up").colors(Theme.TEXT_DIM, Color.WHITE);
        IconButton down = new IconButton(Glyph.DOWN, 20, 38, "Move selected song down").colors(Theme.TEXT_DIM, Color.WHITE);
        IconButton rename = new IconButton(Glyph.EDIT, 20, 38, "Rename playlist").colors(Theme.TEXT_DIM, Color.WHITE);
        IconButton delete = new IconButton(Glyph.TRASH, 20, 38, "Delete playlist").colors(Theme.TEXT_DIM, Theme.DANGER);
        actions.add(playBtn);
        actions.add(addSongs);
        actions.add(javax.swing.Box.createRigidArea(new Dimension(10, 0)));
        actions.add(up);
        actions.add(down);
        actions.add(rename);
        actions.add(delete);

        songs = new SongListPanel(ctx, SongListPanel.Mode.PLAYLIST, this::selectedName);
        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.add(actions, BorderLayout.NORTH);
        body.add(songs, BorderLayout.CENTER);
        detail.add(body, BorderLayout.CENTER);

        EmptyState none = new EmptyState(new PillButton("Create playlist", Glyph.PLUS, PillButton.Style.PRIMARY));
        none.set("No playlists yet", "Create one to group your favourite songs.", true);
        ((PillButton) none.getComponent(0)).addActionListener(e -> {
            String n = ctx.createPlaylistPrompt(this);
            if (n != null) select(n);
        });
        deck.setOpaque(false);
        deck.add(detail, "detail");
        deck.add(none, "none");
        add(deck, BorderLayout.CENTER);

        playBtn.addActionListener(e -> { String n = selectedName(); if (n != null) ctx.playPlaylist(n); });
        addSongs.addActionListener(e -> { String n = selectedName(); if (n != null) AddSongsDialog.show(this, ctx, n); });
        up.addActionListener(e -> songs.moveSelected(-1));
        down.addActionListener(e -> songs.moveSelected(1));
        rename.addActionListener(e -> renameSelected());
        delete.addActionListener(e -> deleteSelected());
        refresh();
    }

    private String selectedName() { return nameList.getSelectedValue(); }

    public void select(String name) {
        refresh();
        if (name != null) nameList.setSelectedValue(name, true);
        showSelected();
    }

    private void renameSelected() {
        String old = selectedName();
        if (old == null) return;
        String n = Dialogs.prompt(this, "Rename playlist", null, old, "Rename", t -> ctx.playlists.validateName(t, old));
        if (n == null) return;
        try {
            String renamed = ctx.playlists.rename(old, n);
            select(renamed);
        } catch (IllegalArgumentException e) {
            ctx.toast(e.getMessage(), true);
        }
    }

    private void deleteSelected() {
        String n = selectedName();
        if (n == null) return;
        if (Dialogs.confirm(this, "Delete playlist?", "Delete \u201C" + n + "\u201D? Your songs stay in the library.", "Delete", true)) {
            ctx.playlists.delete(n);
            ctx.toast("Deleted playlist \u201C" + n + "\u201D.");
        }
    }

    @Override
    public void refresh() {
        updating = true;
        String keep = selectedName();
        names.clear();
        for (String n : ctx.playlists.names()) names.addElement(n);
        if (keep != null && ctx.playlists.exists(keep)) nameList.setSelectedValue(keep, true);
        else if (!names.isEmpty()) nameList.setSelectedIndex(0);
        updating = false;
        showSelected();
        nameList.repaint();
    }

    private void showSelected() {
        String name = selectedName();
        if (name == null) {
            cards.show(deck, "none");
            return;
        }
        cards.show(deck, "detail");
        List<Song> list = ctx.library.resolve(ctx.playlists.songIds(name));
        long total = 0;
        for (Song s : list) total += s.getDurationMs();
        title.setText(name);
        meta.setText(UiKit.plural(list.size(), "song", "songs") + (total > 0 ? "  \u2022  " + UiKit.formatTotal(total) : ""));
        playBtn.setEnabled(!list.isEmpty());
        songs.setSongs(list, "This playlist is empty", "Use \u201CAdd songs\u201D or right-click a song in your library.", false);
    }
}
