package com.moodify.ui;

import com.moodify.model.Song;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Dialog for editing a song's title, artist and genre. */
public final class SongDialog extends BaseDialog {
    private static final int MAX_LEN = 200;

    private SongDialog(Component parent, AppContext ctx, Song song) {
        super(parent, "Edit song");
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        StyledField title = new StyledField(null, null);
        title.setText(song.getTitle());
        StyledField artist = new StyledField(null, null);
        artist.setText(song.getArtist());
        StyledField genre = new StyledField(null, null);
        genre.setText(song.getGenre());

        JLabel error = new JLabel(" ");
        error.setFont(Theme.regular(12));
        error.setForeground(Theme.DANGER);

        int row = 0;
        row = addRow(form, row, "Title", title);
        row = addRow(form, row, "Artist", artist);
        row = addRow(form, row, "Genre", genre);

        JLabel path = UiKit.label(song.getFilePath(), Theme.regular(11), Theme.TEXT_FAINT);
        path.setToolTipText(song.getFilePath());
        path.setPreferredSize(new Dimension(380, 16));
        row = addRow(form, row, song.isMissing() ? "File (missing)" : "File", path);
        GridBagConstraints ec = new GridBagConstraints();
        ec.gridx = 0; ec.gridy = row; ec.gridwidth = 2; ec.anchor = GridBagConstraints.WEST; ec.insets = new Insets(8, 2, 0, 0);
        form.add(error, ec);

        JLabel heading = UiKit.label("Edit song", Theme.bold(17), Theme.TEXT);
        JPanel body = new JPanel(new java.awt.BorderLayout(0, 14));
        body.setOpaque(false);
        body.add(heading, java.awt.BorderLayout.NORTH);
        body.add(form, java.awt.BorderLayout.CENTER);
        setBody(body);

        addButton("Cancel", PillButton.Style.GHOST, this::dispose, false);
        addButton("Save", PillButton.Style.PRIMARY, () -> {
            String t = title.getText().trim();
            String a = artist.getText().trim();
            String g = genre.getText().trim();
            if (t.isEmpty()) { error.setText("The title can't be empty."); title.requestFocusInWindow(); return; }
            if (t.length() > MAX_LEN || a.length() > MAX_LEN || g.length() > MAX_LEN) {
                error.setText("Please keep each field under " + MAX_LEN + " characters.");
                return;
            }
            ctx.library.edit(song.getId(), t, a, g);
            dispose();
        }, true);
        SwingUtilities.invokeLater(() -> { title.requestFocusInWindow(); title.selectAll(); });
    }

    private static int addRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints lc = new GridBagConstraints();
        lc.gridx = 0; lc.gridy = row; lc.anchor = GridBagConstraints.WEST; lc.insets = new Insets(6, 2, 6, 14);
        JLabel l = UiKit.label(label, Theme.regular(13), Theme.TEXT_DIM);
        l.setPreferredSize(new Dimension(70, 20));
        form.add(l, lc);
        GridBagConstraints fc = new GridBagConstraints();
        fc.gridx = 1; fc.gridy = row; fc.weightx = 1; fc.fill = GridBagConstraints.HORIZONTAL; fc.insets = new Insets(6, 0, 6, 0);
        if (field instanceof StyledField) field.setPreferredSize(new Dimension(300, 38));
        form.add(field, fc);
        return row + 1;
    }

    public static void edit(Component parent, AppContext ctx, Song song) {
        SongDialog d = new SongDialog(parent, ctx, song);
        d.getRootPane().setBorder(BorderFactory.createEmptyBorder());
        d.open(480);
    }
}
