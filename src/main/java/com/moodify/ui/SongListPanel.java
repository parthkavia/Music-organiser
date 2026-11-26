package com.moodify.ui;

import com.moodify.audio.PlayerListener;
import com.moodify.model.Song;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The song table used by the Library and the Playlists pages. Double-click (or Enter) plays,
 * right-click or the "..." button opens the actions menu.
 */
public final class SongListPanel extends JPanel {
    public enum Mode { LIBRARY, PLAYLIST }

    private static final String[] COLUMNS = {"#", "TITLE", "GENRE", "TIME", "STATUS", ""};
    private static final int COL_INDEX = 0, COL_TITLE = 1, COL_GENRE = 2, COL_TIME = 3, COL_STATUS = 4, COL_MENU = 5;

    private final AppContext ctx;
    private final Mode mode;
    private final Supplier<String> playlistName;
    private final Model model = new Model();
    private final JTable table;
    private final CardLayout cards = new CardLayout();
    private final EmptyState empty;
    private final JPanel deck = new JPanel(cards);
    private int hoverRow = -1;

    public SongListPanel(AppContext ctx, Mode mode, Supplier<String> playlistName) {
        super(new BorderLayout());
        this.ctx = ctx;
        this.mode = mode;
        this.playlistName = playlistName;
        setOpaque(false);

        table = new JTable(model);
        CellView view = new CellView();
        table.setDefaultRenderer(Object.class, view);
        table.setRowHeight(56);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(Theme.BG);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setFocusable(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        int[] widths = {54, 400, 160, 80, 110, 48};
        for (int i = 0; i < widths.length; i++) {
            TableColumn col = table.getColumnModel().getColumn(i);
            col.setPreferredWidth(widths[i]);
            if (i != COL_TITLE) {
                col.setMinWidth(widths[i]);
                col.setMaxWidth(widths[i]);
            } else {
                col.setMinWidth(200);
            }
        }
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setPreferredSize(new Dimension(100, 34));
        header.setBackground(Theme.BG);
        header.setDefaultRenderer(new HeaderRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(Theme.BG);
        scroll.getViewport().setBackground(Theme.BG);
        UiKit.styleScroll(scroll);
        scroll.setOpaque(false);

        empty = new EmptyState(new PillButton("Import music", Glyph.PLUS, PillButton.Style.PRIMARY));
        PillButton importBtn = (PillButton) empty.getComponent(0);
        importBtn.addActionListener(e -> ctx.importMusic(this));

        deck.setOpaque(false);
        deck.add(scroll, "table");
        deck.add(empty, "empty");
        add(deck, BorderLayout.CENTER);

        installMouse();
        installKeys();
        ctx.addPlayerListener(new PlayerListener() {
            @Override public void trackChanged(Song song, int index, int size) { table.repaint(); }
            @Override public void playbackStateChanged(boolean playing) { table.repaint(); }
        });
    }

    // ------------------------------------------------------------------ data

    /** Shows these songs, keeping the current selection (by song id) where possible. */
    public void setSongs(List<Song> songs, String emptyTitle, String emptySubtitle, boolean emptyShowsImport) {
        Set<String> keep = new HashSet<>();
        for (Song s : selectedSongs()) keep.add(s.getId());
        model.rows = new ArrayList<>(songs);
        model.fireTableDataChanged();
        table.getSelectionModel().clearSelection();
        for (int i = 0; i < model.rows.size(); i++) {
            if (keep.contains(model.rows.get(i).getId())) table.addRowSelectionInterval(i, i);
        }
        if (songs.isEmpty()) {
            empty.set(emptyTitle, emptySubtitle, emptyShowsImport);
            cards.show(deck, "empty");
        } else {
            cards.show(deck, "table");
        }
    }

    public List<Song> getSongs() { return new ArrayList<>(model.rows); }

    public List<Song> selectedSongs() {
        List<Song> out = new ArrayList<>();
        for (int r : table.getSelectedRows()) {
            if (r >= 0 && r < model.rows.size()) out.add(model.rows.get(r));
        }
        return out;
    }

    /** Playlist mode only: moves the (single) selected song up (-1) or down (+1). */
    public void moveSelected(int delta) {
        if (mode != Mode.PLAYLIST) return;
        int[] sel = table.getSelectedRows();
        if (sel.length != 1) {
            ctx.toast("Select one song to move it.", true);
            return;
        }
        int from = sel[0], to = from + delta;
        if (to < 0 || to >= model.rows.size()) return;
        ctx.playlists.move(playlistName.get(), from, to);
    }

    private void playRow(int row) {
        if (row < 0 || row >= model.rows.size()) return;
        ctx.playSongs(new ArrayList<>(model.rows), row);
    }

    // ------------------------------------------------------------------ input

    private void installMouse() {
        MouseAdapter m = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row != hoverRow) {
                    hoverRow = row;
                    table.repaint();
                }
                int col = table.columnAtPoint(e.getPoint());
                table.setCursor(row >= 0 && col == COL_MENU ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
            @Override public void mouseExited(MouseEvent e) {
                hoverRow = -1;
                table.repaint();
            }
            @Override public void mousePressed(MouseEvent e) { maybePopup(e); }
            @Override public void mouseReleased(MouseEvent e) { maybePopup(e); }
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) return;
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0) return;
                if (col == COL_MENU) {
                    if (!table.isRowSelected(row)) table.setRowSelectionInterval(row, row);
                    Rectangle r = table.getCellRect(row, col, false);
                    showMenu(table, r.x + r.width - 4, r.y + r.height - 6);
                } else if (e.getClickCount() == 2) {
                    playRow(row);
                }
            }
            private void maybePopup(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0) return;
                if (!table.isRowSelected(row)) table.setRowSelectionInterval(row, row);
                showMenu(table, e.getX(), e.getY());
            }
        };
        table.addMouseListener(m);
        table.addMouseMotionListener(m);
    }

    private void installKeys() {
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "play");
        table.getActionMap().put("play", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { playRow(table.getSelectedRow()); }
        });
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "remove");
        table.getActionMap().put("remove", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                List<Song> sel = selectedSongs();
                if (sel.isEmpty()) return;
                if (mode == Mode.PLAYLIST) removeFromPlaylist(sel);
                else ctx.removeSongs(SongListPanel.this, sel);
            }
        });
    }

    private void removeFromPlaylist(List<Song> sel) {
        String name = playlistName.get();
        for (Song s : sel) ctx.playlists.removeSong(name, s.getId());
    }

    // ------------------------------------------------------------------ context menu

    private void showMenu(Component invoker, int x, int y) {
        List<Song> sel = selectedSongs();
        if (sel.isEmpty()) return;
        boolean single = sel.size() == 1;
        Song first = sel.get(0);
        JPopupMenu menu = new JPopupMenu();

        if (single) {
            JMenuItem play = new JMenuItem("Play");
            play.addActionListener(e -> playRow(table.getSelectedRow()));
            menu.add(play);
            menu.addSeparator();
        }

        JMenu add = new JMenu("Add to playlist");
        for (String name : ctx.playlists.names()) {
            JMenuItem item = new JMenuItem(name);
            item.addActionListener(e -> ctx.addToPlaylist(sel, name));
            add.add(item);
        }
        if (!ctx.playlists.names().isEmpty()) add.addSeparator();
        JMenuItem create = new JMenuItem("New playlist\u2026");
        create.addActionListener(e -> {
            String name = ctx.createPlaylistPrompt(this);
            if (name != null) ctx.addToPlaylist(sel, name);
        });
        add.add(create);
        menu.add(add);

        if (mode == Mode.PLAYLIST) {
            JMenuItem remove = new JMenuItem("Remove from this playlist");
            remove.addActionListener(e -> removeFromPlaylist(sel));
            menu.add(remove);
            if (single) {
                JMenuItem up = new JMenuItem("Move up");
                up.setEnabled(table.getSelectedRow() > 0);
                up.addActionListener(e -> moveSelected(-1));
                menu.add(up);
                JMenuItem down = new JMenuItem("Move down");
                down.setEnabled(table.getSelectedRow() < model.rows.size() - 1);
                down.addActionListener(e -> moveSelected(1));
                menu.add(down);
            }
        }

        menu.addSeparator();
        if (single) {
            JMenuItem edit = new JMenuItem("Edit info\u2026");
            edit.addActionListener(e -> ctx.editSong(this, first));
            menu.add(edit);
            JMenuItem relink = new JMenuItem("Relink MP3\u2026");
            relink.setEnabled(first.isMissing() || !first.fileExists());
            relink.addActionListener(e -> ctx.relinkSong(this, first));
            menu.add(relink);
        }
        JMenuItem delete = new JMenuItem(sel.size() > 1 ? "Remove " + sel.size() + " songs from library" : "Remove from library");
        delete.addActionListener(e -> ctx.removeSongs(this, sel));
        menu.add(delete);

        menu.show(invoker, x, y);
    }

    // ------------------------------------------------------------------ model

    private static final class Model extends AbstractTableModel {
        List<Song> rows = new ArrayList<>();
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int c) { return COLUMNS[c]; }
        @Override public Object getValueAt(int r, int c) { return r < rows.size() ? rows.get(r) : null; }
        @Override public boolean isCellEditable(int r, int c) { return false; }
    }

    // ------------------------------------------------------------------ rendering

    private static final class HeaderRenderer extends JComponent implements TableCellRenderer {
        private String text = "";
        private int col;
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int row, int c) {
            text = v == null ? "" : v.toString();
            col = c;
            return this;
        }
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            UiKit.antialias(g);
            g.setColor(Theme.BG);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Theme.BORDER);
            g.fillRect(0, getHeight() - 1, getWidth(), 1);
            g.setFont(Theme.bold(11));
            g.setColor(Theme.TEXT_FAINT);
            FontMetrics fm = g.getFontMetrics();
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            if (col == COL_INDEX) g.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, y);
            else g.drawString(text, col == COL_TITLE ? 58 : 4, y);
            g.dispose();
        }
    }

    /** One component paints every cell, by column. */
    private final class CellView extends JComponent implements TableCellRenderer {
        private Song song;
        private int row, col;
        private boolean selected;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int r, int c) {
            this.song = (Song) value;
            this.row = r;
            this.col = c;
            this.selected = isSelected;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            UiKit.antialias(g);
            int w = getWidth(), h = getHeight();
            boolean hover = row == hoverRow;
            g.setColor(selected ? Theme.mix(Theme.BG, Theme.ACCENT, 0.22f) : hover ? new Color(0x1B1B1E) : Theme.BG);
            g.fillRect(0, 0, w, h);
            if (song == null) { g.dispose(); return; }
            Song cur = ctx.currentSong();
            boolean current = cur != null && cur.getId().equals(song.getId());
            Color main = song.isMissing() ? Theme.TEXT_FAINT : current ? Theme.ACCENT_HOVER : Theme.TEXT;

            switch (col) {
                case COL_INDEX -> {
                    if (current) {
                        Glyph.PLAY.paint(g, w / 2.0 - 8, h / 2.0 - 8, 16, Theme.ACCENT_HOVER);
                    } else if (hover) {
                        Glyph.PLAY.paint(g, w / 2.0 - 8, h / 2.0 - 8, 16, Theme.TEXT);
                    } else {
                        g.setFont(Theme.regular(13));
                        g.setColor(Theme.TEXT_DIM);
                        FontMetrics fm = g.getFontMetrics();
                        String n = Integer.toString(row + 1);
                        g.drawString(n, (w - fm.stringWidth(n)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
                    }
                }
                case COL_TITLE -> {
                    g.drawImage(CoverArt.get(40, 6), 8, (h - 40) / 2, null);
                    if (song.isMissing()) {
                        g.setColor(Theme.alpha(Color.BLACK, 140));
                        g.fill(new RoundRectangle2D.Double(8, (h - 40) / 2.0, 40, 40, 12, 12));
                    }
                    g.setFont(Theme.bold(14));
                    FontMetrics fm = g.getFontMetrics();
                    g.setColor(main);
                    int tw = w - 58 - 8;
                    g.drawString(UiKit.ellipsize(fm, song.getTitle(), tw), 58, h / 2 - 3);
                    g.setFont(Theme.regular(12));
                    fm = g.getFontMetrics();
                    g.setColor(Theme.TEXT_DIM);
                    g.drawString(UiKit.ellipsize(fm, song.getArtist(), tw), 58, h / 2 + fm.getAscent() + 1);
                }
                case COL_GENRE -> drawText(g, song.getGenre(), 4, w - 8, h, Theme.TEXT_DIM);
                case COL_TIME -> drawText(g, UiKit.formatTime(song.getDurationMs()), 4, w - 8, h, Theme.TEXT_DIM);
                case COL_STATUS -> {
                    if (song.isMissing()) {
                        Glyph.WARNING.paint(g, 4, h / 2.0 - 8, 16, Theme.DANGER);
                        g.setFont(Theme.regular(12));
                        g.setColor(Theme.DANGER);
                        FontMetrics fm = g.getFontMetrics();
                        g.drawString("Missing", 26, (h - fm.getHeight()) / 2 + fm.getAscent());
                    } else {
                        g.setColor(Theme.SUCCESS);
                        g.fill(new Ellipse2D.Double(8, h / 2.0 - 3, 6, 6));
                        drawText(g, "Available", 22, w - 24, h, Theme.TEXT_FAINT);
                    }
                }
                default -> Glyph.MORE.paint(g, w / 2.0 - 10, h / 2.0 - 10, 20,
                        hover || selected ? Theme.TEXT : Theme.alpha(Theme.TEXT_FAINT, 120));
            }
            g.dispose();
        }

        private void drawText(Graphics2D g, String text, int x, int width, int h, Color color) {
            g.setFont(Theme.regular(13));
            g.setColor(color);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(UiKit.ellipsize(fm, text, width), x, (h - fm.getHeight()) / 2 + fm.getAscent());
        }
    }
}
