package com.moodify.ui;

import com.moodify.data.LibraryQuery;
import com.moodify.data.SortOption;
import com.moodify.model.Song;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/** Pick several library songs to add to a playlist. */
public final class AddSongsDialog extends BaseDialog {
    private AddSongsDialog(Component parent, AppContext ctx, String playlist) {
        super(parent, "Add songs");
        List<Song> candidates = new ArrayList<>();
        for (Song s : ctx.library.getAll()) if (!ctx.playlists.contains(playlist, s.getId())) candidates.add(s);

        DefaultListModel<Song> model = new DefaultListModel<>();
        JList<Song> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setBackground(Theme.ELEVATED);
        list.setFixedCellHeight(44);
        list.setCellRenderer((l, song, index, selected, focus) -> new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(300, 44); }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                UiKit.antialias(g);
                g.setColor(selected ? Theme.mix(Theme.ELEVATED, Theme.ACCENT, 0.45f) : Theme.ELEVATED);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.drawImage(CoverArt.get(32, 5), 10, 6, null);
                g.setFont(Theme.bold(13));
                FontMetrics fm = g.getFontMetrics();
                g.setColor(Theme.TEXT);
                g.drawString(UiKit.ellipsize(fm, song.getTitle(), getWidth() - 60), 52, 19);
                g.setFont(Theme.regular(11));
                g.setColor(Theme.TEXT_DIM);
                fm = g.getFontMetrics();
                g.drawString(UiKit.ellipsize(fm, song.getArtist(), getWidth() - 60), 52, 34);
                g.dispose();
            }
        });
        StyledField filter = new StyledField("Search songs", Glyph.SEARCH);
        Runnable fill = () -> {
            model.clear();
            for (Song s : LibraryQuery.apply(candidates, filter.getText(), null, SortOption.TITLE_ASC)) model.addElement(s);
        };
        filter.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { fill.run(); }
            @Override public void removeUpdate(DocumentEvent e) { fill.run(); }
            @Override public void changedUpdate(DocumentEvent e) { fill.run(); }
        });
        fill.run();

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBackground(Theme.ELEVATED);
        UiKit.styleScroll(scroll);
        scroll.getViewport().setBackground(Theme.ELEVATED);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.setPreferredSize(new Dimension(440, 300));

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(UiKit.label("Add songs to \u201C" + playlist + "\u201D", Theme.bold(17), Theme.TEXT), BorderLayout.NORTH);
        top.add(filter, BorderLayout.CENTER);
        body.add(top, BorderLayout.NORTH);
        if (candidates.isEmpty()) {
            JLabel none = UiKit.label("Every song in your library is already in this playlist.", Theme.regular(13), Theme.TEXT_DIM);
            body.add(none, BorderLayout.CENTER);
        } else {
            body.add(scroll, BorderLayout.CENTER);
            body.add(UiKit.label("Hold Ctrl or Shift to select several songs.", Theme.regular(11), Theme.TEXT_FAINT), BorderLayout.SOUTH);
        }
        setBody(body);
        addButton("Cancel", PillButton.Style.GHOST, this::dispose, false);
        addButton("Add selected", PillButton.Style.PRIMARY, () -> {
            List<Song> sel = list.getSelectedValuesList();
            if (!sel.isEmpty()) ctx.addToPlaylist(sel, playlist);
            dispose();
        }, true);
    }

    public static void show(Component parent, AppContext ctx, String playlist) {
        new AddSongsDialog(parent, ctx, playlist).open(500);
    }
}
